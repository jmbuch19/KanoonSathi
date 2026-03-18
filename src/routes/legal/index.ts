import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';

// ─── DPDP Act 2023 – Compliant Privacy Policy ─────────────────────────────────
// Digital Personal Data Protection Act, 2023 (India) — Structured JSON policy
// served at GET /api/v1/legal/privacy-policy
// The same content is embedded in the Android app for offline access.

const PRIVACY_POLICY = {
  title:         'Privacy Policy',
  platform:      'KanoonSaathi',
  dataFiduciary: 'KanoonSaathi Technologies Pvt. Ltd.',
  lastUpdated:   '16 March 2026',
  effectiveDate: '16 March 2026',
  version:       '1.0',
  governingLaw:  'Digital Personal Data Protection Act, 2023 (India)',
  sections: [
    {
      id: '1',
      title: 'Identity of the Data Fiduciary',
      content: [
        'KanoonSaathi ("we", "our", "the Platform") is an AI-powered legal education platform developed and operated by KanoonSaathi Technologies Pvt. Ltd., a company incorporated under the Companies Act, 2013, with its principal office in India.',
        'We are the Data Fiduciary as defined under Section 2(i) of the Digital Personal Data Protection Act, 2023 ("DPDP Act") in respect of all personal data processed through this Platform.',
        'This Privacy Policy describes how we collect, use, store, share, and protect your personal data, and explains your rights as a Data Principal under the DPDP Act, 2023.',
      ],
    },
    {
      id: '2',
      title: 'Scope of this Policy',
      content: [
        'This Policy applies to all users of the KanoonSaathi mobile application and any associated web interfaces, including Students, Faculty members, and Curious Learners.',
        'This Policy does not apply to third-party websites, platforms, or services that may be linked from our Platform. We encourage you to read the privacy policies of any third-party services you access.',
        'By registering on and using KanoonSaathi, you confirm that you have read, understood, and consent to the processing of your personal data as described in this Policy.',
      ],
    },
    {
      id: '3',
      title: 'Personal Data We Collect',
      content: [
        'We collect personal data that you voluntarily provide and data generated automatically during your use of the Platform. The following categories of personal data are processed:',
      ],
      subsections: [
        {
          title: 'A. Identity & Contact Data',
          points: [
            'Full name (provided during onboarding)',
            'Email address (used for authentication via OTP)',
            'Role / account type (Student, Faculty, Curious Learner)',
          ],
        },
        {
          title: 'B. Academic & Professional Profile Data',
          points: [
            'Students: institution/college name, year of study, semester, subjects of interest, exam target, city, state',
            'Faculty: institution name, designation, subjects currently taught, Bar Council ID (optional)',
            'Curious Learners: display name, areas of legal interest',
          ],
        },
        {
          title: 'C. Usage & Interaction Data',
          points: [
            'Chat messages and AI responses (full conversation history)',
            'Chat session metadata (mode, session creation time, message count, token count)',
            'Bookmarked sessions and saved items',
            'Daily message and token usage statistics',
          ],
        },
        {
          title: 'D. Device & Technical Data',
          points: [
            'Device identifier and device model (collected at login for session management)',
            'App version at time of login',
            'IP address of login requests',
          ],
        },
        {
          title: 'E. Analytics & Behavioural Data',
          points: [
            'Event logs (e.g., session creation, mode selection — no personally sensitive content)',
            'Error and crash reports (may include partial stack traces and device state via Sentry)',
          ],
        },
      ],
    },
    {
      id: '4',
      title: 'Purpose of Processing and Legal Basis',
      content: [
        'Under Section 4 and Section 6 of the DPDP Act, 2023, we process your personal data only for the specific purposes for which your free, specific, informed, and unconditional consent was obtained.',
        'The purposes for which your personal data is processed are:',
      ],
      subsections: [
        {
          title: null,
          points: [
            'Account creation, authentication, and identity verification',
            'Providing personalised AI-assisted legal education experiences',
            'Generating context-aware, role-specific AI responses tailored to your academic/professional profile',
            'Displaying continuity greetings and study recaps across sessions',
            'Delivering geolocation-based internship opportunity notifications (Students only)',
            'Tracking daily usage to enforce fair-use quotas',
            'Monitoring for content safety and moderation',
            'Improving platform quality through aggregated, anonymised analytics',
            'Sending transactional emails (OTP codes, welcome messages)',
            'Fulfilling legal and regulatory obligations under applicable Indian law',
          ],
        },
      ],
    },
    {
      id: '5',
      title: 'Consent Management',
      content: [
        'Your consent is obtained at the time of registration and onboarding via explicit acceptance of this Privacy Policy and our Terms of Service.',
        'You have the right to withdraw your consent at any time. However, withdrawal of consent may result in degraded functionality or inability to use the Platform, as certain processing is necessary for core service delivery.',
        'To withdraw consent or request erasure of your personal data, please contact our Grievance Officer as detailed in Section 11 below.',
        'We do not use consent obtained through bundling, coercion, or deceptive design. Consent for optional data points (such as city/state for internship matching) is sought separately and denial will not affect access to core platform features.',
      ],
    },
    {
      id: '6',
      title: 'Retention of Personal Data',
      content: [
        'In accordance with Section 8(7) of the DPDP Act, 2023, we retain personal data only for as long as necessary to fulfil the purposes for which it was collected, or as required by applicable law.',
        'Our specific retention periods are:',
      ],
      subsections: [
        {
          title: null,
          points: [
            'Account and profile data: retained for the duration of your account and for 1 year after account deletion (for audit and fraud prevention purposes)',
            'Chat messages and session data: retained for 2 years from the date of creation; you may delete individual sessions at any time via the app',
            'OTP records: deleted within 24 hours of generation or first use',
            'Session tokens: automatically expired per your device session',
            'Usage logs and analytics: retained in aggregated, anonymised form for up to 3 years',
            'Device session data: retained for 90 days after last activity',
            'Error and crash reports: retained for 90 days via Sentry',
          ],
        },
      ],
    },
    {
      id: '7',
      title: 'Sharing of Personal Data with Third Parties',
      content: [
        'We share your personal data only with Data Processors who process data strictly on our behalf and under our instructions, pursuant to contractual obligations consistent with the DPDP Act, 2023.',
        'We do not sell, rent, or trade your personal data to any third party for their own marketing purposes.',
        'The Data Processors we engage are:',
      ],
      subsections: [
        {
          title: null,
          points: [
            'Anthropic (Claude AI, USA): processes chat message content to generate AI responses. Data is subject to Anthropic\'s data processing agreement and not stored for model training without opt-in.',
            'OpenAI (optional AI fallback, USA): same purpose as Anthropic. Used only if primary AI is unavailable.',
            'Resend (email service, USA): processes your email address to deliver OTP codes and system notifications.',
            'Upstash (Redis cloud, USA/EU): stores temporary session context data (chat history buffers) for real-time AI continuity. Data expires automatically.',
            'PostgreSQL hosting provider (Railway/Neon/Render, USA): stores all persistent platform data in encrypted databases.',
            'Sentry (USA): receives error and crash reports. Sentry\'s data retention and processing is governed by their Data Processing Agreement.',
          ],
        },
      ],
    },
    {
      id: '8',
      title: 'Cross-Border Transfer of Personal Data',
      content: [
        'Some of our Data Processors, including Anthropic, OpenAI, Resend, Upstash, and Sentry, are located outside India and process your personal data in jurisdictions that may not have data protection laws equivalent to Indian law.',
        'We ensure that such transfers are governed by appropriate contractual safeguards, data processing agreements, and, where applicable, standard contractual clauses to protect your personal data in transit and at rest.',
        'We will comply with any rules or restrictions on cross-border data transfers notified by the Central Government under Section 16 of the DPDP Act, 2023 upon their notification.',
      ],
    },
    {
      id: '9',
      title: 'Security Safeguards',
      content: [
        'We implement appropriate technical and organisational measures to protect your personal data against unauthorised access, alteration, disclosure, or destruction, in compliance with Section 8(5) of the DPDP Act, 2023.',
        'Our security measures include:',
      ],
      subsections: [
        {
          title: null,
          points: [
            'All data in transit is encrypted using TLS 1.2 or higher',
            'Authentication tokens are short-lived JWTs with refresh token rotation',
            'OTPs are hashed using bcrypt before storage and expire within 5 minutes',
            'Database access is restricted by environment-specific firewall rules',
            'All passwords and sensitive tokens are stored only in hashed form',
            'Regular dependency audits and security patching',
            'Automated monitoring and alerting for anomalous system behaviour',
          ],
        },
      ],
    },
    {
      id: '10',
      title: 'Data Breach Notification',
      content: [
        'In the event of a personal data breach that is likely to result in risk to your rights and freedoms, we will notify the Data Protection Board of India as required under Section 8(6) of the DPDP Act, 2023, and will take prompt action to mitigate harm.',
        'Affected Data Principals will be informed via their registered email address within a reasonable timeframe, with details of the nature of the breach and recommended protective actions.',
      ],
    },
    {
      id: '11',
      title: 'Your Rights as a Data Principal (DPDP Act, 2023)',
      content: [
        'The DPDP Act, 2023 grants you the following rights as a Data Principal. You may exercise any of these rights by contacting our Grievance Officer (details in Section 12):',
      ],
      subsections: [
        {
          title: 'Right to Access Information (Section 11)',
          points: [
            'You have the right to obtain a summary of the personal data we hold about you and the processing activities carried out on such data.',
            'You may also request the names of Data Processors and Data Fiduciaries with whom your personal data has been shared.',
          ],
        },
        {
          title: 'Right to Correction and Erasure (Section 12)',
          points: [
            'You have the right to have inaccurate, incomplete, or outdated personal data corrected.',
            'You have the right to request erasure of personal data that is no longer necessary for the purpose for which it was collected, or where you have withdrawn consent.',
            'Erasure requests will be actioned within 30 days of verification, except where retention is required by law.',
          ],
        },
        {
          title: 'Right to Grievance Redressal (Section 13)',
          points: [
            'If you believe your rights have been infringed or your personal data has been processed in a manner inconsistent with this Policy or the DPDP Act, you have the right to grievance redressal.',
            'You may file a complaint with the Data Protection Board of India if your grievance is not resolved to your satisfaction by our Grievance Officer.',
          ],
        },
        {
          title: 'Right to Nominate (Section 14)',
          points: [
            'You have the right to nominate any individual who shall, in the event of your death or incapacity, exercise your rights as Data Principal on your behalf.',
            'To register a nomination, please contact our Grievance Officer.',
          ],
        },
      ],
    },
    {
      id: '12',
      title: 'Grievance Officer & Contact Details',
      content: [
        'In accordance with Section 13 of the DPDP Act, 2023, and the Information Technology (Intermediary Guidelines and Digital Media Ethics Code) Rules, 2021, we have appointed a Grievance Officer.',
        'All data-related requests, correction/erasure requests, consent withdrawals, and grievances should be addressed to:',
      ],
      contact: {
        name:              'Grievance Officer — KanoonSaathi',
        email:             'privacy@kanoonsaathi.in',
        address:           'KanoonSaathi Technologies Pvt. Ltd., India',
        responseTimeframe: 'We will acknowledge your request within 48 hours and aim to resolve it within 30 days.',
      },
    },
    {
      id: '13',
      title: 'Children\'s Data',
      content: [
        'KanoonSaathi is intended for use by persons aged 18 years and above. LLB degree programmes in India are pursued by adults, and our platform is designed for that audience.',
        'We do not knowingly collect or process the personal data of individuals under the age of 18. If we become aware that personal data of a person under 18 has been submitted without verifiable parental consent, we will delete such data promptly.',
        'In accordance with Section 9 of the DPDP Act, 2023, parental consent is required before processing the personal data of a child. We take reasonable steps to verify age at registration.',
      ],
    },
    {
      id: '14',
      title: 'Automated Decision-Making',
      content: [
        'Our Platform uses AI (powered by Anthropic Claude and optionally OpenAI) to process your chat messages and profile data to generate personalised educational responses.',
        'The AI generation process involves automated processing of your inputs. However, all substantive educational decisions (e.g., what content to produce) are governed by prompt engineering constraints and are reviewed at the model level.',
        'No legally significant or similarly consequential decisions about you are made solely by automated means. The AI is a learning aid, not a decision-maker about your rights or status.',
      ],
    },
    {
      id: '15',
      title: 'Changes to this Privacy Policy',
      content: [
        'We may update this Privacy Policy from time to time to reflect changes in law, our data practices, or the services we offer.',
        'When we make material changes, we will notify you via your registered email address and/or a prominent in-app notice at least 15 days before the changes take effect.',
        'Your continued use of the Platform after the effective date of a revised Policy constitutes your acknowledgement of the changes. If you do not agree to the updated Policy, you may exercise your right to erasure and close your account.',
        'The version history and date of last update are displayed at the top of this Policy.',
      ],
    },
    {
      id: '16',
      title: 'Governing Law and Jurisdiction',
      content: [
        'This Privacy Policy is governed by and construed in accordance with the laws of India, including the Digital Personal Data Protection Act, 2023, the Information Technology Act, 2000, and rules made thereunder.',
        'Any disputes arising under or in connection with this Policy shall be subject to the exclusive jurisdiction of the competent courts in India.',
      ],
    },
  ],
};

