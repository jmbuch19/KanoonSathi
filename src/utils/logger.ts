import pino from 'pino';
import { config } from '../config/index.js';

export const logger = pino({
  level: config.NODE_ENV === 'production' ? 'info' : 'debug',
  transport:
    config.NODE_ENV !== 'production'
      ? {
          target: 'pino-pretty',
          options: {
            colorize: true,
            translateTime: 'SYS:standard',
            ignore: 'pid,hostname',
          },
        }
      : undefined,
  // In production, pino outputs NDJSON — easy to pipe into Papertrail or Grafana
  redact: {
    // Never log these fields, even accidentally
    paths: [
      'req.headers.authorization',
      '*.password',
      '*.passwordHash',
      '*.otp',
      '*.refreshToken',
      '*.accessToken',
      '*.mfaSecret',
    ],
    censor: '[REDACTED]',
  },
});
