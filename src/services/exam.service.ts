import Anthropic from '@anthropic-ai/sdk';
import { redis } from '../db/redis.js';
import { prisma } from '../db/prisma.js';
import { config } from '../config/index.js';
import { logger } from '../utils/logger.js';

const anthropic = new Anthropic({ apiKey: config.ANTHROPIC_API_KEY });

// ─── Redis Key Helpers ────────────────────────────────────────────────────────

const examStateKey = (sessionId: string) => `exam_state:${sessionId}`;
const examLastKey  = (userId: string)    => `exam_last:${userId}`;

const EXAM_STATE_TTL    = 4 * 60 * 60;        // 4 hours
const EXAM_COOLDOWN_TTL = 2 * 24 * 60 * 60;   // 48-hour cooldown between exams

// ─── Types ────────────────────────────────────────────────────────────────────

export interface ExamQuestion {
  q:           string;
  options:     string[];  // ['A. ...', 'B. ...', 'C. ...', 'D. ...']
  answer:      string;    // 'A' | 'B' | 'C' | 'D'
  explanation: string;
}

export interface ExamState {
  questions: ExamQuestion[];
  current:   number;
  score:     number;
}

// ─── shouldTriggerExam ────────────────────────────────────────────────────────
// Returns true only when:
//   1. Role is STUDENT
//   2. Not in cooldown (exam was not shown in the last 48h)
//   3. User has ≥ 3 messages in the last 7 days (enough content to build from)

export async function shouldTriggerExam(userId: string, role: string): Promise<boolean> {
  if (role !== 'STUDENT') return false;

  const lastExam = await redis.get(examLastKey(userId));
  if (lastExam) return false;

  const since = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
  const count = await prisma.chatMessage.count({
    where: { userId, role: 'user', createdAt: { gte: since } },
  });

  return count >= 3;
}

// ─── initExamSession ──────────────────────────────────────────────────────────
// Fetches recent user messages using an adaptive time window (48h → 72h → 124h),
// calls Claude Haiku to generate 5 MCQs, saves state to Redis, and returns the
// formatted first question string.
//
// Pass firstName='' when a continuity greeting is already queued for display
// in the same session (avoids a double greeting with the student's name).

export async function initExamSession(
  userId:    string,
  sessionId: string,
  firstName: string,
): Promise<string | null> {
  try {
    // Adaptive window: try 48h first, fall back to 72h, then 124h
    const windows = [48, 72, 124].map(h => new Date(Date.now() - h * 3_600_000));
    let messages: { content: string }[] = [];

    for (const since of windows) {
      messages = await prisma.chatMessage.findMany({
        where:   { userId, role: 'user', createdAt: { gte: since } },
        orderBy: { createdAt: 'asc' },
        select:  { content: true },
      });
      if (messages.length >= 3) break;
    }

    if (messages.length < 3) return null;

    const context = messages.map(m => m.content).join('\n---\n').slice(0, 2000);

    const prompt = `You are a legal education quiz generator for Indian law students.

Based on the following recent study queries from a student, generate exactly 5 MCQ questions.
Each question must test understanding of Indian law concepts mentioned in the queries.

RECENT STUDY TOPICS:
${context}

OUTPUT: Respond with ONLY a valid JSON array (no markdown, no explanation):
[
  {
    "q": "Question text here?",
    "options": ["A. option1", "B. option2", "C. option3", "D. option4"],
    "answer": "B",
    "explanation": "Brief explanation of why B is correct."
  }
]

Rules:
- Exactly 5 questions
- All options must relate to Indian law (IPC, CrPC, CPC, Constitution, etc.)
- Only one correct answer per question
- The "answer" field must be a single capital letter: A, B, C, or D
- Keep questions clear and exam-appropriate`;

    const res = await anthropic.messages.create({
      model:       'claude-haiku-4-5-20251001',
      max_tokens:  1500,
      temperature: 0.4,
      messages:    [{ role: 'user', content: prompt }],
    });

    const raw = res.content[0].type === 'text' ? res.content[0].text : '';
    const jsonMatch = raw.match(/\[[\s\S]*\]/);
    if (!jsonMatch) {
      logger.warn({ sessionId, userId }, '[Exam] Could not parse MCQ JSON from Haiku response');
      return null;
    }

    const questions: ExamQuestion[] = JSON.parse(jsonMatch[0]);
    if (!Array.isArray(questions) || questions.length === 0) return null;

    const state: ExamState = { questions, current: 0, score: 0 };
    await redis.setex(examStateKey(sessionId), EXAM_STATE_TTL, JSON.stringify(state));

    return formatQuestion(questions[0], 1, questions.length, firstName);
  } catch (err) {
    logger.error({ err, sessionId, userId }, '[Exam] Failed to initialise exam session');
    return null;
  }
}

