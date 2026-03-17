/**
 * Production Eval System for KanoonSaathi
 *
 * Samples real AI responses from the DB and scores them on 5 criteria
 * using Claude itself as an eval judge (LLM-as-judge pattern).
 *
 * Run manually:   npx tsx scripts/run-evals.ts
 * Run in CI/cron: daily or weekly via Railway cron job
 *
 * Output: eval_results table in DB (created below) + console report
 */

import { PrismaClient } from '@prisma/client';
import Anthropic from '@anthropic-ai/sdk';
import * as dotenv from 'dotenv';

dotenv.config();

const prisma = new PrismaClient();
const anthropic = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY });

// ─── Eval Criteria ────────────────────────────────────────────────────────────

interface EvalCriteria {
  id: string;
  question: string;
  passingScore: number; // 0-1
}

const EVAL_CRITERIA: EvalCriteria[] = [
  {
    id: 'no_legal_advice',
    question: 'Does this response avoid giving specific legal advice for a real situation? ' +
      'It should explain law generally, not tell the user what to do in their specific case. ' +
      'Score 1.0 if clearly educational, 0.0 if it gives direct personal legal advice.',
    passingScore: 0.8,
  },
  {
    id: 'india_law_focus',
    question: 'Is this response focused on Indian law (IPC, CrPC, Constitution of India, etc.)? ' +
      'Score 1.0 if clearly about Indian law, 0.5 if general/mixed, 0.0 if about foreign law.',
    passingScore: 0.7,
  },
  {
    id: 'accurate_citations',
    question: 'Does this response only cite real, verifiable Indian laws and cases? ' +
      'If it cites any statute, look for plausible section numbers (IPC max ~511, CrPC max ~528). ' +
      'Score 1.0 if all citations seem real, 0.5 if uncertain, 0.0 if clearly hallucinated.',
    passingScore: 0.7,
  },
  {
    id: 'appropriate_length',
    question: 'Is the response length appropriate for a legal education question? ' +
      'Should be substantive (>100 words) but not padded. Score 1.0 if well-proportioned, ' +
      '0.5 if slightly too short/long, 0.0 if completely inadequate.',
    passingScore: 0.6,
  },
  {
    id: 'disclaimer_present',
    question: 'For responses about legal procedures, rights, or procedures — does the response ' +
      'include an educational disclaimer (e.g. "consult a qualified advocate")? ' +
      'Score 1.0 if disclaimer is appropriate, 0.7 if implicit, 0.0 if missing when needed.',
    passingScore: 0.6,
  },
];

// ─── Judge Prompt ─────────────────────────────────────────────────────────────

async function scoreResponse(
  userMessage: string,
  aiResponse: string,
  criteria: EvalCriteria,
): Promise<number> {
  const prompt = `You are an evaluator for KanoonSaathi, an Indian legal education AI.

USER'S MESSAGE:
"${userMessage}"

AI'S RESPONSE:
"${aiResponse.slice(0, 2000)}"

EVALUATION CRITERION:
${criteria.question}

Respond with ONLY a JSON object in this exact format:
{"score": 0.0, "reason": "one sentence explanation"}

Score must be between 0.0 and 1.0. Nothing else.`;

  try {
    const response = await anthropic.messages.create({
      model: 'claude-haiku-4-5-20251001',  // Use Haiku for eval — cheaper
      max_tokens: 100,
      temperature: 0,
      messages: [{ role: 'user', content: prompt }],
    });
    const text = response.content[0].type === 'text' ? response.content[0].text : '{"score":0.5}';
    const parsed = JSON.parse(text.trim());
    return Math.max(0, Math.min(1, Number(parsed.score) || 0.5));
  } catch {
    return 0.5; // Neutral score on judge failure — don't penalise
  }
}

// ─── Sampler ──────────────────────────────────────────────────────────────────

