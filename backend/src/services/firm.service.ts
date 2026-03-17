import crypto from 'crypto';
import { prisma } from '../db/prisma.js';
import { logger } from '../utils/logger.js';
import { ValidationError, ForbiddenError, NotFoundError } from '../utils/errors.js';

// ─── Blocked Email Domains ────────────────────────────────────────────────────
// Law firms must register with an official organisational email, not a free/
// personal service. Any matching domain is rejected at registration time.

const CONSUMER_DOMAINS = new Set([
  // Global free providers
  'gmail.com', 'googlemail.com', 'yahoo.com', 'yahoo.co.in', 'yahoo.co.uk',
  'yahoo.in', 'hotmail.com', 'hotmail.co.in', 'hotmail.co.uk',
  'outlook.com', 'outlook.in', 'live.com', 'msn.com',
  'icloud.com', 'me.com', 'mac.com',
  // Indian consumer providers
  'rediffmail.com', 'rediff.com', 'indiatimes.com', 'sify.com',
  'in.com', 'dataone.in',
  // Other free services
  'ymail.com', 'rocketmail.com', 'aol.com',
  'protonmail.com', 'tutanota.com', 'pm.me',
  // Disposable / burner
  'mailinator.com', 'guerrillamail.com', 'temp-mail.org',
  '10minutemail.com', 'throwaway.email', 'yopmail.com',
  'sharklasers.com', 'guerrillamailblock.com', 'trashmail.com',
]);

function assertOfficialEmail(email: string): void {
  const parts = email.toLowerCase().trim().split('@');
  const domain = parts[1];
  if (!domain || CONSUMER_DOMAINS.has(domain)) {
    throw new ValidationError(
      'A professional firm or organisation email is required. ' +
      'Free or personal email services (Gmail, Yahoo, Outlook, etc.) are not accepted. ' +
      'Please use your official firm email (e.g. name@lawfirmname.com).',
    );
  }
}

// ─── Firm Registration ────────────────────────────────────────────────────────

export interface RegisterFirmInput {
  officialName:      string;
  contactPersonName: string;
  officialEmail:     string;
  websiteUrl?:       string;
  city:              string;
  state:             string;
  specialties:       string[];  // e.g. ['Criminal Law', 'Cyber Law', 'Corporate Law']
  termsAccepted:     boolean;
}

export async function registerFirm(data: RegisterFirmInput) {
  if (!data.termsAccepted) {
    throw new ValidationError(
      'You must read and accept the Terms & Conditions before registering.',
    );
  }

  const email = data.officialEmail.toLowerCase().trim();
  assertOfficialEmail(email);

  const existing = await prisma.lawFirm.findUnique({ where: { officialEmail: email } });
  if (existing) {
    throw new ValidationError(
      'A registration already exists for this email address. ' +
      'If you believe this is an error, contact support@kanoonsaathi.in.',
    );
  }

  if (!data.specialties || data.specialties.length === 0) {
    throw new ValidationError('Please select at least one area of practice.');
  }

  // Generate a one-time auth token for posting internships.
  // This is returned ONCE — the firm is responsible for storing it securely.
  const firmToken = crypto.randomBytes(32).toString('hex');

  const firm = await prisma.lawFirm.create({
    data: {
      officialName:      data.officialName.trim(),
      contactPersonName: data.contactPersonName.trim(),
      officialEmail:     email,
      websiteUrl:        data.websiteUrl?.trim() || null,
      city:              data.city.trim(),
      state:             data.state.trim(),
      specialties:       data.specialties,
      firmToken,
      termsAccepted:     true,
    },
  });

  logger.info({ firmId: firm.id, email: firm.officialEmail, city: firm.city },
    '[Firm] New law firm registered');

  return {
    id:         firm.id,
    firmToken,                             // ⚠️  returned exactly once
    officialName: firm.officialName,
    message:    'Registration successful! ' +
                'Please save your Firm Token — it will not be shown again. ' +
                'Use it in the X-Firm-Token header to create internship postings.',
  };
}

// ─── Firm Authentication ──────────────────────────────────────────────────────

async function getFirmByToken(token: string) {
  if (!token?.trim()) throw new ForbiddenError('Firm token is required.');
  const firm = await prisma.lawFirm.findUnique({ where: { firmToken: token.trim() } });
  if (!firm)          throw new ForbiddenError('Invalid firm token.');
  if (firm.status !== 'active') {
    throw new ForbiddenError('This firm account has been suspended. Contact support@kanoonsaathi.in.');
  }
  return firm;
}

// ─── Create Internship Posting ────────────────────────────────────────────────

export interface CreatePostingInput {
  title:               string;
  description:         string;
  specialtyAreas:      string[];
  yearOfStudyMin?:     number;    // e.g. 4 = final year only; omit for any year
  eligibilityCriteria?: string;
  applicationDeadline?: string;  // ISO-8601 date string
  notificationRadius:  'city' | 'state' | 'national';
}

export async function createPosting(firmToken: string, data: CreatePostingInput) {
  const firm = await getFirmByToken(firmToken);

  const deadline = data.applicationDeadline
    ? new Date(data.applicationDeadline)
    : null;

  if (deadline && deadline < new Date()) {
    throw new ValidationError('Application deadline cannot be in the past.');
  }

  const posting = await prisma.internshipPosting.create({
    data: {
      firmId:              firm.id,
      title:               data.title.trim(),
      description:         data.description.trim(),
      specialtyAreas:      data.specialtyAreas,
      yearOfStudyMin:      data.yearOfStudyMin ?? null,
      eligibilityCriteria: data.eligibilityCriteria?.trim() ?? null,
      applicationDeadline: deadline,
      notificationRadius:  data.notificationRadius,
    },
  });

  logger.info({ postingId: posting.id, firmId: firm.id, radius: data.notificationRadius },
    '[Firm] Internship posting created');

  // Dispatch notifications asynchronously — never block the HTTP response.
  dispatchNotifications(posting, firm).catch((err) =>
    logger.error({ err, postingId: posting.id }, '[Firm] Notification dispatch failed'),
  );

  return {
    id:      posting.id,
    message: 'Internship posted! We are notifying matching students now.',
  };
}

