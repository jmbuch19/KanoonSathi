/**
 * RSS Feed Service — fetches Indian legal news from public RSS feeds.
 * Results are cached in Redis for 30 minutes to avoid hammering sources.
 *
 * Sources:
 *  - Live Law     — largest Indian legal news portal
 *  - Bar & Bench  — SC/HC analysis and news
 *  - The Leaflet  — constitutional law commentary
 */

import Parser from 'rss-parser';
import { redis } from '../db/redis.js';
import { logger } from '../utils/logger.js';

const CACHE_KEY = 'news:feed:v1';
const CACHE_TTL = 30 * 60; // 30 minutes
const MAX_ITEMS_PER_SOURCE = 8;
const FETCH_TIMEOUT_MS = 8_000;

export interface NewsItem {
  id: string;        // sha-ish: source + pubDate + title slice
  title: string;
  summary: string;
  url: string;
  source: string;    // "Live Law" | "Bar & Bench" | "The Leaflet"
  category: string;  // "Supreme Court" | "High Court" | "General"
  publishedAt: string; // ISO 8601
}

interface FeedSource {
  name: string;
  url: string;
  defaultCategory: string;
}

const FEEDS: FeedSource[] = [
  {
    name: 'Live Law',
    url: 'https://www.livelaw.in/rss',
    defaultCategory: 'General',
  },
  {
    name: 'Bar & Bench',
    url: 'https://www.barandbench.com/feed',
    defaultCategory: 'General',
  },
  {
    name: 'The Leaflet',
    url: 'https://theleaflet.in/feed',
    defaultCategory: 'General',
  },
];

// Keyword→category mapping
const CATEGORY_RULES: Array<{ pattern: RegExp; category: string }> = [
  { pattern: /supreme court|sc |apex court/i, category: 'Supreme Court' },
  { pattern: /high court|hc |bombay|delhi hc|madras|calcutta|allahabad/i, category: 'High Court' },
  { pattern: /clat|aibe|judicial service|bar exam|law exam/i, category: 'Exam' },
  { pattern: /constitution|fundamental right|article \d+/i, category: 'Constitutional' },
  { pattern: /criminal|ipc|crpc|bail|acquit|convict/i, category: 'Criminal' },
  { pattern: /corporate|companies act|sebi|rbi|insolvency|ibc/i, category: 'Corporate' },
  { pattern: /family|divorce|matrimonial|custody|maintenance/i, category: 'Family' },
  { pattern: /labour|employment|worker|wage/i, category: 'Labour' },
];

function categorise(title: string, description: string): string {
  const text = `${title} ${description}`;
  for (const rule of CATEGORY_RULES) {
    if (rule.pattern.test(text)) return rule.category;
  }
  return 'General';
}

function makeId(source: string, pubDate: string, title: string): string {
  const raw = `${source}:${pubDate}:${title.slice(0, 40)}`;
  let hash = 0;
  for (let i = 0; i < raw.length; i++) {
    hash = (Math.imul(31, hash) + raw.charCodeAt(i)) | 0;
  }
  return Math.abs(hash).toString(36);
}

function stripHtml(html: string): string {
  return html
    .replace(/<[^>]+>/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#039;/g, "'")
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 280);
}

async function fetchFeed(source: FeedSource, parser: Parser): Promise<NewsItem[]> {
  try {
    const feed = await parser.parseURL(source.url);

    const items: NewsItem[] = [];

    for (const item of (feed.items ?? []).slice(0, MAX_ITEMS_PER_SOURCE)) {
      const title      = (item.title ?? '').trim();
      const link       = item.link ?? '';
      const pubDate    = item.pubDate
        ? new Date(item.pubDate).toISOString()
        : new Date().toISOString();
      const rawSummary = item.contentSnippet ?? item.content ?? item.summary ?? '';
      const summary    = stripHtml(rawSummary) || title;

      if (!title || !link) continue;

      items.push({
        id:          makeId(source.name, pubDate, title),
        title,
        summary,
        url:         link,
        source:      source.name,
        category:    categorise(title, summary),
        publishedAt: pubDate,
      });
    }

    return items;
  } catch (err) {
    logger.warn({ err, feed: source.name }, `[RSS] Failed to fetch ${source.name}`);
    return [];
  }
}

/**
 * Get merged + cached news feed.
 * Returns up to ~24 items (8 per source × 3 sources), sorted newest first.
 */
export async function getNewsFeed(): Promise<NewsItem[]> {
  // Try cache first
  try {
    const cached = await redis.get<NewsItem[]>(CACHE_KEY);
    if (cached && Array.isArray(cached) && cached.length > 0) {
      return cached;
    }
  } catch {
    // Cache miss is fine
  }

  const parser = new Parser({
    timeout: FETCH_TIMEOUT_MS,
    headers: {
      'User-Agent': 'KanoonSaathi/1.0 (+https://kanoonsaathi.in)',
      'Accept': 'application/rss+xml, application/xml, text/xml',
    },
  });

  // Fetch all feeds concurrently
  const results = await Promise.allSettled(
    FEEDS.map(f => fetchFeed(f, parser)),
  );

  const all: NewsItem[] = [];
  for (const result of results) {
    if (result.status === 'fulfilled') {
      all.push(...result.value);
    }
  }

  // Sort newest first
  all.sort((a, b) => new Date(b.publishedAt).getTime() - new Date(a.publishedAt).getTime());

  // Deduplicate by id
  const seen = new Set<string>();
  const deduped = all.filter(item => {
    if (seen.has(item.id)) return false;
    seen.add(item.id);
    return true;
  });

  // Cache for 30 min (even an empty array — no point re-fetching immediately on failure)
  if (deduped.length > 0) {
    redis.setex(CACHE_KEY, CACHE_TTL, JSON.stringify(deduped)).catch(() => {});
  }

  logger.info({ count: deduped.length }, '[RSS] Feed refreshed');
  return deduped;
}
