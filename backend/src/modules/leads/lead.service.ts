import { query, withTransaction } from '../../database/db';
import { notificationService } from '../notifications/notification.service';

export interface CreateEnquiryInput {
  propertyId: string;
  reelId?: string;
  customerName: string;
  customerPhone: string;
  intent: 'SITE_VISIT' | 'CALLBACK' | 'NEGOTIATION' | 'PRICE_DETAILS';
  message?: string;
}

export interface UpdateStageInput {
  stage: 'NEW' | 'CONTACTED' | 'SITE_VISIT_SCHEDULED' | 'NEGOTIATION' | 'CLOSED_WON' | 'LOST';
  notes?: string;
  scheduledVisitAt?: string;
}

export class LeadService {

  // Process consumer enquiry and auto-route to broker/seller
  async createEnquiry(input: CreateEnquiryInput) {
    // 1. Fetch property and seller ID
    const propRes = await query(
      'SELECT id, user_id, title, price_display, locality FROM properties WHERE id = $1',
      [input.propertyId]
    );

    if (propRes.rows.length === 0) {
      throw new Error('Property not found');
    }
    const prop = propRes.rows[0];
    const brokerId = prop.user_id;

    return await withTransaction(async (client) => {
      // 2. Insert into enquiries
      const enqSql = `
        INSERT INTO enquiries (property_id, reel_id, customer_name, customer_phone, message, intent)
        VALUES ($1, $2, $3, $4, $5, $6)
        RETURNING *
      `;
      const enqRes = await client.query(enqSql, [
        input.propertyId,
        input.reelId || null,
        input.customerName,
        input.customerPhone,
        input.message || `Interested via PropReel (${input.intent})`,
        input.intent
      ]);
      const enquiry = enqRes.rows[0];

      // 3. Upsert into leads CRM for the broker/seller
      const leadSql = `
        INSERT INTO leads (
          broker_id, customer_name, customer_phone, property_id,
          property_title, property_price_display, locality, stage, notes
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, 'NEW', $8)
        RETURNING *
      `;
      const leadRes = await client.query(leadSql, [
        brokerId,
        input.customerName,
        input.customerPhone,
        prop.id,
        prop.title,
        prop.price_display,
        prop.locality,
        `Enquiry Intent: ${input.intent}. Message: ${input.message || 'Direct enquiry'}`
      ]);
      const lead = leadRes.rows[0];

      // 4. Record initial lead activity
      await client.query(
        `INSERT INTO lead_activities (lead_id, activity_type, note)
         VALUES ($1, 'ENQUIRY_RECEIVED', $2)`,
        [lead.id, `Received new enquiry for ${prop.title}`]
      );

      // 5. Increment property enquiry count
      await client.query(
        'UPDATE properties SET enquiries_count = enquiries_count + 1 WHERE id = $1',
        [prop.id]
      );

      return {
        enquiryId: enquiry.id,
        leadId: lead.id,
        stage: lead.stage,
        createdAt: lead.created_at
      };
    });

    // Multi-Channel (In-App + WhatsApp) Alert to Broker
    notificationService.notifyBrokerOfLead({
      brokerId,
      brokerName: 'Broker Partner',
      brokerPhone: '+919822000000',
      buyerName: input.customerName,
      buyerPhone: input.customerPhone,
      propertyTitle: prop.title,
      priceDisplay: prop.price_display,
      intent: input.intent,
      leadId: result.leadId
    }).catch(err => console.warn('[LeadService] Notification trigger error:', err.message));

    return result;
  }

  // Get CRM leads for broker with stage filtering
  async getLeads(brokerId?: string, stage?: string) {
    const conditions: string[] = ['1=1'];
    const params: any[] = [];
    let pIdx = 1;

    if (brokerId) {
      conditions.push(`broker_id = $${pIdx++}`);
      params.push(brokerId);
    }
    if (stage && stage !== 'ALL') {
      conditions.push(`stage = $${pIdx++}`);
      params.push(stage);
    }

    const sql = `
      SELECT * FROM leads
      WHERE ${conditions.join(' AND ')}
      ORDER BY 
        CASE WHEN stage = 'NEW' THEN 1
             WHEN stage = 'CONTACTED' THEN 2
             WHEN stage = 'SITE_VISIT_SCHEDULED' THEN 3
             WHEN stage = 'NEGOTIATION' THEN 4
             ELSE 5 END ASC,
        created_at DESC
    `;

    const res = await query(sql, params);
    return res.rows;
  }

  // Update CRM Lead Stage & log activity timeline
  async updateLeadStage(leadId: string, input: UpdateStageInput, userId?: string) {
    return await withTransaction(async (client) => {
      const updateSql = `
        UPDATE leads
        SET 
          stage = $1,
          notes = COALESCE($2, notes),
          scheduled_visit_at = COALESCE($3::TIMESTAMP, scheduled_visit_at),
          updated_at = NOW()
        WHERE id = $4
        RETURNING *
      `;

      const res = await client.query(updateSql, [
        input.stage,
        input.notes || null,
        input.scheduledVisitAt || null,
        leadId
      ]);

      if (res.rows.length === 0) {
        throw new Error('Lead not found');
      }

      const updatedLead = res.rows[0];

      // Log activity
      await client.query(
        `INSERT INTO lead_activities (lead_id, activity_type, note)
         VALUES ($1, 'STAGE_CHANGE', $2)`,
        [leadId, `Stage changed to ${input.stage}. ${input.notes || ''}`]
      );

      return updatedLead;
    });
  }

  // CRM Funnel Analytics for Broker Dashboard
  async getBrokerStats(brokerId: string) {
    const sql = `
      SELECT
        COUNT(*) as total_leads,
        COUNT(*) FILTER (WHERE stage = 'NEW') as new_leads,
        COUNT(*) FILTER (WHERE stage = 'CONTACTED') as contacted_leads,
        COUNT(*) FILTER (WHERE stage = 'SITE_VISIT_SCHEDULED') as visits_scheduled,
        COUNT(*) FILTER (WHERE stage = 'NEGOTIATION') as in_negotiation,
        COUNT(*) FILTER (WHERE stage = 'CLOSED_WON') as closed_won,
        COUNT(*) FILTER (WHERE stage = 'LOST') as lost_leads
      FROM leads
      WHERE broker_id = $1
    `;
    const res = await query(sql, [brokerId]);
    const row = res.rows[0] || {};

    const total = parseInt(row.total_leads || '0', 10);
    const won = parseInt(row.closed_won || '0', 10);
    const conversionRate = total > 0 ? ((won / total) * 100).toFixed(1) : '0.0';

    return {
      totalLeads: total,
      newLeads: parseInt(row.new_leads || '0', 10),
      contactedLeads: parseInt(row.contacted_leads || '0', 10),
      visitsScheduled: parseInt(row.visits_scheduled || '0', 10),
      inNegotiation: parseInt(row.in_negotiation || '0', 10),
      closedWon: won,
      lostLeads: parseInt(row.lost_leads || '0', 10),
      conversionRatePercent: parseFloat(conversionRate)
    };
  }
}

export const leadService = new LeadService();
