import bcrypt from 'bcryptjs';
import crypto from 'crypto';
import { OAuth2Client } from 'google-auth-library';
import { UserRole } from '@prisma/client';
import { prisma } from '../db/prisma.js';
import { otpSendLimiter, otpVerifyLimiter, blacklistToken } from '../db/redis.js';
import { config } from '../config/index.js';
import { logger } from '../utils/logger.js';
import {
  AuthError,
  ConflictError,
  RateLimitError,
  NotFoundError,
} from '../utils/errors.js';
import { emailService } from './email.service.js';

const googleClient = new OAuth2Client(config.GOOGLE_CLIENT_ID);

// ─── Token Generation ─────────────────────────────────────────────────────────

/**
 * Generate a cryptographically random 6-digit OTP.
 * Uses crypto.randomInt which is truly random, unlike Math.random().
 */
function generateOtp(): string {
  return crypto.randomInt(100_000, 999_999).toString();
}

/**
 * Hash an OTP or refresh token with bcrypt before storing.
 * bcrypt is intentionally slow — good for secrets.
 */
async function hashSecret(secret: string): Promise<string> {
  return bcrypt.hash(secret, 10);
}

async function verifySecret(plain: string, hash: string): Promise<boolean> {
  return bcrypt.compare(plain, hash);
}

/**
 * Generate a secure random refresh token (256 bits of entropy).
 * This is stored hashed in DB; the plaintext is sent to client once only.
 */
function generateRefreshToken(): string {
  return crypto.randomBytes(32).toString('hex'); // 64 hex chars
}

// ─── OTP Auth ─────────────────────────────────────────────────────────────────

export async function sendOtp(email: string, _ip: string): Promise<void> {
  // Rate limit: max 3 OTPs per email per hour
  const { success, remaining } = await otpSendLimiter.limit(email);
  if (!success) {
    throw new RateLimitError(`OTP limit reached. Please wait before requesting again. (${remaining} remaining)`);
  }

  const otp = generateOtp();
  const otpHash = await hashSecret(otp);
  const expiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 minutes

  // Invalidate any existing unused OTPs for this email
  await prisma.emailOtp.updateMany({
    where: { email, used: false },
    data: { used: true },
  });

  await prisma.emailOtp.create({
    data: { email, otpHash, expiresAt, purpose: 'login' },
  });

  // Send email
  await emailService.sendOtp(email, otp);

  logger.info({ email }, 'OTP sent');
}

export async function verifyOtp(
  email: string,
  otp: string,
  _ip: string,
): Promise<{ isNewUser: boolean; userId: string; role: UserRole | null }> {
  // Rate limit: max 5 verify attempts per 15 min
  const { success } = await otpVerifyLimiter.limit(email);
  if (!success) {
    throw new RateLimitError('Too many OTP attempts. Please request a new OTP.');
  }

  const record = await prisma.emailOtp.findFirst({
    where: {
      email,
      used: false,
      expiresAt: { gt: new Date() },
    },
    orderBy: { createdAt: 'desc' },
  });

  if (!record) {
    throw new AuthError('OTP expired or invalid. Please request a new one.');
  }

  // Increment attempts
  await prisma.emailOtp.update({
    where: { id: record.id },
    data: { attempts: { increment: 1 } },
  });

  if (record.attempts >= 3) {
    await prisma.emailOtp.update({ where: { id: record.id }, data: { used: true } });
    throw new AuthError('Too many incorrect attempts. Please request a new OTP.');
  }

  const valid = await verifySecret(otp, record.otpHash);
  if (!valid) {
    throw new AuthError('Incorrect OTP. Please try again.');
  }

  // Mark OTP as used
  await prisma.emailOtp.update({ where: { id: record.id }, data: { used: true } });

  // Find or prepare user
  const existingUser = await prisma.user.findUnique({ where: { email } });

  if (existingUser) {
    if (existingUser.status === 'suspended') {
      throw new AuthError('Your account has been suspended. Contact support.');
    }
    if (existingUser.status === 'deleted') {
      throw new AuthError('Account not found.');
    }

    await prisma.user.update({
      where: { id: existingUser.id },
      data: { emailVerified: true, lastLoginAt: new Date() },
    });

    return { isNewUser: false, userId: existingUser.id, role: existingUser.role };
  }

  // New user — create a placeholder (role is set during onboarding)
  // We use STUDENT as a placeholder; it's updated during role selection
  const newUser = await prisma.user.create({
    data: {
      email,
      emailVerified: true,
      role: 'STUDENT', // temporary — overwritten at role selection
      lastLoginAt: new Date(),
      subscription: { create: { plan: 'free' } },
    },
  });

  return { isNewUser: true, userId: newUser.id, role: null };
}

// ─── Google OAuth ─────────────────────────────────────────────────────────────

