import Fastify, { FastifyRequest, FastifyReply } from 'fastify';
import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import jwt from '@fastify/jwt';
import { config } from './config/index.js';
import { logger } from './utils/logger.js';
import { createTrace } from './utils/trace.js';
import { connectDB, disconnectDB } from './db/prisma.js';
import { verifyRedisConnection, apiRateLimiter } from './db/redis.js';
import { authRoutes } from './routes/auth/index.js';
import { chatRoutes } from './routes/chat/index.js';
import { userRoutes } from './routes/user/index.js';
import { adminRoutes } from './routes/admin/index.js';
import { newsRoutes } from './routes/news/index.js';
import { firmRoutes } from './routes/firms/index.js';
import { legalRoutes } from './routes/legal/index.js';
import { AppError } from './utils/errors.js';

async function buildServer() {
  const fastify = Fastify({
    logger: false,
    trustProxy: true,
    bodyLimit: 1_048_576, // 1 MB global body limit
  });

  // ── Trace — attach traceId + span timing to every request ──────────
  fastify.addHook('onRequest', async (req) => {
    req.trace = createTrace();
  });

  fastify.addHook('onResponse', async (req, reply) => {
    reply.header('X-Trace-ID', req.trace?.traceId ?? '');
    req.trace?.finish(req.url, reply.statusCode);
  });

  // ── Global IP rate limit — 100 req/min per IP ───────────────────────
  fastify.addHook('onRequest', async (req, reply) => {
    if (req.url === '/health') return;
    const ip = req.ip ?? 'unknown';
    const { success } = await apiRateLimiter.limit(ip).catch(() => ({ success: true }));
    if (!success) {
      return reply.code(429).send({
        success: false,
        error: { code: 'RATE_LIMIT', message: 'Too many requests. Please slow down and try again.' },
      });
    }
  });

  // ── Plugins ────────────────────────────────────────────────────────
  await fastify.register(helmet, {
    contentSecurityPolicy: {
      directives: {
        defaultSrc:     ["'none'"],
        scriptSrc:      ["'none'"],
        objectSrc:      ["'none'"],
        styleSrc:       ["'none'"],
        frameAncestors: ["'none'"],
      },
    },
  });

  await fastify.register(cors, {
    origin:
      config.NODE_ENV === 'production'
        ? ['https://admin.kanoonsaathi.in']
        : true,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Admin-Token', 'X-Request-ID', 'X-Firm-Token'],
    exposedHeaders: ['X-Trace-ID'],
    credentials: true,
  });

  // User JWT (HS256, 15m access tokens)
  await fastify.register(jwt, {
    secret: config.JWT_SECRET,
    sign: { algorithm: 'HS256' },
  });

  // Admin JWT — isolated namespace so admin tokens can't be used as user tokens
  await fastify.register(jwt, {
    namespace: 'admin',
    secret: config.ADMIN_JWT_SECRET,
    sign: { algorithm: 'HS256' },
  });

  // ── Global Error Handler ────────────────────────────────────────────
  fastify.setErrorHandler(async (error, req: FastifyRequest, reply: FastifyReply) => {
    if (error instanceof AppError) {
      logger.warn({ err: error, path: req.url }, error.message);
      return reply.code(error.statusCode).send({
        success: false,
        error: { code: error.code, message: error.message, details: error.details },
      });
    }

    if (error.validation) {
      return reply.code(400).send({
        success: false,
        error: { code: 'VALIDATION_ERROR', message: 'Invalid request data', details: error.validation },
      });
    }

    logger.error({ err: error, path: req.url }, 'Unhandled error');
    return reply.code(500).send({
      success: false,
      error: {
        code: 'INTERNAL_ERROR',
        message: config.NODE_ENV === 'production' ? 'Something went wrong.' : error.message,
      },
    });
  });

  // ── Health Check ────────────────────────────────────────────────────
  fastify.get('/health', async (_req, reply) => {
    return reply.send({ status: 'ok', timestamp: new Date().toISOString(), version: '1.0.0' });
  });

  // ── Routes ──────────────────────────────────────────────────────────
  await fastify.register(authRoutes,  { prefix: '/api/v1/auth' });
  await fastify.register(chatRoutes,  { prefix: '/api/v1/chat' });
  await fastify.register(userRoutes,  { prefix: '/api/v1/user' });
  await fastify.register(adminRoutes, { prefix: '/api/v1/admin' });
  await fastify.register(newsRoutes,  { prefix: '/api/v1/news' });
  await fastify.register(firmRoutes,  { prefix: '/api/v1/firms' });
  await fastify.register(legalRoutes, { prefix: '/api/v1/legal' });

  fastify.setNotFoundHandler(async (req: FastifyRequest, reply: FastifyReply) => {
    return reply.code(404).send({
      success: false,
      error: { code: 'NOT_FOUND', message: `${req.method} ${req.url} not found` },
    });
  });

  return fastify;
}

async function start() {
  try {
    await connectDB();
    await verifyRedisConnection();

    const fastify = await buildServer();
    await fastify.listen({ port: config.PORT, host: config.HOST });

    logger.info(`🚀 KanoonSaathi API running on http://${config.HOST}:${config.PORT}`);
    logger.info(`📦 Environment: ${config.NODE_ENV}`);

    const shutdown = async (signal: string) => {
      logger.info(`${signal} received. Shutting down...`);
      await fastify.close();
      await disconnectDB();
      process.exit(0);
    };

    process.on('SIGTERM', () => shutdown('SIGTERM'));
    process.on('SIGINT', () => shutdown('SIGINT'));

  } catch (err) {
    logger.fatal({ err }, 'Failed to start server');
    process.exit(1);
  }
}

start();
