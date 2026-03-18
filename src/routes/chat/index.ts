import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import { z } from 'zod';
import { requireAuth } from '../../middleware/auth.middleware.js';
import {
  createChatSession,
  getChatSessions,
  getChatSession,
  deleteSession,
  toggleBookmark,
  sendMessage,
  getModesForRole,
} from '../../services/chat.service.js';
import { reportMessage } from '../../services/moderation.service.js';
import { SUGGESTED_PROMPTS } from '../../services/ai.service.js';
import { prisma } from '../../db/prisma.js';
import { ValidationError } from '../../utils/errors.js';
import { chatFreeLimiter } from '../../db/redis.js';
import { RateLimitError } from '../../utils/errors.js';
import { getExamState } from '../../services/exam.service.js';

const createSessionSchema = z.object({
  chatMode: z.string().min(1),
});

const sendMessageSchema = z.object({
  content: z.string().min(1, 'Message cannot be empty').max(5000, 'Message is too long (max 5000 characters)'),
});

const reportSchema = z.object({
  messageId: z.string().uuid(),
  reason: z.string().min(1).max(500),
});

export async function chatRoutes(fastify: FastifyInstance): Promise<void> {
  // Apply auth to all chat routes
  fastify.addHook('preHandler', requireAuth);

  // GET /api/v1/chat/modes — available chat modes for the user's role
  fastify.get('/modes', async (req: FastifyRequest, reply: FastifyReply) => {
    const modes = getModesForRole(req.user.role);
    return reply.send({ success: true, data: modes });
  });

  // GET /api/v1/chat/suggested — suggested prompts for role
  fastify.get('/suggested', async (req: FastifyRequest, reply: FastifyReply) => {
    const rolePrompts = SUGGESTED_PROMPTS[req.user.role] ?? {};
    return reply.send({ success: true, data: rolePrompts });
  });

  // POST /api/v1/chat/sessions — create new chat session
  fastify.post('/sessions', async (req: FastifyRequest, reply: FastifyReply) => {
    const result = createSessionSchema.safeParse(req.body);
    if (!result.success) throw new ValidationError('Invalid request', result.error.flatten());

    const session = await createChatSession(req.user.sub, req.user.role, result.data.chatMode);

    // Analytics
    prisma.analyticsEvent.create({
      data: {
        userId: req.user.sub,
        sessionId: session.id,
        eventType: 'chat_session_created',
        role: req.user.role,
        eventData: { chatMode: result.data.chatMode },
      },
    }).catch(() => {});

    return reply.code(201).send({ success: true, data: session });
  });

  // GET /api/v1/chat/sessions — list sessions
  fastify.get('/sessions', async (req: FastifyRequest, reply: FastifyReply) => {
    const query = req.query as { page?: string; limit?: string };
    const page = Math.max(1, parseInt(query.page ?? '1'));
    const limit = Math.min(50, parseInt(query.limit ?? '20'));

    const result = await getChatSessions(req.user.sub, page, limit);
    return reply.send({ success: true, data: result });
  });

  // GET /api/v1/chat/sessions/:id — get session with messages
  fastify.get('/sessions/:id', async (req: FastifyRequest, reply: FastifyReply) => {
    const { id } = req.params as { id: string };
    const session = await getChatSession(id, req.user.sub);
    return reply.send({ success: true, data: session });
  });

  // DELETE /api/v1/chat/sessions/:id
  fastify.delete('/sessions/:id', async (req: FastifyRequest, reply: FastifyReply) => {
    const { id } = req.params as { id: string };
    await deleteSession(id, req.user.sub);
    return reply.send({ success: true, data: { message: 'Chat deleted' } });
  });

  // PUT /api/v1/chat/sessions/:id/bookmark
  fastify.put('/sessions/:id/bookmark', async (req: FastifyRequest, reply: FastifyReply) => {
    const { id } = req.params as { id: string };
    const isBookmarked = await toggleBookmark(id, req.user.sub);
    return reply.send({ success: true, data: { isBookmarked } });
  });

  // POST /api/v1/chat/sessions/:id/messages — SEND A MESSAGE (main endpoint)
  fastify.post('/sessions/:id/messages', async (req: FastifyRequest, reply: FastifyReply) => {
    const { id } = req.params as { id: string };

    // Skip the hourly rate limiter when an active exam exists for this session.
    // Exam Q&A is cheap (Redis only, no AI call) and must never be throttled.
    const examActive = await getExamState(id).catch(() => null);
    if (!examActive) {
      const { success, remaining } = await chatFreeLimiter.limit(req.user.sub);
      if (!success) {
        throw new RateLimitError(`Hourly message limit reached. ${remaining} messages remaining.`);
      }
    }

    const result = sendMessageSchema.safeParse(req.body);
    if (!result.success) throw new ValidationError('Invalid message', result.error.flatten());

    // Get user's subscription plan
    const subscription = await prisma.subscription.findUnique({
      where: { userId: req.user.sub },
    });
    const plan = subscription?.plan ?? 'free';

    const response = await sendMessage(
      id,
      req.user.sub,
      req.user.role,
      result.data.content,
      plan,
    );

    return reply.send({ success: true, data: response });
  });

  // GET /api/v1/chat/bookmarks — all bookmarked sessions
  fastify.get('/bookmarks', async (req: FastifyRequest, reply: FastifyReply) => {
    const sessions = await prisma.chatSession.findMany({
      where: { userId: req.user.sub, isBookmarked: true, deletedAt: null },
      orderBy: { updatedAt: 'desc' },
      select: {
        id: true,
        chatMode: true,
        title: true,
        messageCount: true,
        createdAt: true,
        updatedAt: true,
      },
    });
    return reply.send({ success: true, data: sessions });
  });

  // POST /api/v1/chat/report — report a message
  fastify.post('/report', async (req: FastifyRequest, reply: FastifyReply) => {
    const result = reportSchema.safeParse(req.body);
    if (!result.success) throw new ValidationError('Invalid report', result.error.flatten());

    await reportMessage(result.data.messageId, req.user.sub, result.data.reason);
    return reply.send({ success: true, data: { message: 'Report submitted. Thank you.' } });
  });
}
