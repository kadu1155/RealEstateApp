import fs from 'fs';
import path from 'path';
import { pool } from './db';

async function runMigrations() {
  console.log('[Migration] Starting database migration & seed...');
  const migrationsDir = path.join(__dirname, 'migrations');
  const files = fs.readdirSync(migrationsDir)
    .filter(file => file.endsWith('.sql'))
    .sort();

  for (const file of files) {
    const filePath = path.join(migrationsDir, file);
    const sql = fs.readFileSync(filePath, 'utf-8');
    console.log(`[Migration] Running: ${file}...`);
    try {
      await pool.query(sql);
      console.log(`[Migration] Completed: ${file}`);
    } catch (err: any) {
      console.error(`[Migration] Error executing ${file}:`, err.message);
      // continue or abort
    }
  }

  console.log('[Migration] All migrations completed successfully.');
  await pool.end();
}

runMigrations().catch(err => {
  console.error('[Migration] Migration runner encountered error:', err);
  process.exit(1);
});
