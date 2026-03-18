import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import { requireAuth } from '../../middleware/auth.middleware.js';
import { getNewsFeed } from '../../services/rss.service.js';

export async function newsRoutes(fastify: FastifyInstance): Promise<void> {
  // Auth required — ensures only registered users can hit the RSS proxy.
  // This also lets us add per-user filtering later.
  fastify.addHook('preHandler', requireAuth);

  /**
   * GET /api/v1/news/feed
   * Returns merged Indian legal news sorted newest first.
   * Cached in Redis for 30 minutes.
   *
   * Query params:
   *   category — optional filter, e.g. "Supreme Court"
   *   limit    — max items to return (default 20, max 50)
   */
  fastify.get('/feed', async (req: FastifyRequest, reply: FastifyReply) => {
    const query = req.query as { category?: string; limit?: string };
    const limit = Math.min(50, Math.max(1, parseInt(query.limit ?? '20')));

    let items = await getNewsFeed();

    if (query.category) {
      items = items.filter(
        i => i.category.toLowerCase() === query.category!.toLowerCase(),
      );
    }

    return reply.send({
      success: true,
      data: {
        items: items.slice(0, limit),
        total: items.length,
        cachedAt: new Date().toISOString(),
      },
    });
  });
}
