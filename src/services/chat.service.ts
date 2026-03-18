import { UserRole } from '@prisma/client';
import { prisma } from '../db/prisma.js';
import { clearChatContext, appendToChatContext } from '../db/redis.js';
import { generateAIResponse, generateChatTitle } from './ai.service.js';
import { checkInput, checkOutput } from './moderation.service.js';
import { detectSilentLegalAdvice, detectHallucinatedCitations } from './alerts.service.js';
import { NotFoundError, ForbiddenError, QuotaError } from '../utils/errors.js';
import { CHAT_MODES, PLAN_LIMITS } from '../types/index.js';
import { logger } from '../utils/logger.js';
import { shouldTriggerExam, initExamSession, handleExamAnswer } from './exam.service.js';

// ─── Chat Modes ───────────────────────────────────────────────────────────────

export function getModesForRole(role: UserRole) {
  return CHAT_MODES.filter((m) => m.roles.includes(role));
}

// ─── Continuity Greeting ──────────────────────────────────────────────────────
// When a user opens a new chat, greet them by name and reference their last
// session. Saved as the first assistant message — no AI call needed.

async function getUserFirstName(userId: string, role: UserRole): Promise<string> {
  try {
    if (role === 'STUDENT') {
      const p = await prisma.studentProfile.findUnique({ where: { userId }, select: { fullName: true } });
      const name = p?.fullName?.trim().split(' ')[0];
      return name || 'there';
    }
    if (role === 'FACULTY') {
      const p = await prisma.facultyProfile.findUnique({ where: { userId }, select: { fullName: true } });
      const name = p?.fullName?.trim().split(' ')[0];
      return name ? `Prof. ${name}` : 'Professor';
    }
    if (role === 'CURIOUS') {
      const p = await prisma.curiousProfile.findUnique({ where: { userId }, select: { displayName: true } });
      const name = p?.displayName?.trim().split(' ')[0];
      return name || 'there';
    }
  } catch { /* non-critical */ }
  return 'there';
}

function relativeDay(date: Date): string {
  const diffMs  = Date.now() - date.getTime();
  const diffH   = Math.floor(diffMs / 3_600_000);
  const diffD   = Math.floor(diffMs / 86_400_000);
  if (diffH < 1)  return 'just now';
  if (diffH < 6)  return 'a little while ago';
  if (diffH < 20) return 'earlier today';
  if (diffD < 2)  return 'yesterday';
  if (diffD < 7)  return `${diffD} days ago`;
  return 'a while back';
}

async function buildContinuityGreeting(
  userId:           string,
  role:             UserRole,
  currentSessionId: string,
): Promise<string | null> {
  try {
    const [firstName, lastSession] = await Promise.all([
      getUserFirstName(userId, role),
      prisma.chatSession.findFirst({
        where: {
          userId,
          id:           { not: currentSessionId },
          deletedAt:    null,
          messageCount: { gt: 0 },   // must have had real exchanges
        },
        orderBy: { updatedAt: 'desc' },
        select:  { title: true, chatMode: true, updatedAt: true },
      }),
    ]);

    if (!lastSession) return null;   // brand-new user — no greeting needed

    // Use generated title if available; otherwise prettify the chatMode id
    const topic = lastSession.title
      ?? lastSession.chatMode
           .replace(/_/g, ' ')
           .replace(/\b\w/g, (c) => c.toUpperCase());

    const when = relativeDay(lastSession.updatedAt);

    return `Hey ${firstName}! 👋 ${when.charAt(0).toUpperCase() + when.slice(1)} we were exploring **"${topic}"**. Want to pick up from there, or is there something new on your mind today?`;
  } catch {
    return null;   // non-critical — never let this block session creation
  }
}

// ─── Session Management ───────────────────────────────────────────────────────

export async function createChatSession(
  userId: string,
  role: UserRole,
  chatMode: string,
): Promise<{ id: string; chatMode: string; title: string | null }> {
  // Validate mode is allowed for this role
  const allowed = CHAT_MODES.find((m) => m.id === chatMode && m.roles.includes(role));
  if (!allowed) {
    throw new ForbiddenError(`Chat mode "${chatMode}" is not available for your role.`);
  }

  const session = await prisma.chatSession.create({
    data: {
      userId,
      roleAtCreation: role,
      chatMode,
    },
  });

  // Generate continuity greeting and save as the first assistant message.
  // We intentionally do NOT call updateSessionStats here — the greeting is a
  // welcome prompt, not a real message exchange, so messageCount stays at 0.
  // This ensures title auto-generation still triggers on the user's first message.
  const greeting = await buildContinuityGreeting(userId, role, session.id);
  if (greeting) {
    await prisma.chatMessage.create({
      data: {
        sessionId: session.id,
        userId,
        role: 'assistant',
        content: greeting,
      },
    }).catch(() => {}); // non-critical — don't fail the whole request
  }

  // Micro-exam: for returning students, show a knowledge check based on
  // their recent study history. Exam messages are quota-free (no updateSessionStats).
  // Pass firstName='' when a greeting is already shown to avoid a double "Hey Name!" intro.
  if (role === 'STUDENT') {
    const examNeeded = await shouldTriggerExam(userId, role).catch(() => false);
    if (examNeeded) {
      const examFirstName = greeting ? '' : await getUserFirstName(userId, role);
      const examQuestion  = await initExamSession(userId, session.id, examFirstName);
      if (examQuestion) {
        await prisma.chatMessage.create({
          data: { sessionId: session.id, userId, role: 'assistant', content: examQuestion },
        }).catch(() => {}); // non-critical
      }
    }
  }

  return { id: session.id, chatMode: session.chatMode, title: session.title };
}

