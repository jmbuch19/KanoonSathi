import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import { prisma } from '../../db/prisma.js';
import { requireAdmin } from '../../middleware/auth.middleware.js';
import { ValidationError } from '../../utils/errors.js';
import { z } from 'zod';

export async function adminRoutes(fastify: FastifyInstance): Promise<void> {
  fastify.addHook('preHandler', requireAdmin);

  // GET /api/v1/admin/stats — overview dashboard stats
  fastify.get('/stats', async (_req: FastifyRequest, reply: FastifyReply) => {
    const [
      totalUsers,
      usersByRole,
      totalSessions,
      pendingFlags,
      todayUsage,
    ] = await Promise.all([
      prisma.user.count({ where: { status: 'active' } }),
      prisma.user.groupBy({ by: ['role'], _count: true, where: { status: 'active' } }),
      prisma.chatSession.count(),
      prisma.moderationFlag.count({ where: { reviewStatus: 'pending' } }),
      prisma.usageLog.aggregate({
        where: { date: { gte: new Date(new Date().setHours(0, 0, 0, 0)) } },
        _sum: { messageCount: true, tokenCount: true },
      }),
    ]);

    return reply.send({
      success: true,
      data: {
        totalUsers,
        usersByRole,
        totalSessions,
        pendingFlags,
        todayMessages: todayUsage._sum.messageCount ?? 0,
        todayTokens: todayUsage._sum.tokenCount ?? 0,
      },
    });
  });

  // GET /api/v1/admin/users — list users with search/filter
  fastify.get('/users', async (req: FastifyRequest, reply: FastifyReply) => {
    const query = req.query as { page?: string; limit?: string; role?: string; status?: string; search?: string };
    const page = Math.max(1, parseInt(query.page ?? '1'));
    const limit = Math.min(100, parseInt(query.limit ?? '20'));
    const skip = (page - 1) * limit;

    const where: Record<string, unknown> = {};
    if (query.role) where['role'] = query.role;
    if (query.status) where['status'] = query.status;
    if (query.search) {
      where['email'] = { contains: query.search, mode: 'insensitive' };
    }

    const [users, total] = await Promise.all([
      prisma.user.findMany({
        where,
        skip,
        take: limit,
        orderBy: { createdAt: 'desc' },
        select: {
          id: true,
          email: true,
          role: true,
          status: true,
          createdAt: true,
          lastLoginAt: true,
          subscription: { select: { plan: true } },
          _count: { select: { chatSessions: true, moderationFlags: true } },
        },
      }),
      prisma.user.count({ where }),
    ]);

    return reply.send({
      success: true,
      data: { users, pagination: { page, limit, total, pages: Math.ceil(total / limit) } },
    });
  });

  // PUT /api/v1/admin/users/:id/status — suspend or reactivate
  fastify.put('/users/:id/status', async (req: FastifyRequest, reply: FastifyReply) => {
    const { id } = req.params as { id: string };
    const schema = z.object({ status: z.enum(['active', 'suspended']) });
    const result = schema.safeParse(req.body);
    if (!result.success) throw new ValidationError('Invalid status');

    const user = await prisma.user.update({
      where: { id },
      data: { status: result.data.status },
    });

    // Audit log
    await prisma.auditLog.create({
      data: {
        actorId: req.adminUser?.sub,
        actorType: 'admin',
        action: `user.status.${result.data.status}`,
        targetId: id,
        targetType: 'user',
        ipAddress: req.ip,
      },
    });

    return reply.send({ success: true, data: { id: user.id, status: user.status } });
  });

  // GET /api/v1/admin/flags — moderation queue
  fastify.get('/flags', async (req: FastifyRequest, reply: FastifyReply) => {
    const query = req.query as { page?: string; status?: string };
    const page = Math.max(1, parseInt(query.page ?? '1'));
    const status = query.status ?? 'pending';
    const skip = (page - 1) * 20;

    const flags = await prisma.moderationFlag.findMany({
      where: { reviewStatus: status as 'pending' | 'dismissed' | 'actioned' },
      skip,
      take: 20,
      orderBy: { createdAt: 'asc' },
      include: {
        user: { select: { email: true, role: true } },
        message: { select: { content: true, sessionId: true } },
      },
    });

    return reply.send({ success: true, data: flags });
  });

  // PUT /api/v1/admin/flags/:id — resolve a flag
  fastify.put('/flags/:id', async (req: FastifyRequest, reply: FastifyReply) => {
    const { id } = req.params as { id: string };
    const schema = z.object({
      status: z.enum(['dismissed', 'actioned']),
      actionTaken: z.string().max(500).optional(),
    });
    const result = schema.safeParse(req.body);
    if (!result.success) throw new ValidationError('Invalid request');

    const flag = await prisma.moderationFlag.update({
      where: { id },
      data: {
        reviewStatus: result.data.status,
        actionTaken: result.data.actionTaken,
        reviewerId: req.adminUser?.sub,
        reviewedAt: new Date(),
      },
    });

    await prisma.auditLog.create({
      data: {
        actorId: req.adminUser?.sub,
        actorType: 'admin',
        action: `flag.${result.data.status}`,
        targetId: id,
        targetType: 'moderation_flag',
        metadata: { actionTaken: result.data.actionTaken },
      },
    });

    return reply.send({ success: true, data: flag });
  });

  // GET /api/v1/admin/prompts — list prompt templates
  fastify.get('/prompts', async (_req: FastifyRequest, reply: FastifyReply) => {
    const prompts = await prisma.promptTemplate.findMany({
      orderBy: [{ role: 'asc' }, { chatMode: 'asc' }, { version: 'desc' }],
    });
    return reply.send({ success: true, data: prompts });
  });

  // PUT /api/v1/admin/prompts/:id — update a prompt template
  fastify.put('/prompts/:id', async (req: FastifyRequest, reply: FastifyReply) => {
    const { id } = req.params as { id: string };
    const schema = z.object({
      content: z.string().min(50),
      isActive: z.boolean().optional(),
    });
    const result = schema.safeParse(req.body);
    if (!result.success) throw new ValidationError('Invalid prompt content');

    // Deactivate old version and create new version
    const existing = await prisma.promptTemplate.findUnique({ where: { id } });
    if (!existing) throw new Error('Prompt template not found');

    await prisma.promptTemplate.update({ where: { id }, data: { isActive: false } });

    const newPrompt = await prisma.promptTemplate.create({
      data: {
        role: existing.role,
        chatMode: existing.chatMode,
        templateType: existing.templateType,
        name: existing.name,
        content: result.data.content,
        version: existing.version + 1,
        isActive: result.data.isActive ?? true,
        createdById: req.adminUser?.sub,
      },
    });

    await prisma.auditLog.create({
      data: {
        actorId: req.adminUser?.sub,
        actorType: 'admin',
        action: 'prompt_template.updated',
        targetId: newPrompt.id,
        targetType: 'prompt_template',
      },
    });

    return reply.send({ success: true, data: newPrompt });
  });

  // GET /api/v1/admin/analytics — usage trends
  fastify.get('/analytics', async (req: FastifyRequest, reply: FastifyReply) => {
    const query = req.query as { days?: string };
    const days = Math.min(90, parseInt(query.days ?? '7'));
    const since = new Date(Date.now() - days * 24 * 60 * 60 * 1000);

    const [dailyUsage, topEvents, newUsers] = await Promise.all([
      prisma.usageLog.groupBy({
        by: ['date'],
        where: { date: { gte: since } },
        _sum: { messageCount: true, tokenCount: true },
        orderBy: { date: 'asc' },
      }),
      prisma.analyticsEvent.groupBy({
        by: ['eventType'],
        where: { createdAt: { gte: since } },
        _count: true,
        orderBy: { _count: { eventType: 'desc' } },
        take: 10,
      }),
      prisma.user.groupBy({
        by: ['role'],
        where: { createdAt: { gte: since } },
        _count: true,
      }),
    ]);

    return reply.send({ success: true, data: { dailyUsage, topEvents, newUsers } });
  });
}