// ─── Tier-based Notification Dispatch ────────────────────────────────────────
// Tier 0 → same city         (~100 km radius)
// Tier 1 → same state        (~500 km radius)
// Tier 2 → nationwide        (all active students)
//
// If a narrower tier yields fewer than MIN_NOTIFY students, the system
// automatically escalates to the next tier so no posting goes unseen.

const MIN_NOTIFY = 5;

async function dispatchNotifications(
  posting: { id: string; notificationRadius: string; yearOfStudyMin: number | null },
  firm:    { city: string; state: string },
): Promise<void> {

  const yearFilter = posting.yearOfStudyMin
    ? { yearOfStudy: { gte: posting.yearOfStudyMin } }
    : {};

  const baseSP = { onboardingComplete: true, ...yearFilter };

  // Helper — fetch students with optional geo filter
  const fetchStudents = (geoFilter: object) =>
    prisma.user.findMany({
      where: {
        role:      'STUDENT',
        deletedAt: null,
        status:    'active',
        studentProfile: { ...baseSP, ...geoFilter },
      },
      select: { id: true },
    });

  let students: { id: string }[] = [];
  let tier = 2;

  if (posting.notificationRadius === 'city') {
    students = await fetchStudents({ city: { equals: firm.city, mode: 'insensitive' } });
    tier = 0;
    if (students.length < MIN_NOTIFY) {
      students = await fetchStudents({ state: { equals: firm.state, mode: 'insensitive' } });
      tier = 1;
    }
  } else if (posting.notificationRadius === 'state') {
    students = await fetchStudents({ state: { equals: firm.state, mode: 'insensitive' } });
    tier = 1;
  }

  // Always escalate to national if still too few
  if (students.length < MIN_NOTIFY) {
    students = await fetchStudents({});
    tier = 2;
  }

  if (students.length === 0) {
    logger.warn({ postingId: posting.id }, '[Firm] No students matched for notification');
    return;
  }

  // Bulk-insert — skipDuplicates guards against double-dispatch on retries
  await prisma.studentInternshipNotification.createMany({
    data:           students.map((s) => ({ userId: s.id, postingId: posting.id, tier })),
    skipDuplicates: true,
  });

  await prisma.internshipPosting.update({
    where: { id: posting.id },
    data:  { notificationsSent: true },
  });

  logger.info({ postingId: posting.id, count: students.length, tier },
    '[Firm] Notifications dispatched');
}

// ─── Student: Browse Opportunities ───────────────────────────────────────────

export async function getPostingsForStudent(userId: string) {
  // Get all notifications for this student (newest first, max 30)
  const notifications = await prisma.studentInternshipNotification.findMany({
    where:   { userId },
    orderBy: { createdAt: 'desc' },
    take:    30,
    include: {
      posting: {
        include: {
          firm: {
            select: {
              officialName: true,
              city:         true,
              state:        true,
              specialties:  true,
              websiteUrl:   true,
            },
          },
        },
      },
    },
  });

  // Mark unread as read
  const unreadIds = notifications.filter((n) => !n.isRead).map((n) => n.id);
  if (unreadIds.length > 0) {
    await prisma.studentInternshipNotification.updateMany({
      where: { id: { in: unreadIds } },
      data:  { isRead: true },
    });
  }

  return notifications
    .filter((n) => n.posting.status === 'active')
    .map((n) => ({
      id:                  n.posting.id,
      title:               n.posting.title,
      description:         n.posting.description,
      specialtyAreas:      n.posting.specialtyAreas,
      yearOfStudyMin:      n.posting.yearOfStudyMin,
      eligibilityCriteria: n.posting.eligibilityCriteria,
      applicationDeadline: n.posting.applicationDeadline,
      firm: {
        name:        n.posting.firm.officialName,
        city:        n.posting.firm.city,
        state:       n.posting.firm.state,
        specialties: n.posting.firm.specialties,
        website:     n.posting.firm.websiteUrl,
      },
      isNew:    !n.isRead,   // already updated above — so always false after this call
      tier:     n.tier,
      postedAt: n.posting.createdAt,
    }));
}

// ─── Student: Unread notification count ──────────────────────────────────────

export async function getUnreadCount(userId: string): Promise<number> {
  return prisma.studentInternshipNotification.count({
    where: { userId, isRead: false },
  });
}

// ─── Public: List all active postings (no auth — for landing page) ────────────

export async function getPublicPostings(limit = 10) {
  const postings = await prisma.internshipPosting.findMany({
    where:   { status: 'active', notificationsSent: true },
    orderBy: { createdAt: 'desc' },
    take:    limit,
    include: {
      firm: {
        select: { officialName: true, city: true, state: true, specialties: true },
      },
    },
  });

  return postings.map((p) => ({
    id:             p.id,
    title:          p.title,
    specialtyAreas: p.specialtyAreas,
    firm: {
      name:  p.firm.officialName,
      city:  p.firm.city,
      state: p.firm.state,
    },
    deadline: p.applicationDeadline,
    postedAt: p.createdAt,
  }));
}
