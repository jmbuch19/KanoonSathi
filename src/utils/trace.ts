/**
 * Lightweight request tracing — no external dependencies.
 * Uses AsyncLocalStorage so any service can log spans without parameter passing.
 */
import { randomUUID } from 'crypto';
import { AsyncLocalStorage } from 'async_hooks';
import { logger } from './logger.js';

export type Span = { label: string; startMs: number };

export interface RequestTrace {
  traceId: string;
  userId?: string;
  spans: Array<{ label: string; durationMs: number }>;
  startMs: number;
  start: (label: string) => Span;
  end: (span: Span) => number;
  tag: (userId: string) => void;
  finish: (path: string, status: number) => void;
}

// Thread-local storage for the current request trace
export const traceStorage = new AsyncLocalStorage<RequestTrace>();

/** Get the current trace (returns undefined outside a request context) */
export function currentTrace(): RequestTrace | undefined {
  return traceStorage.getStore();
}

/** Start a named span in the current trace */
export function startSpan(label: string): Span {
  const trace = currentTrace();
  return trace ? trace.start(label) : { label, startMs: Date.now() };
}

/** End a span and return its duration in ms */
export function endSpan(span: Span): number {
  const trace = currentTrace();
  return trace ? trace.end(span) : Date.now() - span.startMs;
}

export function createTrace(): RequestTrace {
  const traceId = randomUUID();
  const startMs = Date.now();
  const spans: Array<{ label: string; durationMs: number }> = [];
  let userId: string | undefined;

  const trace: RequestTrace = {
    traceId,
    spans,
    startMs,
    get userId() { return userId; },

    start(label: string): Span {
      return { label, startMs: Date.now() };
    },

    end(span: Span): number {
      const durationMs = Date.now() - span.startMs;
      spans.push({ label: span.label, durationMs });
      return durationMs;
    },

    tag(uid: string) {
      userId = uid;
    },

    finish(path: string, status: number) {
      const totalMs = Date.now() - startMs;
      logger.info(
        { traceId, userId, path, status, totalMs, spans },
        `[TRACE] ${path} → ${status} in ${totalMs}ms`,
      );
      if (totalMs > 5000) {
        logger.warn({ traceId, path, totalMs }, '[ALERT:SLOW_REQUEST] >5s total');
      }
      const aiSpan = spans.find(s => s.label === 'ai_generate');
      if (aiSpan && aiSpan.durationMs > 8000) {
        logger.warn({ traceId, aiDurationMs: aiSpan.durationMs }, '[ALERT:SLOW_AI] Claude >8s');
      }
    },
  };

  return trace;
}

// Declare on FastifyRequest
declare module 'fastify' {
  interface FastifyRequest {
    trace: RequestTrace;
  }
}
