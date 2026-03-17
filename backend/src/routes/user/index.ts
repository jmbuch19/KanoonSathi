import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import { z } from 'zod';
import { requireAuth } from '../../middleware/auth.middleware.js';
import { prisma } from '../../db/prisma.js';
import { ValidationError, NotFoundError } from '../../utils/errors.js';
import { emailService } from '../../services/email.service.js';

// ─── Onboarding Schemas ───────────────────────────────────────────────────────

const studentOnboardingSchema = z.object({
  fullName: z.string().min(1).max(255),
  collegeName: z.string().min(1).max(500),
  yearOfStudy: z.number().int().min(1).max(5),
  semester: z.number().int().min(1).max(10),
  subjectsOfInterest: z.array(z.string()).max(10).default([]),
  examTarget: z.string().max(255).optional(),
});

const facultyOnboardingSchema = z.object({
  fullName: z.string().min(1).max(255),
  institutionName: z.string().min(1).max(500),
  designation: z.string().min(1).max(255),
  subjectsTaught: z.array(z.string()).max(10).default([]),
  barCouncilId: z.string().max(255).optional(),
});

const curiousOnboardingSchema = z.object({
  displayName: z.string().min(1).max(255),
  areasOfInterest: z.array(z.string()).max(10).default([]),
  disclaimerAccepted: z.literal(true, { errorMap: () => ({ message: 'You must accept the disclaimer to continue' }) }),
});

// ─── Profile Update Schemas (all fields optional for partial updates) ─────────

const studentProfileUpdateSchema = z.object({
  fullName: z.string().min(1).max(255).optional(),
  collegeName: z.string().min(1).max(500).optional(),
  yearOfStudy: z.number().int().min(1).max(5).optional(),
  semester: z.number().int().min(1).max(10).optional(),
  subjectsOfInterest: z.array(z.string()).max(10).optional(),
  examTarget: z.string().max(255).optional(),
});

const facultyProfileUpdateSchema = z.object({
  fullName: z.string().min(1).max(255).optional(),
  institutionName: z.string().min(1).max(500).optional(),
  designation: z.string().min(1).max(255).optional(),
  subjectsTaught: z.array(z.string()).max(10).optional(),
  barCouncilId: z.string().max(255).optional(),
});

const curiousProfileUpdateSchema = z.object({
  displayName: z.string().min(1).max(255).optional(),
  areasOfInterest: z.array(z.string()).max(10).optional(),
});

