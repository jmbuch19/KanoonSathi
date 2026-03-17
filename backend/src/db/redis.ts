import { Redis } from '@upstash/redis';
import { Ratelimit } from '@upstash/ratelimit';
import { config } from '../config/index.js';
import { logger } from '../utils/logger.js';

// Upstash Redis — serverless Redis with HTTP API.
// Works on any hosting platform, including Railway and Render.
export const redis = new Redis({
  url: config.UPSTASH_REDIS_REST_URL,
  token: config.UPSTASH_REDIS_REST_TOKEN,
});

// ─── Rate Limiters ────────────────────────────────────────────────────────────
// Sliding window algorithm: counts requests in a rolling time window.

// General API rate limiter (per IP)
export const apiRateLimiter = new Ratelimit({
  redis,
  limiter: Ratelimit.slidingWindow(100, '1 m'),
  analytics: true,
  prefix: 'rl:api',
});

// OTP send limiter (per email) — prevent spam
export const otpSendLimiter = new Ratelimit({
  redis,
  limiter: Ratelimit.slidingWindow(3, '1 h'),
  analytics: true,
  prefix: 'rl:otp_send',
});

// OTP verify limiter (per email) — prevent brute force
export const otpVerifyLimiter = new Ratelimit({
  redis,
  limiter: Ratelimit.slidingWindow(5, '15 m'),
  analytics: true,
  prefix: 'rl:otp_verify',
});

// Chat message limiter (per user, free plan)
export const chatFreeLimiter = new Ratelimit({
  redis,
  limiter: Ratelimit.slidingWindow(30, '1 h'),
  analytics: true,
  prefix: 'rl:chat_free',
});

// ─── Context Cache Helpers ─────────────────────────────────────────────────────
// Chat context is stored in Redis for speed.
// PostgreSQL is the source of truth; Redis is the hot cache.

const CONTEXT_TTL_SECONDS = 30 * 60; // 30 minutes

export interface ContextMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export async function getChatContext(sessionId: string): Promise<ContextMessage[]> {
  const key = `chat_ctx:${sessionId}`;
  const raw = await redis.get<ContextMessage[]>(key);
  return raw ?? [];
}

export async function setChatContext(
  sessionId: string,
  messages: ContextMessage[],
): Promise<void> {
  const key = `chat_ctx:${sessionId}`;
  // Keep only last 20 messages to control token usage
  const trimmed = messages.slice(-20);
  await redis.setex(key, CONTEXT_TTL_SECONDS, JSON.stringify(trimmed));
}

export async function appendToChatContext(
  sessionId: string,
  message: ContextMessage,
): Promise<void> {
  const existing = await getChatContext(sessionId);
  existing.push(message);
  await setChatContext(sessionId, existing);
}

export async function clearChatContext(sessionId: string): Promise<void> {
  await redis.del(`chat_ctx:${sessionId}`);
}

// ─── Session Blacklist ─────────────────────────────────────────────────────────
// When a refresh token is revoked, we add it to a blacklist so it can't be reused
// even if DB update hasn't propagated yet.

export async function blacklistToken(tokenHash: string, ttlSeconds: number): Promise<void> {
  await redis.setex(`bl:${tokenHash}`, ttlSeconds, '1');
}

export async function isTokenBlacklisted(tokenHash: string): Promise<boolean> {
  const val = await redis.get(`bl:${tokenHash}`);
  return val !== null;
}

export async function verifyRedisConnection(): Promise<void> {
  await redis.ping();
  logger.info('Redis (Upstash) connected');
}
