import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import { z } from 'zod';
import {
  sendOtp,
  verifyOtp,
  verifyGoogleToken,
  setUserRole,
  createSession,
  refreshAccessToken,
  revokeSession,
} from '../../services/auth.service.js';
import { requireAuth } from '../../middleware/auth.middleware.js';
import { prisma } from '../../db/prisma.js';
import { ValidationError, AuthError } from '../../utils/errors.js';
import { UserRole } from '@prisma/client';
import type { JwtPayload } from '../../types/index.js';

// ─── Schemas ──────────────────────────────────────────────────────────────────

const sendOtpSchema = z.object({
  email: z.string().email('Please provide a valid email address').toLowerCase(),
});

const verifyOtpSchema = z.object({
  email: z.string().email().toLowerCase(),
  otp: z.string().length(6, 'OTP must be 6 digits').regex(/^\d{6}$/),
});

const googleSchema = z.object({
  idToken: z.string().min(1, 'Google ID token is required'),
  deviceId: z.string().optional(),
  deviceModel: z.string().optional(),
  appVersion: z.string().optional(),
});

const roleSchema = z.object({
  role: z.enum(['STUDENT', 'FACULTY', 'CURIOUS']),
});

const refreshSchema = z.object({
  refreshToken: z.string().min(1),
});

// ─── Helper ───────────────────────────────────────────────────────────────────

function getDeviceInfo(req: FastifyRequest, body: { deviceId?: string; deviceModel?: string; appVersion?: string }) {
  return {
    deviceId: body.deviceId,
    deviceModel: body.deviceModel,
    appVersion: body.appVersion,
    ip: req.ip,
    userAgent: req.headers['user-agent'],
  };
}

async function issueTokens(
  fastify: FastifyInstance,
  userId: string,
  role: UserRole,
  device: ReturnType<typeof getDeviceInfo>,
): Promise<{ accessToken: string; refreshToken: string; sessionId: string }> {
  const session = await createSession(userId, role, device);

  const payload: JwtPayload = {
    sub: userId,
    role,
    sessionId: session.sessionId,
  };

  const accessToken = fastify.jwt.sign(payload, { expiresIn: '15m' });

  return { accessToken, refreshToken: session.refreshToken, sessionId: session.sessionId };
}

// ─── Route Registration ───────────────────────────────────────────────────────

