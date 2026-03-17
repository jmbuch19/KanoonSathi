/**
 * Database Seed Script
 * Run with: npm run db:seed
 *
 * Creates:
 * - Default admin user
 * - Default prompt templates for all roles
 */

import { PrismaClient } from '@prisma/client';
import bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

async function main() {
  console.log('🌱 Seeding database...');

  // ── Admin User ─────────────────────────────────────────────────────────────
  const adminEmail = 'admin@kanoonsaathi.in';
  const adminPassword = process.env.ADMIN_SEED_PASSWORD ?? 'changeme_immediately_123!';

  const existing = await prisma.adminUser.findUnique({ where: { email: adminEmail } });
  if (!existing) {
    await prisma.adminUser.create({
      data: {
        email: adminEmail,
        passwordHash: await bcrypt.hash(adminPassword, 12),
        name: 'Super Admin',
        adminRole: 'super_admin',
        isActive: true,
      },
    });
    console.log(`✅ Admin created: ${adminEmail}`);
    console.log(`⚠️  Default password set. CHANGE IT IMMEDIATELY via admin panel.`);
  } else {
    console.log(`ℹ️  Admin already exists: ${adminEmail}`);
  }

  // ── Suggested Prompt Templates ─────────────────────────────────────────────
  // These are surfaced on the dashboard as quick-start options.
  // They are NOT system prompts — those are in ai.service.ts

  const suggestedPrompts = [
    // Student prompts
    { role: 'STUDENT', chatMode: 'concept_explainer', name: 'Basic Structure Doctrine', content: 'Explain the doctrine of basic structure of the Indian Constitution' },
    { role: 'STUDENT', chatMode: 'concept_explainer', name: 'Mens Rea', content: 'Explain the concept of mens rea with examples from IPC' },
    { role: 'STUDENT', chatMode: 'case_summarizer', name: 'Kesavananda Bharati', content: 'Summarize Kesavananda Bharati v State of Kerala (1973)' },
    { role: 'STUDENT', chatMode: 'exam_prep', name: 'CLAT PG MCQs', content: 'Generate 10 MCQs on Constitutional Law for CLAT PG preparation' },
    // Faculty prompts
    { role: 'FACULTY', chatMode: 'lecture_planner', name: 'Article 21 Lecture', content: 'Plan a 90-minute lecture on Article 21 of the Indian Constitution' },
    { role: 'FACULTY', chatMode: 'quiz_generator', name: 'Law of Torts Quiz', content: 'Generate 20 MCQs on Law of Torts with answer key and explanations' },
    // Curious prompts
    { role: 'CURIOUS', chatMode: 'rights_explainer', name: 'Police Rights', content: 'What are my rights if police stop me on the street in India?' },
    { role: 'CURIOUS', chatMode: 'everyday_law', name: 'Consumer Rights', content: 'What are my rights as a consumer if I receive a defective product?' },
    { role: 'CURIOUS', chatMode: 'legal_terms', name: 'What is FIR', content: 'What does FIR mean and when is it filed?' },
  ];

  for (const prompt of suggestedPrompts) {
    await prisma.promptTemplate.upsert({
      where: {
        // Use name as a natural key for upsert
        id: `seed_${prompt.role}_${prompt.chatMode}_${prompt.name.replace(/\s/g, '_')}`.slice(0, 36),
      },
      update: {},
      create: {
        id: `seed_${prompt.role}_${prompt.chatMode}_${prompt.name.replace(/\s/g, '_')}`.slice(0, 36),
        ...prompt,
        templateType: 'suggested',
        isActive: true,
      },
    });
  }

  console.log(`✅ ${suggestedPrompts.length} prompt templates seeded`);
  console.log('🎉 Seeding complete!');
}

main()
  .catch((e) => {
    console.error('❌ Seed failed:', e);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());
