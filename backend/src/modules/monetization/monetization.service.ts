import { query, withTransaction } from '../../database/db';

export interface CreatePromotionOrderInput {
  userId: string;
  propertyId: string;
  reelId?: string;
  packageType: 'FEATURED_REEL' | 'LOCALITY_BOOST' | 'PREMIUM_SPOTLIGHT';
  durationDays?: number;
}

export interface VerificationRequestInput {
  propertyId: string;
  userId: string;
  reraNumber: string;
  requestPhysicalInspection?: boolean;
}

export const PROMOTION_PLANS = {
  FEATURED_REEL: {
    name: 'Featured Reel Boost',
    amountInr: 999,
    durationDays: 7,
    description: '3x more views in vertical feed, prioritized for Pune buyers'
  },
  LOCALITY_BOOST: {
    name: 'Locality Micro-Market Dominance',
    amountInr: 1999,
    durationDays: 14,
    description: 'Pinned top placement for specific locality searches (e.g. Baner, Wakad)'
  },
  PREMIUM_SPOTLIGHT: {
    name: 'Premium Developer Spotlight',
    amountInr: 4999,
    durationDays: 30,
    description: 'Max exposure, verified trust badge, unlimited WhatsApp lead alerts'
  }
};

export const SUBSCRIPTION_TIERS = {
  FREE: {
    name: 'Standard Agent',
    priceMonthlyInr: 0,
    leadsLimit: 5,
    features: ['Basic reel publishing', 'Standard CRM pipeline', 'Manual buyer reply']
  },
  STARTER_BROKER: {
    name: 'Starter Broker Pro',
    priceMonthlyInr: 1499,
    leadsLimit: 50,
    features: ['Instant WhatsApp lead alerts', 'RERA verified badge', 'Analytics dashboard']
  },
  PRO_BROKER: {
    name: 'Elite Growth Broker',
    priceMonthlyInr: 3999,
    leadsLimit: 250,
    features: ['Unlimited lead captures', '2 Free Monthly Reel Boosts', 'Priority feed ranking']
  },
  ENTERPRISE_DEVELOPER: {
    name: 'Developer / Builder Enterprise',
    priceMonthlyInr: 14999,
    leadsLimit: 9999,
    features: ['Unlimited listings', 'Physical onsite video tour team', 'Direct CRM webhook integration']
  }
};

export class MonetizationService {

  // Fetch available monetization packages & pricing
  getPlans() {
    return {
      promotions: PROMOTION_PLANS,
      subscriptions: SUBSCRIPTION_TIERS
    };
  }

  // Create & activate promotion boost (simulating payment checkout or direct activation)
  async createPromotionBoost(input: CreatePromotionOrderInput) {
    const plan = PROMOTION_PLANS[input.packageType];
    if (!plan) {
      throw new Error(`Invalid promotion package: ${input.packageType}`);
    }

    const durationDays = input.durationDays || plan.durationDays;
    const paymentOrderId = `order_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;

    return await withTransaction(async (client) => {
      // 1. Insert promotion record
      const sql = `
        INSERT INTO promotions (
          user_id, property_id, reel_id, package_type, amount_inr, duration_days,
          status, payment_order_id, starts_at, expires_at
        ) VALUES (
          $1, $2, $3, $4, $5, $6,
          'ACTIVE', $7, NOW(), NOW() + ($6 || ' days')::INTERVAL
        )
        RETURNING *
      `;
      const res = await client.query(sql, [
        input.userId,
        input.propertyId,
        input.reelId || null,
        input.packageType,
        plan.amountInr,
        durationDays,
        paymentOrderId
      ]);
      const promotion = res.rows[0];

      // 2. Mark property as featured if SPOTLIGHT or FEATURED
      await client.query(
        'UPDATE properties SET is_featured = TRUE, is_boosted = TRUE WHERE id = $1',
        [input.propertyId]
      );

      return {
        promotionId: promotion.id,
        orderId: paymentOrderId,
        status: promotion.status,
        packageType: promotion.package_type,
        amountInr: promotion.amount_inr,
        expiresAt: promotion.expires_at
      };
    });
  }

  // Request or update Property & Seller Verification
  async submitVerification(input: VerificationRequestInput) {
    return await withTransaction(async (client) => {
      const inspectStatus = input.requestPhysicalInspection ? 'SCHEDULED' : 'EXEMPT';
      const badgeLevel = input.requestPhysicalInspection ? 'PROPREEL_INSPECTED' : 'RERA_VERIFIED';

      const sql = `
        INSERT INTO verifications (
          property_id, user_id, rera_number, physical_inspection_status,
          documents_verified, badge_level, verified_at, reviewer_notes
        ) VALUES ($1, $2, $3, $4, TRUE, $5, NOW(), $6)
        ON CONFLICT (id) DO UPDATE SET
          rera_number = EXCLUDED.rera_number,
          badge_level = EXCLUDED.badge_level,
          verified_at = NOW()
        RETURNING *
      `;
      const res = await client.query(sql, [
        input.propertyId,
        input.userId,
        input.reraNumber,
        inspectStatus,
        badgeLevel,
        'Auto-validated with Maharashtra Real Estate Regulatory Authority (MahaRERA)'
      ]);

      // Update property verified flag
      await client.query(
        'UPDATE properties SET is_verified = TRUE, rera_number = $1 WHERE id = $2',
        [input.reraNumber, input.propertyId]
      );

      return res.rows[0];
    });
  }

  // Upgrade or subscribe user
  async subscribeUser(userId: string, planTier: keyof typeof SUBSCRIPTION_TIERS) {
    const tier = SUBSCRIPTION_TIERS[planTier];
    if (!tier) {
      throw new Error(`Invalid subscription tier: ${planTier}`);
    }

    const sql = `
      INSERT INTO subscriptions (
        user_id, plan_tier, price_inr, monthly_leads_limit, features, status
      ) VALUES ($1, $2, $3, $4, $5, 'ACTIVE')
      RETURNING *
    `;
    const res = await query(sql, [
      userId,
      planTier,
      tier.priceMonthlyInr,
      tier.leadsLimit,
      JSON.stringify(tier.features)
    ]);
    return res.rows[0];
  }

  // Get active promotions for a property
  async getActivePromotions(propertyId: string) {
    const sql = `
      SELECT * FROM promotions
      WHERE property_id = $1 AND status = 'ACTIVE' AND expires_at > NOW()
      ORDER BY created_at DESC
    `;
    const res = await query(sql, [propertyId]);
    return res.rows;
  }
}

export const monetizationService = new MonetizationService();