export async function authRoutes(fastify: FastifyInstance): Promise<void> {
  // POST /api/v1/auth/otp/send
  fastify.post('/otp/send', async (req: FastifyRequest, reply: FastifyReply) => {
    const result = sendOtpSchema.safeParse(req.body);
    if (!result.success) throw new ValidationError('Invalid request', result.error.flatten());

    await sendOtp(result.data.email, req.ip);

    return reply.code(200).send({
      success: true,
      data: { message: 'OTP sent to your email. It expires in 5 minutes.' },
    });
  });

  // POST /api/v1/auth/otp/verify
  fastify.post('/otp/verify', async (req: FastifyRequest, reply: FastifyReply) => {
    const result = verifyOtpSchema.safeParse(req.body);
    if (!result.success) throw new ValidationError('Invalid request', result.error.flatten());

    const { email, otp } = result.data;
    const body = req.body as { deviceId?: string; deviceModel?: string; appVersion?: string };

    const { isNewUser, userId, role } = await verifyOtp(email, otp, req.ip);

    // If new user — don't issue tokens yet (they must select a role first)
    if (isNewUser) {
      const setupToken = fastify.jwt.sign({ sub: userId, setup: true } as any, { expiresIn: '15m' });
      return reply.code(200).send({
        success: true,
        data: { isNewUser: true, setupToken, requiresRoleSelection: true },
      });
    }

    const tokens = await issueTokens(fastify, userId, role!, getDeviceInfo(req, body));

    return reply.code(200).send({
      success: true,
      data: { isNewUser: false, ...tokens },
    });
  });

  // POST /api/v1/auth/google
  fastify.post('/google', async (req: FastifyRequest, reply: FastifyReply) => {
    const result = googleSchema.safeParse(req.body);
    if (!result.success) throw new ValidationError('Invalid request', result.error.flatten());

    const { idToken, ...deviceFields } = result.data;
    const { isNewUser, userId, role } = await verifyGoogleToken(idToken);

    if (isNewUser) {
      const setupToken = fastify.jwt.sign({ sub: userId, setup: true } as any, { expiresIn: '15m' });
      return reply.code(200).send({
        success: true,
        data: { isNewUser: true, setupToken, requiresRoleSelection: true },
      });
    }

    const tokens = await issueTokens(fastify, userId, role!, getDeviceInfo(req, deviceFields));

    return reply.code(200).send({
      success: true,
      data: { isNewUser: false, ...tokens },
    });
  });

  // POST /api/v1/auth/role — called after first-time auth for new users
  fastify.post('/role', async (req: FastifyRequest, reply: FastifyReply) => {
    // Verify the short-lived setup token issued during OTP/Google verify
    const authHeader = req.headers.authorization;
    if (!authHeader?.startsWith('Bearer ')) throw new AuthError('Setup token required');
    const setupToken = authHeader.slice(7);

    let tokenPayload: Record<string, unknown>;
    try {
      tokenPayload = fastify.jwt.verify<Record<string, unknown>>(setupToken);
    } catch {
      throw new AuthError('Invalid or expired setup token');
    }
    if (tokenPayload.setup !== true || typeof tokenPayload.sub !== 'string') {
      throw new AuthError('Invalid setup token');
    }
    const userId = tokenPayload.sub;

    const body = req.body as { role: string; deviceId?: string; deviceModel?: string; appVersion?: string };
    const roleResult = roleSchema.safeParse({ role: body.role });
    if (!roleResult.success) throw new ValidationError('Invalid role');

    await setUserRole(userId, roleResult.data.role);

    const tokens = await issueTokens(fastify, userId, roleResult.data.role, getDeviceInfo(req, body));

    return reply.code(200).send({
      success: true,
      data: { ...tokens, role: roleResult.data.role },
    });
  });

  // POST /api/v1/auth/refresh
  fastify.post('/refresh', async (req: FastifyRequest, reply: FastifyReply) => {
    const result = refreshSchema.safeParse(req.body);
    if (!result.success) throw new ValidationError('refreshToken is required');

    const { userId, role } = await refreshAccessToken(result.data.refreshToken);

    const body = req.body as { deviceId?: string; deviceModel?: string; appVersion?: string };

    // Issue fresh tokens
    const newSession = await createSession(userId, role, getDeviceInfo(req, body));
    const payload: JwtPayload = { sub: userId, role, sessionId: newSession.sessionId };
    const accessToken = fastify.jwt.sign(payload, { expiresIn: '15m' });

    return reply.code(200).send({
      success: true,
      data: { accessToken, refreshToken: newSession.refreshToken },
    });
  });

  // POST /api/v1/auth/logout  [requires auth]
  fastify.post('/logout', { preHandler: [requireAuth] }, async (req: FastifyRequest, reply: FastifyReply) => {
    await revokeSession(req.user.sessionId, req.user.sub);
    return reply.code(200).send({ success: true, data: { message: 'Logged out successfully' } });
  });

  // GET /api/v1/auth/me  [requires auth]
  fastify.get('/me', { preHandler: [requireAuth] }, async (req: FastifyRequest, reply: FastifyReply) => {
    const user = await prisma.user.findUnique({
      where: { id: req.user.sub },
      select: {
        id: true,
        email: true,
        role: true,
        status: true,
        createdAt: true,
        studentProfile: { select: { fullName: true, onboardingComplete: true, yearOfStudy: true, semester: true, examTarget: true, subjectsOfInterest: true, collegeName: true } },
        facultyProfile: { select: { fullName: true, onboardingComplete: true, institutionName: true, designation: true, verified: true } },
        curiousProfile: { select: { displayName: true, onboardingComplete: true, disclaimerAccepted: true } },
        subscription: { select: { plan: true, status: true, expiresAt: true } },
      },
    });

    if (!user) throw new Error('User not found');

    return reply.send({ success: true, data: user });
  });
}
