/**
 * Silent Failure Taxonomy for KanoonSaathi
 *
 * "Silent failures" are failures that return HTTP 200 or a valid-looking response
 * but are actually broken or harmful. Harder to detect than crashes.
 *
 * Taxonomy:
 *  SILENT_LEGAL_ADVICE        — AI gave direct legal advice despite guardrails
 *  SILENT_EMPTY_AI_RESPONSE   — AI returned empty or near-empty content
 *  SILENT_CONTEXT_LOSS        — Redis context expired; AI answered without history
 *  SILENT_QUOTA_MASKED        — User hit quota but received generic "AI error"
 *  SILENT_MODERATION_BYPASS   — Output passed moderation but contains advice patterns
 *  SILENT_EMAIL_NOT_DELIVERED — OTP email "sent" but Resend reported no delivery
 *  SILENT_DB_STALE_READ       — Message saved to DB but read-back returned stale data
 *  SILENT_HALLUCINATED_CITE   — AI cited a non-existent section/case
 */

import { logger } from '../utils/logger.js';
import { prisma } from '../db/prisma.js';

export type SilentFailureCode =
  | 'SILENT_LEGAL_ADVICE'
  | 'SILENT_EMPTY_AI_RESPONSE'
  | 'SILENT_CONTEXT_LOSS'
  | 'SILENT_QUOTA_MASKED'
  | 'SILENT_MODERATION_BYPASS'
  | 'SILENT_EMAIL_NOT_DELIVERED'
  | 'SILENT_HALLUCINATED_CITE';

interface FailureContext {
  userId?: string;
  sessionId?: string;
  messageId?: string;
  content?: string;
  meta?: Record<string, unknown>;
}

/**
 * Log a detected silent failure.
 * Writes to both logger (for log aggregation) and moderation_flags (for admin review).
 */
export async function reportSilentFailure(
  code: SilentFailureCode,
  ctx: FailureContext,
): Promise<void> {
  // Always log with structured data — can be scraped by Datadog / Grafana / Sentry
  logger.warn(
    {
      silentFailure: code,
      userId: ctx.userId,
      sessionId: ctx.sessionId,
      messageId: ctx.messageId,
      meta: ctx.meta,
    },
    `[ALERT:${code}]`,
  );

  // Persist to DB for admin visibility (uses existing moderation_flags table)
  if (ctx.userId && ctx.messageId) {
    try {
      const flagTypeMap: Record<SilentFailureCode, 'legal_advice' | 'hallucination' | 'harmful' | 'abuse'> = {
        SILENT_LEGAL_ADVICE:        'legal_advice',
        SILENT_EMPTY_AI_RESPONSE:   'harmful',
        SILENT_CONTEXT_LOSS:        'harmful',
        SILENT_QUOTA_MASKED:        'harmful',
        SILENT_MODERATION_BYPASS:   'legal_advice',
        SILENT_EMAIL_NOT_DELIVERED: 'harmful',
        SILENT_HALLUCINATED_CITE:   'hallucination',
      };
      await prisma.moderationFlag.create({
        data: {
          userId:       ctx.userId,
          messageId:    ctx.messageId,
          flagType:     flagTypeMap[code],
          flagSource:   'auto',
          confidence:   0.8,
          reviewStatus: 'pending',
        },
      });
    } catch {
      // Don't crash the request if flag persistence fails
    }
  }
}

// ─── Detection Helpers ────────────────────────────────────────────────────────

/** Legal advice patterns that should NEVER appear in AI output  */
const LEGAL_ADVICE_OUTPUT_PATTERNS = [
  /you should (file|register|sue|claim|pursue|challenge|appeal)/i,
  /my advice (is|would be)/i,
  /i (recommend|suggest|advise) you (to )?/i,
  /in your (specific |particular )?case,? you/i,
  /you are entitled to/i,
  /you have a (strong |valid )?case/i,
  /your best option is to/i,
  /you should (definitely|probably|immediately)/i,
];

/** Section/statute hallucination detectors */
const HALLUCINATION_PATTERNS = [
  /section \d{4,}/i,              // Section 12345+ (IPC max is ~511)
  /article \d{4,}/i,              // Article 1000+ in Constitution
  /ipc section [1-9]\d{3,}/i,    // IPC Section 1000+
];

/**
 * Detect if AI output contains silent legal advice.
 * Call this after moderation.checkOutput() passes.
 */
export function detectSilentLegalAdvice(
  content: string,
  userId: string,
  messageId: string,
  sessionId: string,
): boolean {
  const found = LEGAL_ADVICE_OUTPUT_PATTERNS.find(p => p.test(content));
  if (found) {
    reportSilentFailure('SILENT_LEGAL_ADVICE', {
      userId,
      sessionId,
      messageId,
      content,
      meta: { pattern: found.toString() },
    });
    return true;
  }
  return false;
}

/**
 * Detect possible hallucinated citations in AI output.
 */
export function detectHallucinatedCitations(
  content: string,
  userId: string,
  sessionId: string,
  messageId: string,
): void {
  HALLUCINATION_PATTERNS.forEach(p => {
    if (p.test(content)) {
      reportSilentFailure('SILENT_HALLUCINATED_CITE', {
        userId,
        sessionId,
        messageId,
        content: content.slice(0, 300),
        meta: { pattern: p.toString() },
      });
    }
  });
}

/**
 * Detect context loss: user has prior messages but AI context was empty.
 */
export function detectContextLoss(
  sessionId: string,
  userId: string,
  priorMessageCount: number,
  contextLength: number,
): void {
  if (priorMessageCount > 2 && contextLength === 0) {
    reportSilentFailure('SILENT_CONTEXT_LOSS', {
      userId,
      sessionId,
      meta: { priorMessageCount, contextLength },
    });
  }
}
