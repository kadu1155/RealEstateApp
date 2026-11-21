import { Router } from 'express';
import { monetizationService } from './monetization.service';
import { requireAuth, AuthenticatedRequest } from '../auth/auth.middleware';
import { sendSuccess, sendError } from '../../common/response';

export const monetizationRouter = Router();

// 1. GET /api/v1/monetization/plans (Public pricing catalog)
monetizationRouter.get('/plans', (req, res) => {
  try {
    const catalog = monetizationService.getPlans();
    return sendSuccess(res, catalog, 'Monetization plans retrieved');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

// 2. POST /api/v1/monetization/boost (Promote Reel / Property)
monetizationRouter.post('/boost', async (req, res) => {
  try {
    const { userId, propertyId, reelId, packageType, durationDays } = req.body;
    if (!propertyId || !packageType) {
      return sendError(res, 'propertyId and packageType are required', 400);
    }

    const boostResult = await monetizationService.createPromotionBoost({
      userId: userId || 'usr_pune_broker_01',
      propertyId,
      reelId,
      packageType,
      durationDays
    });

    return sendSuccess(res, boostResult, 'Promotion boost activated successfully', null, 201);
  } catch (err: any) {
    return sendError(res, err.message, 400);
  }
});

// 3. POST /api/v1/monetization/verify (RERA & Physical Inspection Request)
monetizationRouter.post('/verify', async (req, res) => {
  try {
    const { userId, propertyId, reraNumber, requestPhysicalInspection } = req.body;
    if (!propertyId || !reraNumber) {
      return sendError(res, 'propertyId and reraNumber are required', 400);
    }

    const verification = await monetizationService.submitVerification({
      userId: userId || 'usr_pune_broker_01',
      propertyId,
      reraNumber,
      requestPhysicalInspection: Boolean(requestPhysicalInspection)
    });

    return sendSuccess(res, verification, 'Verification submitted and approved', null, 201);
  } catch (err: any) {
    return sendError(res, err.message, 400);
  }
});

// 4. POST /api/v1/monetization/subscribe (Broker Tier Subscription)
monetizationRouter.post('/subscribe', async (req, res) => {
  try {
    const { userId, planTier } = req.body;
    if (!planTier) {
      return sendError(res, 'planTier is required (e.g. STARTER_BROKER, PRO_BROKER)', 400);
    }

    const subscription = await monetizationService.subscribeUser(
      userId || 'usr_pune_broker_01',
      planTier
    );
    return sendSuccess(res, subscription, `Upgraded to ${planTier}`);
  } catch (err: any) {
    return sendError(res, err.message, 400);
  }
});

// 5. GET /api/v1/monetization/promotions/:propertyId (Check active promotions)
monetizationRouter.get('/promotions/:propertyId', async (req, res) => {
  try {
    const promotions = await monetizationService.getActivePromotions(req.params.propertyId);
    return sendSuccess(res, promotions, 'Active promotions retrieved');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});
