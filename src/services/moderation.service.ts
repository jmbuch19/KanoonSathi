import { UserRole } from '@prisma/client';
import { prisma } from '../db/prisma.js';
import { logger } from '../utils/logger.js';

// ─── Pattern Definitions ──────────────────────────────────────────────────────
// Layer 1: Fast synchronous pattern matching.
// These fire before ANY AI call, so cost is zero.

// Patterns that always result in a hard BLOCK (no AI call made)
const BLOCKED_PATTERNS = [
  /how\s+(do\s+i|can\s+i|to)\s+(destroy|fabricate|forge|tamper\s+with)\s+evidence/i,
  /help\s+me\s+(hide|destroy|plant)\s+evidence/i,
  /how\s+to\s+bribe\s+(a\s+)?(judge|police|officer|magistrate)/i,
  /draft\s+(a\s+)?(fake|forged|fraudulent|false)\s+(document|affidavit|fir|complaint)/i,
  /how\s+to\s+(avoid|escape|evade)\s+(police|arrest|prosecution|court|taxes)/i,
  /how\s+to\s+launder\s+money/i,
  /help\s+me\s+(commit|cover\s+up)\s+(a\s+)?(crime|fraud|theft|murder)/i,
];

// Patterns that trigger a WARNING and soft redirect (AI call still made with extra safety instructions)
const ADVICE_SEEKING_PATTERNS = [
  /should\s+i\s+(sue|file\s+a\s+case|go\s+to\s+court|appeal)/i,
  /am\s+i\s+(liable|guilty|at\s+fault|responsible)/i,
  /will\s+i\s+win\s+(the\s+case|in\s+court|my\s+appeal)/i,
  /is\s+(my\s+)?(landlord|employer|spouse|partner|company)\s+(breaking|violating)\s+the\s+law/i,
  /can\s+i\s+(sue|take\s+legal\s+action\s+against|file\s+a\s+case\s+against)/i,
  /what\s+are\s+my\s+chances\s+(of\s+winning|in\s+court)/i,
];

// PII patterns — detect if user is sharing sensitive personal information
const PII_PATTERNS = [
  /\b\d{12}\b/, // Aadhaar number (12 digits)
  /\b\d{10}\b/, // Phone number (10 digits) — might be noisy
  /pan\s*:?\s*[a-z]{5}\d{4}[a-z]/i, // PAN card
  /account\s*(?:no|number)\s*:?\s*\d{9,18}/i, // Bank account
];

// ─── Output Safety Checks ─────────────────────────────────────────────────────
// Patterns in the AI's RESPONSE that indicate it gave advice despite instructions

const OUTPUT_ADVICE_PATTERNS = [
  /you\s+should\s+(file|sue|go\s+to\s+court|appeal|contact\s+a\s+lawyer)/i,
  /i\s+recommend\s+(filing|suing|going\s+to\s+court)/i,
  /your\s+(landlord|employer|spouse)\s+is\s+(liable|guilty|at\s+fault)/i,
  /you\s+will\s+(likely\s+)?win/i,
  /you\s+have\s+a\s+strong\s+case/i,
  /as\s+your\s+(lawyer|advocate|legal\s+counsel)/i,
];

// ─── Response Messages ────────────────────────────────────────────────────────

const REFUSAL_MESSAGES = {
  harmful: `I'm not able to help with that. KanoonSaathi is designed for legal education only — understanding law, not circumventing it.\n\nIf you have a question about how a law works or what a legal concept means, I'm happy to help!`,

  legal_advice_redirect: `I notice you might be looking for advice on a specific situation. KanoonSaathi provides legal education only — I can explain the law, but I can't advise you on what to do in your personal case.\n\nFor your specific situation, please:\n• Consult a qualified advocate\n• Contact your District Legal Services Authority (DLSA) for free legal aid: **15100**\n• Use the National Legal Services Authority portal: **nalsa.gov.in**\n\nWould you like me to explain the relevant area of law instead?`,

  output_replaced: `I can help you understand this area of law generally.\n\n{ORIGINAL_RESPONSE}\n\n⚠️ **Important:** This is general legal information for educational purposes only. It is NOT legal advice. For advice on your specific situation, please consult a qualified advocate or contact DLSA (helpline: 15100).`,

  pii_warning: `I noticed you may have shared some personal information. For your privacy, please don't share sensitive details like Aadhaar numbers, PAN, bank details, or phone numbers in this chat.\n\nLet me help you understand the law generally without needing personal details.`,
};

// ─── Moderation Result Types ──────────────────────────────────────────────────

export interface InputModerationResult {
  blocked: boolean;
  flagged: boolean;
  reason?: string;
  score: number;
  refusalMessage?: string;
  warning?: string;
}