export async function getChatSessions(userId: string, page = 1, limit = 20) {
  const skip = (page - 1) * limit;

  const [sessions, total] = await Promise.all([
    prisma.chatSession.findMany({
      where: { userId, deletedAt: null },
      orderBy: { updatedAt: 'desc' },
      skip,
      take: limit,
      select: {
        id: true,
        chatMode: true,
        title: true,
        isBookmarked: true,
        messageCount: true,
        createdAt: true,
        updatedAt: true,
      },
    }),
    prisma.chatSession.count({ where: { userId, deletedAt: null } }),
  ]);

  return {
    sessions,
    pagination: { page, limit, total, pages: Math.ceil(total / limit) },
  };
}

export async function getChatSession(sessionId: string, userId: string) {
  const session = await prisma.chatSession.findFirst({
    where: { id: sessionId, userId, deletedAt: null },
    include: {
      messages: {
        orderBy: { createdAt: 'asc' },
        select: {
          id: true,
          role: true,
          content: true,
          createdAt: true,
          wasRefused: true,
          moderationFlagged: true,
        },
      },
    },
  });

  if (!session) throw new NotFoundError('Chat session not found');
  return session;
}

export async function deleteSession(sessionId: string, userId: string): Promise<void> {
  const session = await prisma.chatSession.findFirst({
    where: { id: sessionId, userId, deletedAt: null },
  });
  if (!session) throw new NotFoundError('Chat session not found');

  await prisma.chatSession.update({
    where: { id: sessionId },
    data: { deletedAt: new Date() },
  });

  await clearChatContext(sessionId);
}

export async function toggleBookmark(
  sessionId: string,
  userId: string,
): Promise<boolean> {
  const session = await prisma.chatSession.findFirst({
    where: { id: sessionId, userId, deletedAt: null },
  });
  if (!session) throw new NotFoundError('Chat session not found');

  const updated = await prisma.chatSession.update({
    where: { id: sessionId },
    data: { isBookmarked: !session.isBookmarked },
  });

  return updated.isBookmarked;
}

// ─── Message Sending ──────────────────────────────────────────────────────────

export interface SendMessageResult {
  userMessage: { id: string; content: string; role: string; createdAt: Date };
  aiMessage: { id: string; content: string; role: string; createdAt: Date };
  refused: boolean;
  warning?: string;
}

