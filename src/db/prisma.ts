import { PrismaClient } from '@prisma/client';
import { logger } from '../utils/logger.js';

// Single shared Prisma instance — never instantiate multiple PrismaClient objects.
// In development, hot-reload can create multiple instances without this guard.

const globalForPrisma = globalThis as unknown as { prisma?: PrismaClient };

export const prisma =
  globalForPrisma.prisma ??
  new PrismaClient({
    log: [
      { level: 'error', emit: 'event' },
      { level: 'warn', emit: 'event' },
    ],
  });

if (process.env.NODE_ENV !== 'production') {
  globalForPrisma.prisma = prisma;
}

// Log DB errors through pino so all logs go to same place
(prisma as any).$on('error', (e: unknown) => {
  logger.error({ err: e }, 'Prisma error');
});

(prisma as any).$on('warn', (e: unknown) => {
  logger.warn({ warn: e }, 'Prisma warning');
});

export async function connectDB(): Promise<void> {
  await prisma.$connect();
  logger.info('PostgreSQL connected');
}

export async function disconnectDB(): Promise<void> {
  await prisma.$disconnect();
  logger.info('PostgreSQL disconnected');
}
