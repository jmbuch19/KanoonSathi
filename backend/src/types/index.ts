import { UserRole } from '@prisma/client';

// ─── JWT Payload ──────────────────────────────────────────────────────────────
export interface JwtPayload {
  sub: string;        // user UUID
  role: UserRole;
  sessionId: string;
  iat?: number;
  exp?: number;
}

export interface AdminJwtPayload {
  sub: string;        // admin UUID
  adminRole: string;
  iat?: number;
  exp?: number;
}

// ─── Fastify Request Augmentation ─────────────────────────────────────────────
// After auth middleware runs, req.user is available everywhere.
declare module 'fastify' {
  interface FastifyRequest {
    user: JwtPayload;
    adminUser?: AdminJwtPayload;
  }
}

// ─── API Response Shapes ──────────────────────────────────────────────────────
export interface ApiSuccess<T = unknown> {
  success: true;
  data: T;
}

export interface ApiError {
  success: false;
  error: {
    code: string;
    message: string;
    details?: unknown;
  };
}

export type ApiResponse<T = unknown> = ApiSuccess<T> | ApiError;

// ─── Chat Types ───────────────────────────────────────────────────────────────
export interface ChatMode {
  id: string;
  name: string;
  description: string;
  icon: string;
  roles: UserRole[];
}

export const CHAT_MODES: ChatMode[] = [
  // Student modes
  { id: 'concept_explainer',   name: 'Concept Explainer',    description: 'Understand legal concepts clearly',         icon: '📖', roles: ['STUDENT', 'FACULTY'] },
  { id: 'case_summarizer',     name: 'Case Summarizer',      description: 'Summarize landmark Indian cases',            icon: '⚖️', roles: ['STUDENT', 'FACULTY'] },
  { id: 'bare_act_navigator',  name: 'Bare Act Navigator',   description: 'Explore, understand & summarise any statute',icon: '📜', roles: ['STUDENT', 'FACULTY'] },
  { id: 'exam_prep',           name: 'Exam Prep',            description: 'Prepare for CLAT PG, AIBE, Judicial Services',icon: '📝', roles: ['STUDENT'] },
  { id: 'quiz_mode',           name: 'Quiz Mode',            description: 'Test your legal knowledge with MCQs',        icon: '🎯', roles: ['STUDENT'] },
  { id: 'career_guidance',     name: 'Career Guidance',      description: 'Career paths, internships & progression',    icon: '🚀', roles: ['STUDENT'] },
  { id: 'drafting_assistant',  name: 'Drafting Assistant',   description: 'Learn legal drafting (educational)',         icon: '✍️', roles: ['STUDENT', 'FACULTY'] },
  // Faculty modes (5 boards — each matches a distinct teaching workflow)
  { id: 'concept_deepdive',  name: 'Knowledge Hub',       description: 'Deepen your mastery of subjects you teach',       icon: '🧠', roles: ['FACULTY'] },
  { id: 'lecture_notes',     name: 'Lecture Notes',        description: 'Build structured notes for your next class',        icon: '📝', roles: ['FACULTY'] },
  { id: 'discussion_board',  name: 'Discussion Board',     description: 'Socratic questions, debates & class activities',    icon: '💬', roles: ['FACULTY'] },
  { id: 'quiz_generator',    name: 'Assessments',          description: 'MCQs, essays & problem questions with answer keys', icon: '📋', roles: ['FACULTY'] },
  { id: 'case_analysis',     name: 'Case Analysis',        description: 'Deep-dive into judgments & precedents for class',   icon: '⚖️', roles: ['FACULTY'] },
  // Curious modes
  { id: 'rights_explainer',    name: 'Know Your Rights',     description: 'Understand your fundamental rights',         icon: '🏛️', roles: ['CURIOUS'] },
  { id: 'legal_terms',         name: 'Legal Terms',          description: 'Simple definitions of legal words',          icon: '📚', roles: ['CURIOUS'] },
  { id: 'everyday_law',        name: 'Everyday Law',         description: 'How law affects daily life',                 icon: '🏠', roles: ['CURIOUS'] },
];

// ─── Plan Limits ──────────────────────────────────────────────────────────────
export const PLAN_LIMITS = {
  free: {
    messagesPerHour: 30,
    tokensPerDay: 50_000,
  },
  basic: {
    messagesPerHour: 100,
    tokensPerDay: 200_000,
  },
  pro: {
    messagesPerHour: 999_999,
    tokensPerDay: 1_000_000,
  },
} as const;