export async function sendMessage(
  sessionId: string,
  userId: string,
  role: UserRole,
  content: string,
  plan: string,
): Promise<SendMessageResult> {
  // 1. Verify session ownership
  const session = await prisma.chatSession.findFirst({
    where: { id: sessionId, userId, deletedAt: null },
  });
  if (!session) throw new NotFoundError('Chat session not found');

  // 1.5. EXAM INTERCEPT ─────────────────────────────────────────────────────
  // Must run before the quota/rate-limit checks so exam Q&A never consumes the
  // user's daily token budget.
  //
  // handleExamAnswer returns:
  //   { message, done } → exam was active & processed the input (save & return)
  //   null (exam active, real message) → exam was ejected, fall through to AI
  //   null (no exam)                   → no exam in progress, fall through to AI
  const examResult = await handleExamAnswer(sessionId, userId, content).catch(() => null);
  if (examResult !== null) {
    const inputMod = await checkInput(content, userId, role).catch(() => ({ score: 0, flagged: false, blocked: false }));
    const userMsg = await prisma.chatMessage.create({
      data: {
        sessionId,
        userId,
        role:                'user',
        content,
        moderationScore:     inputMod.score,
        moderationFlagged:   inputMod.flagged,
        wasRefused:          false,
      },
    });
    const examMsg = await prisma.chatMessage.create({
      data: { sessionId, userId, role: 'assistant', content: examResult.message },
    });
    // Intentionally no updateSessionStats / updateUsageLogs — exam is quota-free
    return {
      userMessage: toMessageDto(userMsg),
      aiMessage:   toMessageDto(examMsg),
      refused:     false,
    };
  }
  // ─────────────────────────────────────────────────────────────────────────

  // 2. Check daily token quota
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const usageLog = await prisma.usageLog.findFirst({
    where: { userId, date: today },
  });
  const planKey = plan as keyof typeof PLAN_LIMITS;
  const limits = PLAN_LIMITS[planKey] ?? PLAN_LIMITS.free;

  if ((usageLog?.tokenCount ?? 0) >= limits.tokensPerDay) {
    throw new QuotaError('Daily token limit reached. Upgrade your plan or try again tomorrow.');
  }

  // 3. Input moderation
  const inputMod = await checkInput(content, userId, role);

  // Save user message regardless (for audit)
  const userMsg = await prisma.chatMessage.create({
    data: {
      sessionId,
      userId,
      role: 'user',
      content,
      moderationScore: inputMod.score,
      moderationFlagged: inputMod.flagged,
      wasRefused: inputMod.blocked,
    },
  });

  // If blocked, return refusal without calling AI
  if (inputMod.blocked) {
    const refusalMsg = await prisma.chatMessage.create({
      data: {
        sessionId,
        userId,
        role: 'assistant',
        content: inputMod.refusalMessage ?? 'I cannot help with that request.',
        wasRefused: true,
      },
    });

    await updateSessionStats(sessionId, 0);

    return {
      userMessage: toMessageDto(userMsg),
      aiMessage: toMessageDto(refusalMsg),
      refused: true,
    };
  }

  // 4. On the first real user message, prime Redis context with the greeting
  //    so the AI understands "Yes" / "Let's continue" type replies.
  if (session.messageCount === 0) {
    const greetingMsg = await prisma.chatMessage.findFirst({
      where:   { sessionId, role: 'assistant' },
      orderBy: { createdAt: 'asc' },
      select:  { content: true },
    });
    if (greetingMsg) {
      await appendToChatContext(sessionId, { role: 'assistant', content: greetingMsg.content });
    }
  }

  // 5. Generate AI response
  let aiContent: string;
  let tokensUsed = 0;
  let outputFlagged = false;

  try {
    const aiResponse = await generateAIResponse({
      sessionId,
      userId,
      role,
      chatMode: session.chatMode,
      userMessage: content,
    });

    tokensUsed = aiResponse.tokens;

    // 5. Output moderation
    const outputMod = await checkOutput(aiResponse.content, userId, role);
    aiContent = outputMod.safe ? aiResponse.content : (outputMod.safeReplacement ?? aiResponse.content);
    outputFlagged = !outputMod.safe;

  } catch (err) {
    logger.error({ err, sessionId, userId }, 'AI generation failed');
    aiContent = 'I\'m having trouble responding right now. Please try again in a moment. If the problem persists, try starting a new chat.';
  }

  // 6. Save AI response
  const aiMsg = await prisma.chatMessage.create({
    data: {
      sessionId,
      userId,
      role: 'assistant',
      content: aiContent,
      tokensUsed,
      moderationFlagged: outputFlagged,
    },
  });

  // 7. Silent failure detection — runs after save, never blocks response
  detectSilentLegalAdvice(aiContent, userId, aiMsg.id, sessionId);
  detectHallucinatedCitations(aiContent, userId, sessionId, aiMsg.id);

  // 8. Auto-generate title for first message
  if (session.messageCount === 0 && !session.title) {
    generateChatTitle(content).then((title) => {
      prisma.chatSession.update({
        where: { id: sessionId },
        data: { title },
      }).catch(() => {}); // non-critical
    });
  }

  // 8. Update stats
  await updateSessionStats(sessionId, tokensUsed);
  await updateUsageLogs(userId, tokensUsed);

  // 9. Analytics event (fire and forget)
  prisma.analyticsEvent.create({
    data: {
      userId,
      sessionId,
      eventType: 'message_sent',
      role: role.toString(),
      eventData: { chatMode: session.chatMode, tokensUsed },
    },
  }).catch(() => {});

  return {
    userMessage: toMessageDto(userMsg),
    aiMessage: toMessageDto(aiMsg),
    refused: false,
    warning: inputMod.warning,
  };
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function toMessageDto(msg: { id: string; content: string; role: string; createdAt: Date }) {
  return { id: msg.id, content: msg.content, role: msg.role, createdAt: msg.createdAt };
}

async function updateSessionStats(sessionId: string, tokens: number): Promise<void> {
  await prisma.chatSession.update({
    where: { id: sessionId },
    data: {
      messageCount: { increment: 2 }, // user + assistant
      totalTokens: { increment: tokens },
      updatedAt: new Date(),
    },
  });
}

async function updateUsageLogs(userId: string, tokens: number): Promise<void> {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  await prisma.usageLog.upsert({
    where: { userId_date: { userId, date: today } },
    update: {
      messageCount: { increment: 1 },
      tokenCount: { increment: tokens },
      apiCalls: { increment: 1 },
    },
    create: {
      userId,
      date: today,
      messageCount: 1,
      tokenCount: tokens,
      apiCalls: 1,
    },
  });
}
