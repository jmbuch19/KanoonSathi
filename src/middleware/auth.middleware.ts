import { FastifyRequest, FastifyReply } from 'fastify';
import { AuthError, ForbiddenError } from '../utils/errors.js';
import { UserRole } from '@prisma/client';

// Attached to every protected route.
// The JWT plugin (registered in server.ts) already verifies the signature and expiry.
// This middleware extracts the payload and attaches it to req.user.

export async function requireAuth(req: FastifyRequest, _reply: FastifyReply): Promise<void> {
  try {
    await req.jwtVerify();
  } catch {
    throw new AuthError('Invalid or expired token. Please log in again.');
  }
}

// Role guard — call after requireAuth
export function requireRole(...roles: UserRole[]) {
  return async (req: FastifyRequest, _reply: FastifyReply): Promise<void> => {
    if (!req.user) throw new AuthError();
    if (!roles.includes(req.user.role)) {
      throw new ForbiddenError(`This feature is only available for: ${roles.join(', ')}`);
    }
  };
}

// Admin-only guard (uses separate admin JWT)
export async function requireAdmin(req: FastifyRequest, _reply: FastifyReply): Promise<void> {
  try {
    // Admin tokens are verified with the admin secret
    // We use a custom header to differentiate: X-Admin-Token
    const adminToken = req.headers['x-admin-token'];
    if (!adminToken || typeof adminToken !== 'string') throw new Error();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const adminJwt = (req.server as any).adminJwt;
    const payload = adminJwt.verify(adminToken);
    if (!payload || typeof payload !== 'object' || !('adminRole' in payload)) {
      throw new Error();
    }
    req.adminUser = payload as { sub: string; adminRole: string };
  } catch {
    throw new AuthError('Admin authentication required.');
  }
}
