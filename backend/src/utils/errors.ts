// Typed error classes.
// Each maps to a specific HTTP status code.
// Fastify's error handler will catch these and format the response.

export class AppError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly statusCode: number = 500,
    public readonly details?: unknown,
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export class ValidationError extends AppError {
  constructor(message: string, details?: unknown) {
    super('VALIDATION_ERROR', message, 400, details);
    this.name = 'ValidationError';
  }
}

export class AuthError extends AppError {
  constructor(message = 'Authentication required') {
    super('AUTH_REQUIRED', message, 401);
    this.name = 'AuthError';
  }
}

export class ForbiddenError extends AppError {
  constructor(message = 'Access denied') {
    super('FORBIDDEN', message, 403);
    this.name = 'ForbiddenError';
  }
}

export class NotFoundError extends AppError {
  constructor(message = 'Resource not found') {
    super('NOT_FOUND', message, 404);
    this.name = 'NotFoundError';
  }
}

export class RateLimitError extends AppError {
  constructor(message = 'Too many requests. Please slow down.') {
    super('RATE_LIMIT', message, 429);
    this.name = 'RateLimitError';
  }
}

export class QuotaError extends AppError {
  constructor(message = 'Daily usage limit reached. Upgrade your plan for more.') {
    super('QUOTA_EXCEEDED', message, 429);
    this.name = 'QuotaError';
  }
}

export class AIError extends AppError {
  constructor(message = 'AI service temporarily unavailable') {
    super('AI_ERROR', message, 503);
    this.name = 'AIError';
  }
}

export class ConflictError extends AppError {
  constructor(message: string) {
    super('CONFLICT', message, 409);
    this.name = 'ConflictError';
  }
}
