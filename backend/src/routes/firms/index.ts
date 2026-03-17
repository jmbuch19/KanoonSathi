import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import { z } from 'zod';
import { requireAuth } from '../../middleware/auth.middleware.js';
import {
  registerFirm,
  createPosting,
  getPostingsForStudent,
  getUnreadCount,
  getPublicPostings,
} from '../../services/firm.service.js';
import { ValidationError, ForbiddenError } from '../../utils/errors.js';

// ─── Validation Schemas ───────────────────────────────────────────────────────

export const VALID_SPECIALTIES = [
  'Criminal Law', 'Civil Law', 'Corporate Law', 'Constitutional Law',
  'Cyber Law', 'IPR & Technology Law', 'Family & Personal Law',
  'Labour & Employment Law', 'Tax Law', 'Real Estate Law',
  'Banking & Finance Law', 'Environmental Law', 'International Law',
  'Human Rights Law', 'Arbitration & ADR', 'General Practice',
];

const VALID_RADII = ['city', 'state', 'national'] as const;

const registerFirmSchema = z.object({
  officialName:      z.string().min(2).max(200),
  contactPersonName: z.string().min(2).max(100),
  officialEmail:     z.string().email(),
  websiteUrl:        z.string().url().optional().or(z.literal('')),
  city:              z.string().min(2).max(100),
  state:             z.string().min(2).max(100),
  specialties:       z.array(z.string()).min(1).max(8),
  termsAccepted:     z.literal(true, {
    errorMap: () => ({ message: 'You must accept the Terms & Conditions.' }),
  }),
});

const createPostingSchema = z.object({
  title:               z.string().min(5).max(200),
  description:         z.string().min(20).max(3000),
  specialtyAreas:      z.array(z.string()).min(1).max(6),
  yearOfStudyMin:      z.number().int().min(1).max(5).optional(),
  eligibilityCriteria: z.string().max(1000).optional(),
  applicationDeadline: z.string().datetime({ offset: true }).optional(),
  notificationRadius:  z.enum(VALID_RADII),
});

// ─── KanoonSaathi Bridge Terms & Conditions ───────────────────────────────────

export const TERMS_AND_CONDITIONS = `KanoonSaathi Internship Bridge — Terms & Conditions

1. PLATFORM ROLE
   KanoonSaathi is a bridge platform that facilitates connections between verified
   law firms and law students. KanoonSaathi is NOT a manpower provider, placement
   agency, or recruitment firm. We do not guarantee placements, interview calls, or
   employment offers of any kind.

2. FIRM RESPONSIBILITIES
   By registering, you confirm that:
   a) You represent a legitimate, registered law firm or legal organisation in India.
   b) All internship postings comply with applicable Indian labour and employment laws.
   c) Postings are genuine and open to qualified student candidates.
   d) You will not use any student data for purposes outside the specific internship listed.
   e) Your official email is tied to an active organisational domain, not a free
      consumer email service (Gmail, Yahoo, Outlook, etc.).

3. STUDENT PRIVACY
   Students' personal contact details are NOT shared with firms through this platform.
   All initial communication is mediated through KanoonSaathi's in-app system only.

4. NOTIFICATION TIERS
   When a posting is submitted, KanoonSaathi notifies students based on geography:
   - Tier 0 (City Radius): Students in approximately the same city (~100 km)
   - Tier 1 (State Radius): Students within the same state (~500 km)
   - Tier 2 (National): All registered students across India
   KanoonSaathi reserves the right to escalate notification reach if a narrower
   tier yields fewer than 5 matching students.

5. CONTENT STANDARDS
   All postings must be genuine, accurate, and non-discriminatory. KanoonSaathi
   reserves the right to remove any posting or suspend any firm account that
   violates these standards, misrepresents itself, or causes harm to students.

6. DISCLAIMER
   KanoonSaathi bears no liability for professional, contractual, or legal outcomes
   arising from firm–student interactions facilitated through this platform.

By submitting the registration form, you acknowledge, understand, and fully agree
to all the terms stated above.`.trim();

// ─── Routes ───────────────────────────────────────────────────────────────────

export async function firmRoutes(fastify: FastifyInstance): Promise<void> {

  // ── Public routes (no user auth required) ────────────────────────────────────

  // GET /api/v1/firms/terms
  fastify.get('/terms', async (_req: FastifyRequest, reply: FastifyReply) => {
    return reply.send({ success: true, data: { terms: TERMS_AND_CONDITIONS } });
  });

  // GET /api/v1/firms/specialties
  fastify.get('/specialties', async (_req: FastifyRequest, reply: FastifyReply) => {
    return reply.send({ success: true, data: { specialties: VALID_SPECIALTIES } });
  });

  // GET /api/v1/firms/postings/public  — landing page showcase
  fastify.get('/postings/public', async (req: FastifyRequest, reply: FastifyReply) => {
    const q = req.query as { limit?: string };
    const limit = Math.min(20, Math.max(1, parseInt(q.limit ?? '10')));
    const postings = await getPublicPostings(limit);
    return reply.send({ success: true, data: postings });
  });

  // POST /api/v1/firms  — firm self-registration
  fastify.post('/', async (req: FastifyRequest, reply: FastifyReply) => {
    const result = registerFirmSchema.safeParse(req.body);
    if (!result.success) throw new ValidationError('Invalid registration data', result.error.flatten());

    const registration = await registerFirm(result.data);

    return reply.code(201).send({
      success: true,
      data: {
        ...registration,
        terms: TERMS_AND_CONDITIONS,   // echo T&C for firm's own records
      },
    });
  });

  // POST /api/v1/firms/postings  — create internship posting (X-Firm-Token auth)
  fastify.post('/postings', async (req: FastifyRequest, reply: FastifyReply) => {
    const token = (req.headers['x-firm-token'] as string) ?? '';
    if (!token) throw new ForbiddenError('X-Firm-Token header is required to create a posting.');

    const result = createPostingSchema.safeParse(req.body);
    if (!result.success) throw new ValidationError('Invalid posting data', result.error.flatten());

    const posting = await createPosting(token, result.data);
    return reply.code(201).send({ success: true, data: posting });
  });

  // ── Protected student routes (requires valid user JWT) ────────────────────────
  // Scoped in a child plugin so the auth hook only applies here.

  fastify.register(async (studentScope: FastifyInstance) => {
    studentScope.addHook('preHandler', requireAuth);

    // GET /api/v1/firms/opportunities
    // Returns geo-targeted internship postings for the signed-in student.
    studentScope.get('/opportunities', async (req: FastifyRequest, reply: FastifyReply) => {
      // Only students and faculty see opportunities; other roles get empty list
      if (req.user.role !== 'STUDENT' && req.user.role !== 'FACULTY') {
        return reply.send({ success: true, data: [] });
      }
      const postings = await getPostingsForStudent(req.user.sub);
      return reply.send({ success: true, data: postings });
    });

    // GET /api/v1/firms/notifications/unread
    // Returns unread notification count for dashboard badge dot.
    studentScope.get('/notifications/unread', async (req: FastifyRequest, reply: FastifyReply) => {
      const count = await getUnreadCount(req.user.sub);
      return reply.send({ success: true, data: { count } });
    });
  });
}