// ─── getExamState ─────────────────────────────────────────────────────────────

export async function getExamState(sessionId: string): Promise<ExamState | null> {
  const raw = await redis.get<string>(examStateKey(sessionId));
  if (!raw) return null;
  try {
    return typeof raw === 'string' ? JSON.parse(raw) as ExamState : raw as ExamState;
  } catch {
    return null;
  }
}

// ─── handleExamAnswer ─────────────────────────────────────────────────────────
// Returns null when:
//   a) No active exam for this session  → fall through to normal AI
//   b) User typed a real question        → exam auto-exits, fall through to AI
//
// Returns { message, done } when exam was active and the input was processed
// (correct/wrong answer, or intentional skip).  Caller should save these to DB
// but must NOT call updateUsageLogs — exam interactions are quota-free.

export async function handleExamAnswer(
  sessionId: string,
  userId:    string,
  userInput: string,
): Promise<{ message: string; done: boolean } | null> {
  const state = await getExamState(sessionId);
  if (!state) return null;

  const { questions, current, score } = state;

  // Intentional skip / exit
  const skipPattern = /\b(skip|bypass|exit|stop|cancel|quit|later|nevermind|never\s*mind)\b/i;
  if (skipPattern.test(userInput) && userInput.length < 80) {
    await redis.del(examStateKey(sessionId));
    return {
      message: `No problem! 😊 Quiz cleared. What would you like to explore today?`,
      done:    true,
    };
  }

  // Single-letter answer: A / B / C / D (with optional trailing punctuation)
  const match = userInput.trim().match(/^([A-Da-d])[.):\s]*$/);
  if (!match) {
    // Real message → auto-exit exam, return null so caller falls through to AI
    await redis.del(examStateKey(sessionId));
    return null;
  }

  const userAnswer = match[1].toUpperCase();
  const q          = questions[current];
  const isCorrect  = userAnswer === q.answer;
  const newScore   = isCorrect ? score + 1 : score;
  const isFinal    = current >= questions.length - 1;

  const feedback = isCorrect
    ? `✅ Correct! ${q.explanation}`
    : `❌ Not quite — the correct answer is **${q.answer}**. ${q.explanation}`;

  if (isFinal) {
    await redis.del(examStateKey(sessionId));
    // Record cooldown — don't show exam again for 48 hours
    await redis.setex(examLastKey(userId), EXAM_COOLDOWN_TTL, Date.now().toString());

    const total = questions.length;
    const pct   = Math.round((newScore / total) * 100);
    const grade = pct >= 80 ? '🌟 Excellent!' : pct >= 60 ? '👍 Good effort!' : '📚 Keep revising!';

    return {
      message: `${feedback}\n\n---\n**Your Score: ${newScore}/${total} (${pct}%)** ${grade}\n\nGreat work going through the quiz! Ask me anything — just type your question.`,
      done:    true,
    };
  }

  // Advance to next question
  const newState: ExamState = { questions, current: current + 1, score: newScore };
  await redis.setex(examStateKey(sessionId), EXAM_STATE_TTL, JSON.stringify(newState));

  return {
    message: `${feedback}\n\n${formatQuestion(questions[current + 1], current + 2, questions.length, '')}`,
    done:    false,
  };
}

// ─── formatQuestion ───────────────────────────────────────────────────────────

function formatQuestion(
  q:         ExamQuestion,
  num:       number,
  total:     number,
  firstName: string,
): string {
  const intro = firstName
    ? `Hey ${firstName}! 🎯 Quick knowledge check based on your recent studies.\n_(Type A, B, C, or D to answer — or "skip" to exit the quiz.)_\n\n`
    : '';
  return `${intro}**Question ${num} of ${total}**\n\n${q.q}\n\n${q.options.join('\n')}\n\n_Type A, B, C, or D_`;
}