// ─── Routes ───────────────────────────────────────────────────────────────────

export async function legalRoutes(fastify: FastifyInstance): Promise<void> {

  // GET /api/v1/legal/privacy-policy  — public, no auth
  fastify.get('/privacy-policy', async (_req: FastifyRequest, reply: FastifyReply) => {
    return reply.send({ success: true, data: PRIVACY_POLICY });
  });

  // GET /api/v1/legal/privacy-policy/text  — plain text version for display
  fastify.get('/privacy-policy/text', async (_req: FastifyRequest, reply: FastifyReply) => {
    const lines: string[] = [
      `${PRIVACY_POLICY.title} — ${PRIVACY_POLICY.platform}`,
      `Last Updated: ${PRIVACY_POLICY.lastUpdated}  |  Effective: ${PRIVACY_POLICY.effectiveDate}`,
      `Governing Law: ${PRIVACY_POLICY.governingLaw}`,
      '',
    ];

    for (const section of PRIVACY_POLICY.sections) {
      lines.push(`${section.id}. ${section.title}`);
      lines.push('─'.repeat(60));
      for (const para of section.content) {
        lines.push(para);
        lines.push('');
      }
      if ('subsections' in section && section.subsections) {
        for (const sub of section.subsections as { title: string | null; points: string[] }[]) {
          if (sub.title) {
            lines.push(`  ${sub.title}`);
          }
          for (const point of sub.points) {
            lines.push(`  • ${point}`);
          }
          lines.push('');
        }
      }
      if ('contact' in section && section.contact) {
        const c = section.contact as Record<string, string>;
        lines.push(`  Name:     ${c.name}`);
        lines.push(`  Email:    ${c.email}`);
        lines.push(`  Address:  ${c.address}`);
        lines.push(`  Timeline: ${c.responseTimeframe}`);
        lines.push('');
      }
      lines.push('');
    }

    return reply
      .header('Content-Type', 'text/plain; charset=utf-8')
      .send(lines.join('\n'));
  });
}
