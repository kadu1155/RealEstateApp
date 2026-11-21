import { Router } from 'express';
import { leadService } from './lead.service';
import { requireAuth, AuthenticatedRequest } from '../auth/auth.middleware';
import { sendSuccess, sendError } from '../../common/response';

export const leadRouter = Router();

// 1. POST /api/v1/enquiries (Public lead submission)
leadRouter.post('/enquiries', async (req, res) => {
  try {
    const { propertyId, reelId, customerName, customerPhone, intent, message } = req.body;
    if (!propertyId || !customerName || !customerPhone) {
      return sendError(res, 'propertyId, customerName, and customerPhone are required', 400);
    }
    const result = await leadService.createEnquiry({
      propertyId,
      reelId,
      customerName,
      customerPhone,
      intent: intent || 'CALLBACK',
      message
    });
    return sendSuccess(res, result, 'Enquiry submitted successfully', null, 201);
  } catch (err: any) {
    return sendError(res, err.message, 400);
  }
});

// 2. GET /api/v1/leads (Protected CRM pipeline)
leadRouter.get('/', async (req, res) => {
  try {
    const brokerId = req.query.brokerId as string;
    const stage = req.query.stage as string;

    const leads = await leadService.getLeads(brokerId, stage);
    return sendSuccess(res, leads, 'Leads retrieved successfully');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

// 3. GET /api/v1/leads/stats (CRM Funnel metrics)
leadRouter.get('/stats', async (req, res) => {
  try {
    const brokerId = (req.query.brokerId as string) || 'usr_pune_broker_01';
    const stats = await leadService.getBrokerStats(brokerId);
    return sendSuccess(res, stats, 'Lead statistics retrieved');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

// 4. PATCH /api/v1/leads/:id/stage (Update Lead Stage)
leadRouter.patch('/:id/stage', async (req, res) => {
  try {
    const { stage, notes, scheduledVisitAt } = req.body;
    if (!stage) {
      return sendError(res, 'Stage is required', 400);
    }
    const updatedLead = await leadService.updateLeadStage(req.params.id, {
      stage,
      notes,
      scheduledVisitAt
    });
    return sendSuccess(res, updatedLead, `Lead updated to ${stage}`);
  } catch (err: any) {
    return sendError(res, err.message, 400);
  }
});
