import { config } from '../config/index.js';
import { logger } from '../utils/logger.js';

// Using Resend REST API directly — more reliable than SMTP in serverless/cloud environments.
// Docs: https://resend.com/docs/api-reference/emails/send-email

const RESEND_API_URL = 'https://api.resend.com/emails';

async function sendEmail(to: string, subject: string, html: string, text: string): Promise<void> {
  const response = await fetch(RESEND_API_URL, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${config.RESEND_API_KEY}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      from: `KanoonSaathi <${config.EMAIL_FROM}>`,
      to: [to],
      subject,
      html,
      text,
    }),
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    logger.error({ status: response.status, error, to }, 'Resend API error');
    throw new Error(`Email delivery failed: ${JSON.stringify(error)}`);
  }

  const result = await response.json() as { id?: string };
  logger.info({ to, emailId: result.id }, 'Email sent via Resend');
}

export const emailService = {
  async sendOtp(email: string, otp: string): Promise<void> {
    const html = `
      <!DOCTYPE html>
      <html>
      <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; max-width: 480px; margin: 0 auto; padding: 24px; background: #f5f5f5;">
        <div style="background: #fff; border-radius: 16px; padding: 32px; box-shadow: 0 2px 8px rgba(0,0,0,0.08);">
          <div style="text-align: center; margin-bottom: 28px;">
            <h1 style="color: #1a1a2e; font-size: 22px; margin: 0;">⚖️ KanoonSaathi</h1>
            <p style="color: #888; margin: 4px 0 0; font-size: 13px;">AI-powered Legal Education</p>
          </div>

          <p style="color: #444; font-size: 15px; margin: 0 0 20px;">Your login verification code:</p>

          <div style="background: #1a1a2e; border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 20px;">
            <span style="color: #fff; font-size: 40px; font-weight: 700; letter-spacing: 14px; font-family: 'Courier New', monospace;">${otp}</span>
          </div>

          <p style="color: #888; font-size: 13px; text-align: center; margin: 0;">
            Expires in <strong>5 minutes</strong> &nbsp;·&nbsp; Do not share this code
          </p>
        </div>

        <p style="color: #bbb; font-size: 11px; text-align: center; margin-top: 20px;">
          If you didn't request this, you can safely ignore this email.
        </p>
      </body>
      </html>
    `;

    const text = `Your KanoonSaathi login code is: ${otp}\n\nThis code expires in 5 minutes.\nNever share this code with anyone.`;

    await sendEmail(email, `${otp} — Your KanoonSaathi login code`, html, text);
  },

  async sendAccountDeletion(email: string, name: string): Promise<void> {
    const html = `
      <!DOCTYPE html>
      <html>
      <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; max-width: 480px; margin: 0 auto; padding: 24px; background: #f5f5f5;">
        <div style="background: #fff; border-radius: 16px; padding: 32px; box-shadow: 0 2px 8px rgba(0,0,0,0.08);">
          <div style="text-align: center; margin-bottom: 24px;">
            <h1 style="color: #1a1a2e; font-size: 22px; margin: 0;">⚖️ KanoonSaathi</h1>
            <p style="color: #888; margin: 4px 0 0; font-size: 13px;">AI-powered Legal Education</p>
          </div>

          <div style="background: #fef2f2; border-left: 4px solid #dc2626; border-radius: 4px; padding: 14px 16px; margin-bottom: 24px;">
            <p style="color: #991b1b; font-size: 14px; font-weight: 600; margin: 0 0 4px;">Account Deleted</p>
            <p style="color: #b91c1c; font-size: 13px; margin: 0;">Your account has been permanently deleted as requested.</p>
          </div>

          <p style="color: #374151; font-size: 15px;">Hi ${name},</p>

          <p style="color: #6b7280; font-size: 14px; line-height: 1.6;">
            As per your request, your KanoonSaathi account and all associated personal data have been <strong style="color: #374151;">permanently and irreversibly deleted</strong> from our systems. This includes:
          </p>

          <ul style="color: #6b7280; font-size: 14px; line-height: 1.8; padding-left: 20px;">
            <li>Your profile and account information</li>
            <li>All chat sessions and message history</li>
            <li>Bookmarked sessions and saved items</li>
            <li>Usage history and analytics data</li>
            <li>Device session records</li>
          </ul>

          <p style="color: #6b7280; font-size: 14px; line-height: 1.6;">
            This action was completed in accordance with your rights under the
            <strong style="color: #374151;">Digital Personal Data Protection Act, 2023 (India)</strong> — specifically your Right to Erasure under Section 12.
          </p>

          <div style="background: #f9fafb; border-radius: 10px; padding: 16px; margin-top: 20px;">
            <p style="color: #374151; font-size: 13px; margin: 0 0 8px;"><strong>What happens next?</strong></p>
            <p style="color: #6b7280; font-size: 13px; margin: 0; line-height: 1.6;">
              Your email address will be retained for fraud prevention purposes for up to 1 year as described in our Privacy Policy, after which it will also be purged.
              You can re-register on KanoonSaathi at any time using the same or a different email address.
            </p>
          </div>

          <p style="color: #9ca3af; font-size: 13px; margin-top: 24px; text-align: center;">
            If this deletion was not requested by you, please contact us immediately at
            <a href="mailto:privacy@kanoonsaathi.in" style="color: #1a1a2e;">privacy@kanoonsaathi.in</a>
          </p>
        </div>

        <p style="color: #bbb; font-size: 11px; text-align: center; margin-top: 20px;">
          KanoonSaathi Technologies Pvt. Ltd., India &nbsp;·&nbsp; DPDP Act 2023 Compliant
        </p>
      </body>
      </html>
    `;

    const text = [
      `Hi ${name},`,
      '',
      'Your KanoonSaathi account has been permanently deleted as requested.',
      '',
      'All data deleted: profile, chat history, bookmarks, usage logs, and device sessions.',
      '',
      'This was done under your Right to Erasure (Section 12, DPDP Act 2023).',
      '',
      'If you did not request this, contact privacy@kanoonsaathi.in immediately.',
      '',
      'KanoonSaathi Technologies Pvt. Ltd.',
    ].join('\n');

    try {
      await sendEmail(email, 'Your KanoonSaathi account has been deleted — Action Confirmed', html, text);
    } catch (err) {
      logger.warn({ err, email }, 'Account deletion email failed — non-critical, data already deleted');
    }
  },

  async sendWelcome(email: string, name: string, role: string): Promise<void> {
    const roleMessages: Record<string, string> = {
      STUDENT: 'Start exploring legal concepts, case summaries, and exam prep tools built for LLB students.',
      FACULTY: 'Plan lectures, generate quizzes, and make legal education more effective with AI.',
      CURIOUS: 'Learn about your rights and understand everyday Indian law in simple language.',
    };

    const html = `
      <!DOCTYPE html>
      <html>
      <body style="font-family: -apple-system, sans-serif; max-width: 480px; margin: 0 auto; padding: 24px;">
        <div style="background: #fff; border-radius: 16px; padding: 32px; box-shadow: 0 2px 8px rgba(0,0,0,0.08);">
          <h1 style="color: #1a1a2e; font-size: 22px;">Welcome to KanoonSaathi, ${name}! 🎉</h1>
          <p style="color: #555;">${roleMessages[role] ?? 'Welcome aboard!'}</p>
          <div style="background: #fff8e1; border-left: 4px solid #f59e0b; padding: 12px 16px; border-radius: 4px; margin-top: 20px;">
            <p style="color: #92400e; font-size: 13px; margin: 0;">
              <strong>Remember:</strong> KanoonSaathi provides legal education only. For legal advice on your specific situation, always consult a qualified advocate.
            </p>
          </div>
        </div>
      </body>
      </html>
    `;

    try {
      await sendEmail(email, 'Welcome to KanoonSaathi ⚖️', html, html);
    } catch (err) {
      logger.warn({ err, email }, 'Welcome email failed — non-critical, continuing');
    }
  },
};