export async function verifyGoogleToken(
  idToken: string,
): Promise<{ isNewUser: boolean; userId: string; role: UserRole | null }> {
  let payload;
  try {
    const ticket = await googleClient.verifyIdToken({
      idToken,
      audience: config.GOOGLE_CLIENT_ID,
    });
    payload = ticket.getPayload();
  } catch (err) {
    logger.warn({ err }, 'Google token verification failed');
    throw new AuthError('Invalid Google token. Please try signing in again.');
  }

  if (!payload || !payload.email) {
    throw new AuthError('Could not retrieve email from Google account.');
  }

  const { sub, email, name: _name, email_verified } = payload;

  if (!email_verified) {
    throw new AuthError('Google account email is not verified.');
  }

  // Check if user exists by Google sub or email
  const existingUser = await prisma.user.findFirst({
    where: { OR: [{ googleSub: sub }, { email }] },
  });

  if (existingUser) {
    if (existingUser.status === 'suspended') throw new AuthError('Account suspended.');
    if (existingUser.status === 'deleted') throw new AuthError('Account not found.');

    // Link Google sub if not already linked
    if (!existingUser.googleSub) {
      await prisma.user.update({
        where: { id: existingUser.id },
        data: { googleSub: sub, lastLoginAt: new Date() },
      });
    } else {
      await prisma.user.update({
        where: { id: existingUser.id },
        data: { lastLoginAt: new Date() },
      });
    }

    return { isNewUser: false, userId: existingUser.id, role: existingUser.role };
  }

  // New user via Google
  const newUser = await prisma.user.create({
    data: {
      email: email!,
      emailVerified: true,
      googleSub: sub,
      role: 'STUDENT', // temporary placeholder
      lastLoginAt: new Date(),
      subscription: { create: { plan: 'free' } },
    },
  });

  return { isNewUser: true, userId: newUser.id, role: null };
}

// ─── Session Management ───────────────────────────────────────────────────────

export interface DeviceInfo {
  deviceId?: string;
  deviceModel?: string;
  appVersion?: string;
  ip?: string;
  userAgent?: string;
}

export async function createSession(
  userId: string,
  _role: UserRole,
  device: DeviceInfo,
): Promise<{ accessToken: string; refreshToken: string; sessionId: string }> {
  const refreshToken = generateRefreshToken();
  const refreshTokenHash = await hashSecret(refreshToken);
  const expiresAt = new Date(
    Date.now() + config.JWT_REFRESH_EXPIRY_DAYS * 24 * 60 * 60 * 1000,
  );

  const session = await prisma.userSession.create({
    data: {
      userId,
      refreshTokenHash,
      deviceId: device.deviceId,
      deviceModel: device.deviceModel,
      appVersion: device.appVersion,
      ipAddress: device.ip,
      userAgent: device.userAgent,
      expiresAt,
    },
  });

  // We store what's needed; fastify.jwt.sign() is called in the route layer
  return {
    accessToken: '', // filled in by route
    refreshToken,
    sessionId: session.id,
  };
}

export async function refreshAccessToken(
  refreshToken: string,
): Promise<{ userId: string; role: UserRole; sessionId: string }> {
  // Find all non-revoked sessions and check hash
  // We can't look up by plaintext token — we must check all active sessions for this could be slow
  // Solution: store a fast-lookup hash (first 8 chars as index key is not needed since bcrypt is the comparison)
  // Better: store a non-hashed session ID prefix for lookup, then bcrypt compare

  // For MVP: scan recent sessions. In production, use a faster lookup strategy.
  const sessions = await prisma.userSession.findMany({
    where: {
      revoked: false,
      expiresAt: { gt: new Date() },
    },
    include: { user: true },
    orderBy: { createdAt: 'desc' },
    take: 1000, // cap for safety
  });

  for (const session of sessions) {
    const match = await verifySecret(refreshToken, session.refreshTokenHash);
    if (match) {
      if (session.user.status !== 'active') {
        throw new AuthError('Account is not active.');
      }

      // Rotate: revoke old session, create new one
      await prisma.userSession.update({
        where: { id: session.id },
        data: { revoked: true, revokedAt: new Date() },
      });

      // Blacklist old token in Redis for TTL of original expiry
      const ttl = Math.floor((session.expiresAt.getTime() - Date.now()) / 1000);
      if (ttl > 0) {
        await blacklistToken(session.refreshTokenHash, ttl);
      }

      return {
        userId: session.userId,
        role: session.user.role,
        sessionId: session.id,
      };
    }
  }

  throw new AuthError('Invalid or expired refresh token. Please log in again.');
}

export async function revokeSession(sessionId: string, userId: string): Promise<void> {
  const session = await prisma.userSession.findFirst({
    where: { id: sessionId, userId },
  });

  if (!session) return; // silently succeed

  await prisma.userSession.update({
    where: { id: session.id },
    data: { revoked: true, revokedAt: new Date() },
  });

  // Blacklist in Redis
  const ttl = Math.floor((session.expiresAt.getTime() - Date.now()) / 1000);
  if (ttl > 0) {
    await blacklistToken(session.refreshTokenHash, ttl);
  }
}

// ─── Role Selection (one-time, at onboarding) ─────────────────────────────────

export async function setUserRole(userId: string, role: UserRole): Promise<void> {
  const user = await prisma.user.findUnique({ where: { id: userId } });
  if (!user) throw new NotFoundError('User not found');

  // Role can only be set once in MVP
  // Check if onboarding is already complete
  const profileComplete =
    (role === 'STUDENT' && (await prisma.studentProfile.findUnique({ where: { userId } }))?.onboardingComplete) ||
    (role === 'FACULTY' && (await prisma.facultyProfile.findUnique({ where: { userId } }))?.onboardingComplete) ||
    (role === 'CURIOUS' && (await prisma.curiousProfile.findUnique({ where: { userId } }))?.onboardingComplete);

  if (profileComplete) {
    throw new ConflictError('Role already set. Contact support to change.');
  }

  await prisma.user.update({
    where: { id: userId },
    data: { role, tosAcceptedAt: new Date() },
  });

  // Create empty profile record for this role
  if (role === 'STUDENT') {
    await prisma.studentProfile.upsert({
      where: { userId },
      update: {},
      create: { userId },
    });
  } else if (role === 'FACULTY') {
    await prisma.facultyProfile.upsert({
      where: { userId },
      update: {},
      create: { userId },
    });
  } else {
    await prisma.curiousProfile.upsert({
      where: { userId },
      update: {},
      create: { userId },
    });
  }
}
