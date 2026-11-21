import { Router } from 'express';
import { notificationService } from './notification.service';
import { whatsappService } from './whatsapp.service';
import { requireAuth, AuthenticatedRequest } from '../auth/auth.middleware';
import { sendSuccess, sendError } from '../../common/response';

export const notificationRouter = Router();

// 1. GET /api/v1/notifications
notificationRouter.get('/', requireAuth, async (req: AuthenticatedRequest, res) => {
  try {
    const userId = req.user!.userId;
    const notifications = await notificationService.getUserNotifications(userId);
    return sendSuccess(res, notifications, 'Notifications retrieved successfully');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

// 2. PATCH /api/v1/notifications/:id/read
notificationRouter.patch('/:id/read', requireAuth, async (req: AuthenticatedRequest, res) => {
  try {
    const userId = req.user!.userId;
    const updated = await notificationService.markAsRead(req.params.id, userId);
    if (!updated) {
      return sendError(res, 'Notification not found or access denied', 404);
    }
    return sendSuccess(res, updated, 'Notification marked as read');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

// 3. PATCH /api/v1/notifications/read-all
notificationRouter.patch('/read-all', requireAuth, async (req: AuthenticatedRequest, res) => {
  try {
    const userId = req.user!.userId;
    const result = await notificationService.markAllAsRead(userId);
    return sendSuccess(res, result, 'All notifications marked as read');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

// 4. POST /api/v1/notifications/whatsapp/preview
notificationRouter.post('/whatsapp/preview', async (req, res) => {
  try {
    const { phone, type, buyerName, propertyTitle, priceDisplay, locality, intent, shareUrl } = req.body;
    let messageText = '';

    if (type === 'ENQUIRY') {
      messageText = whatsappService.generateEnquiryMessage(
        buyerName || 'Prospective Buyer',
        propertyTitle || 'Featured Property',
        priceDisplay || 'Price upon request',
        locality || 'Pune',
        intent || 'Site Visit'
      );
    } else if (type === 'SHARE') {
      messageText = whatsappService.generateShareMessage(
        propertyTitle || 'Featured Property',
        priceDisplay || 'Price upon request',
        locality || 'Pune',
        shareUrl || 'https://propreel.in/properties'
      );
    } else {
      return sendError(res, 'Invalid preview type. Must be ENQUIRY or SHARE', 400);
    }

    const clickToChatUrl = whatsappService.generateClickToChatUrl(phone || '919876543210', messageText);

    return sendSuccess(res, {
      messageText,
      clickToChatUrl
    }, 'WhatsApp preview generated');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});
