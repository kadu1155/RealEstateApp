import Redis from 'ioredis';
import { config } from '../common/config';

export const redis = new Redis(config.redisUrl, {
  maxRetriesPerRequest: 3,
  retryStrategy(times) {
    if (times > 5) return null;
    return Math.min(times * 100, 3000);
  },
  lazyConnect: true
});

redis.on('error', (err) => {
  // Graceful degradation when Redis is optional in local offline dev
  if (config.nodeEnv === 'development') {
    // Suppress spammy offline logs during dev
  } else {
    console.error('[Redis] Connection error:', err.message);
  }
});

export async function getCachedJson<T>(key: string): Promise<T | null> {
  try {
    if (redis.status !== 'ready') return null;
    const data = await redis.get(key);
    return data ? (JSON.parse(data) as T) : null;
  } catch {
    return null;
  }
}

export async function setCachedJson(key: string, value: any, ttlSeconds = 300): Promise<void> {
  try {
    if (redis.status !== 'ready') return;
    await redis.setex(key, ttlSeconds, JSON.stringify(value));
  } catch {
    // Non-blocking cache failure
  }
}

export async function invalidateCachePrefix(prefix: string): Promise<void> {
  try {
    if (redis.status !== 'ready') return;
    const keys = await redis.keys(`${prefix}*`);
    if (keys.length > 0) {
      await redis.del(...keys);
    }
  } catch {
    // Non-blocking
  }
}