export interface OutputModerationResult {
  safe: boolean;
  score: number;
  safeReplacement?: string;
}

// ─── Main Moderation Functions ────────────────────────────────────────────────

export async function checkInput(
  content: string,
  userId: string,
  role: UserRole,
): Promise<InputModerationResult> {
  // Layer 1a: Hard block patterns
  for (const pattern of BLOCKED_PATTERNS) {
    if (pattern.test(content)) {
      logger.warn({ userId, role, pattern: pattern.source }, 'Input blocked by moderation');

      // Log the flag
      await logModerationFlag(userId, null, 'harmful', 'auto', 1.0);

      return {
        blocked: true,
        flagged: true,
        reason: 'harmful',
        score: 1.0,
        refusalMessage: REFUSAL_MESSAGES.harmful,
      };
    }
  }

  // Layer 1b: PII detection
  for (const pattern of PII_PATTERNS) {
    if (pattern.test(content)) {
      // Don't block — warn and continue
      return {
        blocked: false,
        flagged: true,
        reason: 'pii',
        score: 0.6,
        warning: REFUSAL_MESSAGES.pii_warning,
      };
    }
  }

  // Layer 1c: Legal advice seeking (stricter for CURIOUS role)
  if (role === 'CURIOUS') {
    for (const pattern of ADVICE_SEEKING_PATTERNS) {
      if (pattern.test(content)) {
        // For CURIOUS: soft redirect, don't block, AI will still respond with educational content
        return {
          blocked: false,
          flagged: true,
          reason: 'legal_advice',
          score: 0.7,
          warning: REFUSAL_MESSAGES.legal_advice_redirect,
        };
      }
    }
  }

  return { blocked: false, flagged: false, score: 0 };
}

export async function checkOutput(
  content: string,
  userId: string,
  role: UserRole,
): Promise<OutputModerationResult> {
  // Check if AI response contains advice-like statements despite instructions
  for (const pattern of OUTPUT_ADVICE_PATTERNS) {
    if (pattern.test(content)) {
      logger.warn({ userId, role, pattern: pattern.source }, 'Output moderation flag: possible legal advice');

      await logModerationFlag(userId, null, 'legal_advice', 'auto', 0.8);

      // For CURIOUS: replace response with safer version
      if (role === 'CURIOUS') {
        // Don't discard the response — add strong disclaimer wrapping
        const safeVersion = REFUSAL_MESSAGES.output_replaced.replace('{ORIGINAL_RESPONSE}', content);
        return { safe: false, score: 0.8, safeReplacement: safeVersion };
      }

      // For STUDENT/FACULTY: append disclaimer only (they understand the context better)
      const withDisclaimer = content + '\n\n⚠️ **Reminder:** This is educational information only. For your specific situation, consult a qualified advocate.';
      return { safe: false, score: 0.5, safeReplacement: withDisclaimer };
    }
  }

  return { safe: true, score: 0 };
}

// ─── Flag Logger ──────────────────────────────────────────────────────────────

async function logModerationFlag(
  userId: string,
  messageId: string | null,
  flagType: string,
  source: string,
  confidence: number,
): Promise<void> {
  try {
    await prisma.moderationFlag.create({
      data: {
        userId,
        messageId,
        flagType: flagType as 'legal_advice' | 'harmful' | 'abuse' | 'hallucination' | 'inappropriate' | 'pii',
        flagSource: source as 'auto' | 'user_report' | 'admin',
        confidence,
      },
    });

    // Check abuse threshold: if user has 5+ flags in 7 days → auto-suspend
    const recentFlagCount = await prisma.moderationFlag.count({
      where: {
        userId,
        createdAt: { gte: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) },
        flagSource: 'auto',
      },
    });

    if (recentFlagCount >= 5) {
      logger.warn({ userId, recentFlagCount }, 'User hit abuse threshold — suspending');
      await prisma.user.update({
        where: { id: userId },
        data: { status: 'suspended' },
      });
    }
  } catch (err) {
    // Non-critical: log error but don't fail the request
    logger.error({ err, userId }, 'Failed to log moderation flag');
  }
}

// ─── User Report ──────────────────────────────────────────────────────────────

export async function reportMessage(
  messageId: string,
  userId: string,
  reason: string,
): Promise<void> {
  const message = await prisma.chatMessage.findUnique({ where: { id: messageId } });
  if (!message) return;

  await prisma.moderationFlag.create({
    data: {
      userId,
      messageId,
      flagType: 'inappropriate',
      flagSource: 'user_report',
      confidence: 1.0,
    },
  });

  logger.info({ messageId, userId, reason }, 'Message reported by user');
}
