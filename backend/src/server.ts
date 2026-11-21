import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import morgan from 'morgan';
import { config } from './common/config';
import { query, checkDatabaseHealth } from './database/db';
import { sendSuccess, sendError } from './common/response';
import { authRouter } from './modules/auth/auth.controller';
import { propertyRouter } from './modules/properties/property.controller';
import { reelRouter } from './modules/reels/reel.controller';
import { leadRouter } from './modules/leads/lead.controller';
import { notificationRouter } from './modules/notifications/notification.controller';
import { monetizationRouter } from './modules/monetization/monetization.controller';

const app = express();

app.use(helmet());
app.use(cors({ origin: '*' }));
app.use(express.json());
app.use(morgan('dev'));

// Mount Modules
app.use('/api/v1/auth', authRouter);
app.use('/api/v1/properties', propertyRouter);
app.use('/api/v1/reels', reelRouter);
app.use('/api/v1/leads', leadRouter);
app.use('/api/v1/notifications', notificationRouter);
app.use('/api/v1/monetization', monetizationRouter);
app.use('/api/v1', leadRouter); // also handles /api/v1/enquiries

// 1. Healthcheck
app.get('/api/health', async (req, res) => {
  const dbOk = await checkDatabaseHealth();
  res.json({
    status: dbOk ? 'healthy' : 'degraded',
    service: 'propreel-backend-api',
    version: '1.0.0',
    timestamp: new Date().toISOString(),
    database: dbOk ? 'connected' : 'disconnected'
  });
});

// Start Server
if (process.env.NODE_ENV !== 'test' && !process.env.VERCEL) {
  app.listen(config.port, () => {
    console.log(`[PropReel Backend] Listening on port ${config.port} (env: ${config.nodeEnv})`);
  });
}

export default app;
