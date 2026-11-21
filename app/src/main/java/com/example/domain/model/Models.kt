package com.example.domain.model

enum class UserRole(val displayName: String) {
    CUSTOMER("Buyer / Tenant"),
    OWNER("Property Owner"),
    BROKER("Real Estate Agent"),
    BUILDER("Builder / Developer"),
    REAL_ESTATE_PRO("Property Professional"),
    ADMIN("Administrator")
}

enum class VerificationBadge(val label: String) {
    NONE("Unverified"),
    OWNER_VERIFIED("Verified Owner"),
    RERA_VERIFIED("RERA Registered"),
    BUILDER_PARTNER("Authorized Builder")
}

enum class PropertyCategory(val title: String, val shortCode: String) {
    RESIDENTIAL_BUY("Residential Buy", "Buy"),
    RESIDENTIAL_RENT("Residential Rent", "Rent"),
    COMMERCIAL_BUY("Commercial Buy", "Comm Buy"),
    COMMERCIAL_RENT("Commercial Rent", "Comm Rent"),
    PLOTS_LAND("Plots & Land", "Plots"),
    NEW_PROJECTS("New Projects", "Projects")
}

enum class FurnishingStatus(val label: String) {
    FURNISHED("Furnished"),
    SEMI_FURNISHED("Semi-Furnished"),
    UNFURNISHED("Unfurnished")
}

enum class PossessionStatus(val label: String) {
    READY_TO_MOVE("Ready to Move"),
    UNDER_CONSTRUCTION("Under Construction")
}

enum class LeadStage(val label: String, val stepNumber: Int) {
    NEW("New Enquiry", 1),
    CONTACTED("Contacted", 2),
    INTERESTED("Interested", 3),
    SITE_VISIT("Site Visit Scheduled", 4),
    NEGOTIATION("Negotiation", 5),
    CLOSED("Deal Closed", 6),
    NOT_RELEVANT("Not Relevant", 0)
}

enum class EnquiryIntent(val label: String) {
    CALLBACK("Request Callback"),
    SITE_VISIT("Schedule Site Visit"),
    BROCHURE("Request Digital Brochure"),
    BEST_OFFER("Get Best Price Offer")
}

enum class PromotionStatus(val label: String) {
    DRAFT("Draft"),
    PAYMENT_PENDING("Payment Pending"),
    PAID("Paid"),
    ACTIVE("Active / Live"),
    PAUSED("Paused"),
    COMPLETED("Completed")
}

data class User(
    val id: String,
    val mobile: String,
    val fullName: String,
    val role: UserRole,
    val avatarUrl: String,
    val city: String = "Pune",
    val isVerified: Boolean = false,
    val verificationBadge: VerificationBadge = VerificationBadge.NONE
)

data class BusinessProfile(
    val userId: String,
    val companyName: String,
    val reraNumber: String?,
    val experienceYears: Int,
    val operatingLocalities: List<String>,
    val bio: String,
    val totalListings: Int = 0,
    val activeLeadsCount: Int = 0
)

