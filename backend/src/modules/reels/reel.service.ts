import { query } from '../../database/db';

export interface ReelFeedQuery {
  city?: string;
  page?: number;
  limit?: number;
}

export interface CreateReelInput {
  userId: string;
  propertyId?: string;
  caption: string;
  videoUrl: string;
  thumbnailUrl: string;
  durationSeconds: number;
  city: string;
  locality: string;
}

export class ReelService {

  // Fetch Ranked Reels Feed for City
  async getFeed(params: ReelFeedQuery) {
    const city = params.city || 'Pune';
    const limit = Math.min(params.limit || 20, 50);
    const offset = ((params.page || 1) - 1) * limit;

    const sql = `
      SELECT 
        r.*,
        u.full_name as creator_name,
        u.verification_badge as creator_badge,
        u.role as creator_role,
        u.avatar_url as creator_avatar,
        p.title as property_title,
        p.price_display as property_price_display,
        p.locality as property_locality,
        p.bhk as property_bhk,
        p.carpet_area_sqft as property_carpet_area
      FROM reels r
      JOIN users u ON r.user_id = u.id
      LEFT JOIN properties p ON r.property_id = p.id
      WHERE r.city ILIKE $1 AND r.status = 'ACTIVE'
      ORDER BY r.is_sponsored DESC, r.created_at DESC
      LIMIT $2 OFFSET $3
    `;

    try {
      const res = await query(sql, [city, limit, offset]);
      return res.rows;
    } catch (err: any) {
      console.warn('[DB Warning] Database unreachable, serving pre-seeded Pune reels:', err.message);
      return [
        {
          id: 'r1111111-1111-1111-1111-111111111111',
          caption: 'Check out this breathtaking 3 BHK with 180° unobstructed hills view in Baner!',
          video_url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
          thumbnail_url: 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=80',
          city: 'Pune',
          locality: 'Baner',
          creator_name: 'Vikram Deshmukh',
          creator_badge: 'RERA_VERIFIED',
          views_count: 4820,
          likes_count: 428
        },
        {
          id: 'r2222222-2222-2222-2222-222222222222',
          caption: 'Zero brokerage! Selling my East-facing 2 BHK flat in Pristine Grandeur, Wakad.',
          video_url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
          thumbnail_url: 'https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=1000&q=80',
          city: 'Pune',
          locality: 'Wakad',
          creator_name: 'Priya Kulkarni',
          creator_badge: 'OWNER_VERIFIED',
          views_count: 2150,
          likes_count: 215
        }
      ];
    }
  }

  // Get Single Reel by ID
  async getReelById(id: string) {
    const sql = `
      SELECT 
        r.*,
        u.full_name as creator_name,
        u.verification_badge as creator_badge,
        u.role as creator_role,
        u.avatar_url as creator_avatar,
        p.title as property_title,
        p.price_display as property_price_display,
        p.locality as property_locality,
        p.bhk as property_bhk
      FROM reels r
      JOIN users u ON r.user_id = u.id
      LEFT JOIN properties p ON r.property_id = p.id
      WHERE r.id = $1
    `;
    const res = await query(sql, [id]);
    if (res.rows.length === 0) {
      throw new Error('Reel not found');
    }
    return res.rows[0];
  }

  // Create New Real Estate Video Reel
  async createReel(input: CreateReelInput) {
    if (input.durationSeconds > 60) {
      throw new Error('Real estate reels must not exceed 60 seconds.');
    }

    const sql = `
      INSERT INTO reels (
        user_id, property_id, caption, video_url, thumbnail_url,
        duration_seconds, city, locality, status
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'ACTIVE')
      RETURNING *
    `;

    const values = [
      input.userId,
      input.propertyId || null,
      input.caption,
      input.videoUrl,
      input.thumbnailUrl,
      input.durationSeconds,
      input.city,
      input.locality
    ];

    const res = await query(sql, values);
    const newReel = res.rows[0];

    // If linked to a property, update property's linked_reel_id
    if (input.propertyId) {
      await query('UPDATE properties SET linked_reel_id = $1 WHERE id = $2', [newReel.id, input.propertyId]);
    }

    return newReel;
  }

  // Toggle Reel Like
  async toggleLike(userId: string, reelId: string): Promise<{ liked: boolean; likesCount: number }> {
    const checkSql = `
      SELECT id FROM reel_engagements 
      WHERE user_id = $1 AND reel_id = $2 AND engagement_type = 'LIKE'
    `;
    const existing = await query(checkSql, [userId, reelId]);

    if (existing.rows.length > 0) {
      // Remove Like
      await query('DELETE FROM reel_engagements WHERE id = $1', [existing.rows[0].id]);
      const updateRes = await query(
        'UPDATE reels SET likes_count = GREATEST(likes_count - 1, 0) WHERE id = $1 RETURNING likes_count',
        [reelId]
      );
      return { liked: false, likesCount: updateRes.rows[0]?.likes_count ?? 0 };
    } else {
      // Add Like
      await query(
        "INSERT INTO reel_engagements (user_id, reel_id, engagement_type) VALUES ($1, $2, 'LIKE')",
        [userId, reelId]
      );
      const updateRes = await query(
        'UPDATE reels SET likes_count = likes_count + 1 WHERE id = $1 RETURNING likes_count',
        [reelId]
      );
      return { liked: true, likesCount: updateRes.rows[0]?.likes_count ?? 1 };
    }
  }

  // Record Reel View (Increments view counter)
  async recordView(reelId: string, userId?: string, watchTimeSeconds = 3) {
    // Increment reel views count
    await query('UPDATE reels SET views_count = views_count + 1 WHERE id = $1', [reelId]);

    if (userId) {
      await query(
        `INSERT INTO reel_engagements (user_id, reel_id, engagement_type, watch_time_seconds)
         VALUES ($1, $2, 'VIEW', $3)`,
        [userId, reelId, watchTimeSeconds]
      ).catch(() => {});
    }
  }

  // Record Enquiry Click on Reel
  async recordEnquiryClick(reelId: string) {
    await query('UPDATE reels SET enquiry_clicks_count = enquiry_clicks_count + 1 WHERE id = $1', [reelId]);
  }
}

export const reelService = new ReelService();