export async function userRoutes(fastify: FastifyInstance): Promise<void> {
  fastify.addHook('preHandler', requireAuth);

  // GET /api/v1/user/usage — daily usage stats
  fastify.get('/usage', async (req: FastifyRequest, reply: FastifyReply) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const [usage, subscription] = await Promise.all([
      prisma.usageLog.findFirst({ where: { userId: req.user.sub, date: today } }),
      prisma.subscription.findUnique({ where: { userId: req.user.sub } }),
    ]);

    return reply.send({
      success: true,
      data: {
        today: {
          messages: usage?.messageCount ?? 0,
          tokens: usage?.tokenCount ?? 0,
          apiCalls: usage?.apiCalls ?? 0,
        },
        plan: subscription?.plan ?? 'free',
      },
    });
  });

  // POST /api/v1/user/onboarding — submit onboarding data
  fastify.post('/onboarding', async (req: FastifyRequest, reply: FastifyReply) => {
    const { role, sub: userId } = req.user;

    if (role === 'STUDENT') {
      const result = studentOnboardingSchema.safeParse(req.body);
      if (!result.success) throw new ValidationError('Invalid onboarding data', result.error.flatten());

      await prisma.studentProfile.upsert({
        where: { userId },
        update: { ...result.data, onboardingComplete: true },
        create: { userId, ...result.data, onboardingComplete: true },
      });
    } else if (role === 'FACULTY') {
      const result = facultyOnboardingSchema.safeParse(req.body);
      if (!result.success) throw new ValidationError('Invalid onboarding data', result.error.flatten());

      await prisma.facultyProfile.upsert({
        where: { userId },
        update: { ...result.data, onboardingComplete: true },
        create: { userId, ...result.data, onboardingComplete: true },
      });
    } else if (role === 'CURIOUS') {
      const result = curiousOnboardingSchema.safeParse(req.body);
      if (!result.success) throw new ValidationError('Invalid onboarding data', result.error.flatten());

      await prisma.curiousProfile.upsert({
        where: { userId },
        update: { ...result.data, disclaimerAcceptedAt: new Date(), onboardingComplete: true },
        create: { userId, ...result.data, disclaimerAcceptedAt: new Date(), onboardingComplete: true },
      });
    }

    // Analytics
    prisma.analyticsEvent.create({
      data: { userId, eventType: 'onboarding_complete', role: role.toString() },
    }).catch(() => {});

    return reply.send({ success: true, data: { message: 'Profile saved. Welcome to KanoonSaathi!' } });
  });

  // PUT /api/v1/user/profile — update profile fields
  fastify.put('/profile', async (req: FastifyRequest, reply: FastifyReply) => {
    const { role, sub: userId } = req.user;

    if (role === 'STUDENT') {
      const result = studentProfileUpdateSchema.safeParse(req.body);
      if (!result.success) throw new ValidationError('Invalid profile data', result.error.flatten());
      if (Object.keys(result.data).length === 0) throw new ValidationError('No fields to update');
      await prisma.studentProfile.update({ where: { userId }, data: result.data });
    } else if (role === 'FACULTY') {
      const result = facultyProfileUpdateSchema.safeParse(req.body);
      if (!result.success) throw new ValidationError('Invalid profile data', result.error.flatten());
      if (Object.keys(result.data).length === 0) throw new ValidationError('No fields to update');
      await prisma.facultyProfile.update({ where: { userId }, data: result.data });
    } else if (role === 'CURIOUS') {
      const result = curiousProfileUpdateSchema.safeParse(req.body);
      if (!result.success) throw new ValidationError('Invalid profile data', result.error.flatten());
      if (Object.keys(result.data).length === 0) throw new ValidationError('No fields to update');
      await prisma.curiousProfile.update({ where: { userId }, data: result.data });
    }

    return reply.send({ success: true, data: { message: 'Profile updated' } });
  });

  // GET /api/v1/user/saved — saved/bookmarked items
  fastify.get('/saved', async (req: FastifyRequest, reply: FastifyReply) => {
    const items = await prisma.savedItem.findMany({
      where: { userId: req.user.sub },
      orderBy: { createdAt: 'desc' },
    });
    return reply.send({ success: true, data: items });
  });

  // POST /api/v1/user/saved — save an item
  fastify.post('/saved', async (req: FastifyRequest, reply: FastifyReply) => {
    const schema = z.object({
      itemType: z.enum(['message', 'session', 'prompt']),
      referenceId: z.string().uuid(),
      note: z.string().max(500).optional(),
    });

    const result = schema.safeParse(req.body);
    if (!result.success) throw new ValidationError('Invalid request', result.error.flatten());

    const item = await prisma.savedItem.upsert({
      where: {
        userId_itemType_referenceId: {
          userId: req.user.sub,
          itemType: result.data.itemType,
          referenceId: result.data.referenceId,
        },
      },
      update: { note: result.data.note },
      create: { userId: req.user.sub, ...result.data },
    });

    return reply.code(201).send({ success: true, data: item });
  });

  // DELETE /api/v1/user/saved/:id
  fastify.delete('/saved/:id', async (req: FastifyRequest, reply: FastifyReply) => {
    const { id } = req.params as { id: string };
    const item = await prisma.savedItem.findFirst({ where: { id, userId: req.user.sub } });
    if (!item) throw new NotFoundError('Saved item not found');

    await prisma.savedItem.delete({ where: { id } });
    return reply.send({ success: true, data: { message: 'Removed from saved items' } });
  });

  // ─── Account Self-Deletion (DPDP Act 2023 — Right to Erasure, Section 12) ───
  // DELETE /api/v1/user/me — permanently deletes the authenticated user and all their data.
  // All related records cascade-delete via Prisma schema FK constraints.
  // A confirmation email is sent to the user's address after deletion.
  fastify.delete('/me', async (req: FastifyRequest, reply: FastifyReply) => {
    const { sub: userId } = req.user;

    // Fetch display info BEFORE deletion so we can personalise the confirmation email
    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: {
        email: true,
        role: true,
        studentProfile: { select: { fullName: true } },
        facultyProfile:  { select: { fullName: true } },
        curiousProfile:  { select: { displayName: true } },
      },
    });

    if (!user) throw new NotFoundError('User not found');

    const displayName =
      user.studentProfile?.fullName ??
      user.facultyProfile?.fullName ??
      user.curiousProfile?.displayName ??
      'Valued User';

    const { email } = user;

    // Permanently delete the user record — Prisma cascade constraints remove
    // all profiles, sessions, chat sessions/messages, saved items, usage logs,
    // analytics events, internship notifications, and subscriptions.
    await prisma.user.delete({ where: { id: userId } });

    // Send DPDP-compliant confirmation email (fire-and-forget — data is already gone)
    emailService.sendAccountDeletion(email, displayName).catch(() => {});

    return reply.send({
      success: true,
      data: { message: 'Your account and all associated data have been permanently deleted. A confirmation email has been sent.' },
    });
  });
}