data class Property(
    val id: String,
    val userId: String,
    val sellerName: String,
    val sellerRole: UserRole,
    val sellerPhone: String,
    val sellerBadge: VerificationBadge,
    val title: String,
    val description: String,
    val category: PropertyCategory,
    val price: Double,
    val priceDisplay: String, // e.g., "₹78 Lakh" or "₹1.45 Cr"
    val isNegotiable: Boolean,
    val maintenanceCharge: Double = 0.0,
    val city: String,
    val locality: String,
    val landmark: String,
    val bhk: Int?,
    val bathrooms: Int?,
    val carpetAreaSqft: Double,
    val floorNumber: Int?,
    val totalFloors: Int?,
    val furnishing: FurnishingStatus,
    val possession: PossessionStatus,
    val reraId: String? = null,
    val amenities: List<String>,
    val imageUrls: List<String>,
    val linkedReelId: String? = null,
    val isFeatured: Boolean = false,
    val viewsCount: Int = 0,
    val enquiriesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Reel(
    val id: String,
    val userId: String,
    val creatorName: String,
    val creatorBadge: VerificationBadge,
    val creatorRole: UserRole,
    val propertyId: String?,
    val propertyTitle: String?,
    val propertyPriceDisplay: String?,
    val propertyLocality: String?,
    val propertyBhk: Int?,
    val caption: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val city: String,
    val locality: String,
    val isSponsored: Boolean = false,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val savesCount: Int = 0,
    val sharesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    val mediaType: String = "video"
)

data class Enquiry(
    val id: String,
    val propertyId: String,
    val propertyTitle: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val recipientId: String,
    val message: String,
    val intent: EnquiryIntent,
    val timestamp: Long = System.currentTimeMillis()
)

data class Lead(
    val id: String,
    val enquiryId: String,
    val brokerId: String,
    val customerName: String,
    val customerPhone: String,
    val propertyId: String,
    val propertyTitle: String,
    val propertyPriceDisplay: String,
    val locality: String,
    val stage: LeadStage,
    val notes: String = "",
    val scheduledVisitDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class SavedSearch(
    val id: String,
    val name: String,
    val city: String,
    val locality: String?,
    val category: PropertyCategory?,
    val minPrice: Double?,
    val maxPrice: Double?,
    val bhk: Int?
)

data class PromotionCampaign(
    val id: String,
    val title: String,
    val targetType: String, // REEL or PROPERTY
    val targetId: String,
    val budget: Double,
    val durationDays: Int,
    val estimatedReach: Int,
    val status: PromotionStatus,
    val impressionsDelivered: Int = 0,
    val clicksDelivered: Int = 0
)

// --- PHASE 3: REFER & EARN DOMAIN MODELS ---

enum class AttributionSource(val displayName: String) {
    WHATSAPP("WhatsApp"),
    DIRECT_LINK("Direct Link"),
    COPY_LINK("Copied Link")
}

enum class ReferralStatus(val displayName: String) {
    PENDING("Pending"),
    QUALIFIED("Qualified"),
    REWARDED("Rewarded")
}

data class ReferralCode(
    val code: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

data class Referral(
    val id: String,
    val referrerId: String,
    val referredUserId: String,
    val referralCode: String,
    val attributionSource: AttributionSource,
    val status: ReferralStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val qualifiedAt: Long? = null,
    val rewardedAt: Long? = null,
    val notes: String? = null
)

data class ReferralStats(
    val referralCode: String,
    val referralLink: String,
    val totalReferrals: Int,
    val pendingCount: Int,
    val qualifiedCount: Int,
    val rewardedCount: Int
)

sealed class ReferralApplyResult {
    data class Success(val referral: Referral) : ReferralApplyResult()
    data class Error(val message: String) : ReferralApplyResult()
}

/**
 * Reward listener hook prepared for Phase 4 Coin Economy integration.
 */
interface ReferralRewardListener {
    suspend fun onReferralQualified(referral: Referral)
    suspend fun onReferralRewarded(referral: Referral)
}

// --- PHASE 4: COIN ECONOMY DOMAIN MODELS ---

object CoinEconomyConfig {
    const val CURRENCY_CODE = "APP_COINS"
    const val CURRENCY_LABEL = "PropReel Coins"
    const val REFERRER_REWARD_COINS = 50
    const val INVITEE_WELCOME_COINS = 25
}

enum class CoinTransactionType(val displayName: String) {
    CREDIT("Credit"),
    DEBIT("Debit")
}

enum class CoinTransactionCategory(val displayName: String) {
    REFERRAL_REWARD("Referral Reward"),
    WELCOME_REWARD("Welcome Reward"),
    REDEMPTION("In-App Redemption")
}

data class CoinTransaction(
    val id: String,
    val userId: String,
    val amount: Int,
    val transactionType: CoinTransactionType,
    val category: CoinTransactionCategory,
    val description: String,
    val referenceId: String?,
    val idempotencyKey: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CoinWalletSummary(
    val authoritativeBalance: Int,
    val referralRewardsCoins: Int,
    val welcomeRewardsCoins: Int,
    val totalRedeemedCoins: Int,
    val pendingReferralRewardsCoins: Int,
    val qualifiedAwaitingCreditCoins: Int,
    val pendingWelcomeRewardCoins: Int,
    val qualifiedReferralsAwaitingId: String? = null,
    val isLocalPrototypeLedger: Boolean = true
) {
    val totalCreditedCoins: Int
        get() = referralRewardsCoins + welcomeRewardsCoins

    val totalPendingRewardsCoins: Int
        get() = pendingReferralRewardsCoins + qualifiedAwaitingCreditCoins + pendingWelcomeRewardCoins
}

/**
 * Configurable in-app redemption perk rule.
 * Strictly for in-app utility perks; carries zero cash conversion or monetary value.
 */
data class RedemptionRule(
    val id: String,
    val title: String,
    val description: String,
    val coinCost: Int,
    val minBalanceRequired: Int = coinCost,
    val maxRedemptionsPerUser: Int = 10,
    val categoryLabel: String = "In-App Perk",
    val isEnabled: Boolean = true
)

sealed class CoinOperationResult {
    data class Success(
        val transaction: CoinTransaction,
        val newBalance: Int,
        val message: String
    ) : CoinOperationResult()

    data class DuplicateRejected(
        val reason: String,
        val currentBalance: Int
    ) : CoinOperationResult()

    data class Error(
        val message: String,
        val currentBalance: Int = 0
    ) : CoinOperationResult()
}

sealed class CoinRewardProcessResult {
    data class Success(
        val referralId: String,
        val referrerTransaction: CoinTransaction,
        val inviteeTransaction: CoinTransaction,
        val referrerNewBalance: Int,
        val inviteeNewBalance: Int
    ) : CoinRewardProcessResult()

    data class AlreadyProcessed(
        val referralId: String,
        val reason: String
    ) : CoinRewardProcessResult()

    data class Rejected(
        val referralId: String,
        val reason: String
    ) : CoinRewardProcessResult()
}