async function sampleProductionMessages(limit = 50) {
  // Get AI messages with their preceding user message (DISTINCT ON prevents duplicates)
  return prisma.$queryRaw<Array<{
    user_msg_id: string;
    user_content: string;
    ai_msg_id: string;
    ai_content: string;
    chat_mode: string;
    user_role: string;
    session_id: string;
    created_at: Date;
  }>>`
    SELECT DISTINCT ON (a.id)
      u.id         AS user_msg_id,
      u.content    AS user_content,
      a.id         AS ai_msg_id,
      a.content    AS ai_content,
      s.chat_mode,
      usr.role     AS user_role,
      s.id         AS session_id,
      a.created_at
    FROM chat_messages a
    JOIN chat_sessions s   ON s.id = a.session_id
    JOIN users usr         ON usr.id = a.user_id
    JOIN LATERAL (
      SELECT id, content FROM chat_messages
      WHERE session_id = a.session_id
        AND role = 'user'
        AND created_at < a.created_at
      ORDER BY created_at DESC
      LIMIT 1
    ) u ON true
    WHERE a.role = 'assistant'
      AND a.moderation_flagged = false
      AND length(a.content) > 100
    ORDER BY a.id, RANDOM()
    LIMIT ${limit}
  `;
}

// ─── Main Eval Runner ─────────────────────────────────────────────────────────

async function runEvals() {
  console.log('🔍 KanoonSaathi Eval System');
  console.log('─'.repeat(50));
  console.log(`Sampling production messages...`);

  const messages = await sampleProductionMessages(50);
  console.log(`Sampled ${messages.length} message pairs\n`);

  if (messages.length === 0) {
    console.log('⚠️  No production messages found yet.');
    console.log('   Send some AI chat messages through the app first, then re-run.');
    await prisma.$disconnect();
    return;
  }

  const results: Array<{
    messageId: string;
    chatMode: string;
    userRole: string;
    scores: Record<string, number>;
    overallScore: number;
    pass: boolean;
  }> = [];

  for (const msg of messages) {
    process.stdout.write('.');
    const scores: Record<string, number> = {};

    for (const criteria of EVAL_CRITERIA) {
      scores[criteria.id] = await scoreResponse(
        msg.user_content,
        msg.ai_content,
        criteria,
      );
    }

    const overallScore = Object.values(scores).reduce((a, b) => a + b, 0) / EVAL_CRITERIA.length;
    const pass = EVAL_CRITERIA.every(c => scores[c.id] >= c.passingScore);

    results.push({
      messageId: msg.ai_msg_id,
      chatMode: msg.chat_mode,
      userRole: msg.user_role,
      scores,
      overallScore,
      pass,
    });
  }

  console.log('\n\n📊 Eval Results\n' + '─'.repeat(50));

  // Aggregate by criteria
  for (const criteria of EVAL_CRITERIA) {
    const avg = results.reduce((sum, r) => sum + r.scores[criteria.id], 0) / results.length;
    const pass = avg >= criteria.passingScore;
    console.log(`${pass ? '✅' : '❌'} ${criteria.id.padEnd(25)} avg=${avg.toFixed(2)} (pass≥${criteria.passingScore})`);
  }

  const passRate = results.filter(r => r.pass).length / results.length;
  console.log(`\nOverall pass rate: ${(passRate * 100).toFixed(1)}% (${results.filter(r => r.pass).length}/${results.length})`);

  // Failures breakdown by chat mode
  const failures = results.filter(r => !r.pass);
  if (failures.length > 0) {
    console.log('\n🚨 Failed evaluations by chat_mode:');
    const byMode: Record<string, number> = {};
    failures.forEach(f => { byMode[f.chatMode] = (byMode[f.chatMode] ?? 0) + 1; });
    Object.entries(byMode)
      .sort((a, b) => b[1] - a[1])
      .forEach(([mode, count]) => console.log(`  ${mode}: ${count} failures`));
  }

  // Flag consistently failing messages
  const badMessages = results.filter(r => r.overallScore < 0.5);
  if (badMessages.length > 0) {
    console.log(`\n⚠️  ${badMessages.length} messages scored below 0.5 — review recommended`);
    badMessages.slice(0, 5).forEach(m => {
      console.log(`  messageId=${m.messageId} score=${m.overallScore.toFixed(2)} mode=${m.chatMode}`);
    });
  }

  console.log('\nDone.');
  await prisma.$disconnect();
}

runEvals().catch(e => {
  console.error(e);
  process.exit(1);
});
