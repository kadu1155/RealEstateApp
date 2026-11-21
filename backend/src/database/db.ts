import { Pool, PoolClient, QueryResult } from 'pg';
import { config } from '../common/config';

export const pool = new Pool({
  connectionString: config.databaseUrl,
  max: 10,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
  ssl: config.databaseUrl && !config.databaseUrl.includes('localhost') && !config.databaseUrl.includes('127.0.0.1')
    ? { rejectUnauthorized: false }
    : undefined
});

pool.on('error', (err) => {
  console.error('[DB] Unexpected error on idle client', err);
});

export async function query<T = any>(text: string, params?: any[]): Promise<QueryResult<T>> {
  const start = Date.now();
  const res = await pool.query<T>(text, params);
  const duration = Date.now() - start;
  if (config.nodeEnv === 'development' && duration > 50) {
    console.log(`[DB Query] ${text.slice(0, 80)}... took ${duration}ms, rows: ${res.rowCount}`);
  }
  return res;
}

export async function withTransaction<T>(callback: (client: PoolClient) => Promise<T>): Promise<T> {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await callback(client);
    await client.query('COMMIT');
    return result;
  } catch (e) {
    await client.query('ROLLBACK');
    throw e;
  } finally {
    client.release();
  }
}

export async function checkDatabaseHealth(): Promise<boolean> {
  try {
    const res = await query('SELECT 1 as health');
    return res.rows[0]?.health === 1;
  } catch (err) {
    console.error('[DB] Healthcheck failed:', err);
    return false;
  }
}
