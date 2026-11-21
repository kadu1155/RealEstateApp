import { query } from '../../database/db';
import { whatsappService } from './whatsapp.service';

export interface CreateNotificationInput {
  userId: string;
  title: string;
  body: string;
  type: 'LEAD_RECEIVED' | 'SITE_VISIT_SCHEDULED' | 'STAGE_UPDATED' | 'PRICE_DROP' | 'SYSTEM';
  data?: Record<string, any>;
}

export class NotificationService {
  
  // Create in-app notification
  async createNotification(input: CreateNotificationInput) {
    const sql = `
      INSERT INTO notifications (user_id, title, body, type, data, is_read)
      VALUES ($1, $2, $3, $4, $5, FALSE)
      RETURNING *
    `;
    const res = await query(sql, [
      input.userId,
      input.title,
      input.body,
      input.type,
      JSON.stringify(input.data || {})
    ]);
    return res.rows[0];
  }

  // Get User's notifications
  async getUserNotifications(userId: string, limit = 50) {
    const sql = `
      SELECT * FROM notifications
      WHERE user_id = $1
      ORDER BY created_at DESC
      LIMIT $2
    `;
    const res = await query(sql, [userId, limit]);
    return res.rows;
  }

  // Mark notification as read
  async markAsRead(notificationId: string, userId: string) {
    const sql = `
      UPDATE notifications
      SET is_read = TRUE
      WHERE id = $1 AND user_id = $2
      RETURNING *
    `;
    const res = await query(sql, [notificationId, userId]);
    return res.rows[0];
  }

  // Mark all notifications as read
  async markAllAsRead(userId: string) {
    const sql = `
      UPDATE notifications
      SET is_read = TRUE
      WHERE user_id = $1 AND is_read = FALSE
    `;
    const res = await query(sql, [userId]);
    return { updated: res.rowCount };
  }

  // Notify broker of a new lead via In-App + WhatsApp
  async notifyBrokerOfLead(params: {
    brokerId: string;
    brokerName: string;
    brokerPhone: string;
    buyerName: string;
    buyerPhone: string;
    propertyTitle: string;
    priceDisplay: string;
    intent: string;
    leadId: string;
  }) {
    // 1. In-App Notification
    await this.createNotification({
      userId: params.brokerId,
      title: '🎯 New Lead Received!',
      body: `${params.buyerName} inquired about ${params.propertyTitle} (${params.priceDisplay})`,
      type: 'LEAD_RECEIVED',
      data: { leadId: params.leadId, buyerPhone: params.buyerPhone, intent: params.intent }
    }).catch(err => console.warn('[NotificationService] In-app notify failed:', err.message));

    // 2. WhatsApp Multi-Channel Alert
    const waResult = await whatsappService.sendLeadAlertToBroker({
      brokerPhone: params.brokerPhone,
      brokerName: params.brokerName,
      buyerName: params.buyerName,
      buyerPhone: params.buyerPhone,
      propertyTitle: params.propertyTitle,
      priceDisplay: params.priceDisplay,
      intent: params.intent
    });

    return { inAppSent: true, whatsappResult: waResult };
  }
}

export const notificationService = new NotificationService();
