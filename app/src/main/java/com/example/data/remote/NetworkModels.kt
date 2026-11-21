package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: T?,
    @Json(name = "message") val message: String?,
    @Json(name = "meta") val meta: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class NetworkProperty(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "seller_name") val sellerName: String?,
    @Json(name = "seller_role") val sellerRole: String?,
    @Json(name = "seller_phone") val sellerPhone: String?,
    @Json(name = "seller_badge") val sellerBadge: String?,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "category") val category: String,
    @Json(name = "price") val price: Double,
    @Json(name = "price_display") val priceDisplay: String,
    @Json(name = "is_negotiable") val isNegotiable: Boolean = false,
    @Json(name = "maintenance_charge") val maintenanceCharge: Double = 0.0,
    @Json(name = "city") val city: String,
    @Json(name = "locality") val locality: String,
    @Json(name = "landmark") val landmark: String?,
    @Json(name = "bhk") val bhk: Int?,
    @Json(name = "bathrooms") val bathrooms: Int?,
    @Json(name = "carpet_area_sqft") val carpetAreaSqft: Double,
    @Json(name = "floor_number") val floorNumber: Int?,
    @Json(name = "total_floors") val totalFloors: Int?,
    @Json(name = "furnishing") val furnishing: String,
    @Json(name = "possession") val possession: String,
    @Json(name = "rera_id") val reraId: String?,
    @Json(name = "amenities") val amenities: List<String>?,
    @Json(name = "image_urls") val imageUrls: List<String>?,
    @Json(name = "linked_reel_id") val linkedReelId: String?,
    @Json(name = "is_featured") val isFeatured: Boolean = false,
    @Json(name = "views_count") val viewsCount: Int = 0,
    @Json(name = "enquiries_count") val enquiriesCount: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NetworkReel(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "creator_name") val creatorName: String?,
    @Json(name = "creator_badge") val creatorBadge: String?,
    @Json(name = "creator_role") val creatorRole: String?,
    @Json(name = "property_id") val propertyId: String?,
    @Json(name = "property_title") val propertyTitle: String?,
    @Json(name = "property_price_display") val propertyPriceDisplay: String?,
    @Json(name = "property_locality") val propertyLocality: String?,
    @Json(name = "property_bhk") val propertyBhk: Int?,
    @Json(name = "caption") val caption: String,
    @Json(name = "video_url") val videoUrl: String,
    @Json(name = "thumbnail_url") val thumbnailUrl: String,
    @Json(name = "duration_seconds") val durationSeconds: Double,
    @Json(name = "city") val city: String,
    @Json(name = "locality") val locality: String,
    @Json(name = "is_sponsored") val isSponsored: Boolean = false,
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "comments_count") val commentsCount: Int = 0,
    @Json(name = "saves_count") val savesCount: Int = 0,
    @Json(name = "shares_count") val sharesCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class NetworkLead(
    @Json(name = "id") val id: String,
    @Json(name = "enquiry_id") val enquiryId: String,
    @Json(name = "broker_id") val brokerId: String,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "customer_phone") val customerPhone: String,
    @Json(name = "property_id") val propertyId: String,
    @Json(name = "property_title") val propertyTitle: String,
    @Json(name = "property_price_display") val propertyPriceDisplay: String,
    @Json(name = "locality") val locality: String,
    @Json(name = "stage") val stage: String,
    @Json(name = "notes") val notes: String?,
    @Json(name = "scheduled_visit_at") val scheduledVisitAt: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class EnquiryRequest(
    @Json(name = "propertyId") val propertyId: String,
    @Json(name = "reelId") val reelId: String?,
    @Json(name = "customerName") val customerName: String,
    @Json(name = "customerPhone") val customerPhone: String,
    @Json(name = "intent") val intent: String,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class EnquiryResponse(
    @Json(name = "enquiryId") val enquiryId: String,
    @Json(name = "leadId") val leadId: String,
    @Json(name = "stage") val stage: String,
    @Json(name = "createdAt") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class UpdateLeadStageRequest(
    @Json(name = "stage") val stage: String,
    @Json(name = "notes") val notes: String?
)

@JsonClass(generateAdapter = true)
data class SendOtpRequest(
    @Json(name = "mobile") val mobile: String
)

@JsonClass(generateAdapter = true)
data class SendOtpResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "isSandbox") val isSandbox: Boolean = false
)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(
    @Json(name = "mobile") val mobile: String,
    @Json(name = "otp") val otp: String,
    @Json(name = "role") val role: String? = null,
    @Json(name = "fullName") val fullName: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String,
    @Json(name = "isNewUser") val isNewUser: Boolean = false
)

@JsonClass(generateAdapter = true)
data class UpdateRoleRequest(
    @Json(name = "role") val role: String,
    @Json(name = "businessDetails") val businessDetails: Map<String, Any>? = null
)

// --- PHASE 4: PRODUCTION-AUTHORITATIVE COIN ECONOMY API CONTRACTS ---

@JsonClass(generateAdapter = true)
data class NetworkCoinTransaction(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "amount") val amount: Int,
    @Json(name = "transaction_type") val transactionType: String,
    @Json(name = "category") val category: String,
    @Json(name = "description") val description: String,
    @Json(name = "reference_id") val referenceId: String?,
    @Json(name = "idempotency_key") val idempotencyKey: String,
    @Json(name = "timestamp") val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class CoinLedgerResponse(
    @Json(name = "authoritative_balance") val authoritativeBalance: Int,
    @Json(name = "transactions") val transactions: List<NetworkCoinTransaction>
)

@JsonClass(generateAdapter = true)
data class VerifyAndRewardReferralRequest(
    @Json(name = "referralId") val referralId: String,
    @Json(name = "referredUserId") val referredUserId: String,
    @Json(name = "playInstallReferrerToken") val playInstallReferrerToken: String? = null
)

@JsonClass(generateAdapter = true)
data class VerifyAndRewardReferralResponse(
    @Json(name = "referralId") val referralId: String,
    @Json(name = "status") val status: String,
    @Json(name = "referrerCreditedCoins") val referrerCreditedCoins: Int,
    @Json(name = "inviteeCreditedCoins") val inviteeCreditedCoins: Int
)

@JsonClass(generateAdapter = true)
data class RedeemCoinsRequest(
    @Json(name = "ruleId") val ruleId: String,
    @Json(name = "expectedCoinCost") val expectedCoinCost: Int
)

@JsonClass(generateAdapter = true)
data class RedeemCoinsResponse(
    @Json(name = "transactionId") val transactionId: String,
    @Json(name = "newAuthoritativeBalance") val newAuthoritativeBalance: Int,
    @Json(name = "ruleId") val ruleId: String
)

