package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sellerName: String,
    val sellerRole: String,
    val sellerPhone: String,
    val sellerBadge: String,
    val title: String,
    val description: String,
    val category: String,
    val price: Double,
    val priceDisplay: String,
    val isNegotiable: Boolean,
    val maintenanceCharge: Double,
    val city: String,
    val locality: String,
    val landmark: String,
    val bhk: Int?,
    val bathrooms: Int?,
    val carpetAreaSqft: Double,
    val floorNumber: Int?,
    val totalFloors: Int?,
    val furnishing: String,
    val possession: String,
    val reraId: String?,
    val amenitiesJson: String, // stored as comma-separated or json
    val imageUrlsJson: String,
    val linkedReelId: String?,
    val isFeatured: Boolean,
    val viewsCount: Int,
    val enquiriesCount: Int,
    val createdAt: Long
)

@Entity(tableName = "reels")
data class ReelEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val creatorName: String,
    val creatorBadge: String,
    val creatorRole: String,
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
    val isSponsored: Boolean,
    val likesCount: Int,
    val commentsCount: Int,
    val savesCount: Int,
    val sharesCount: Int,
    val isLikedByMe: Boolean,
    val isSavedByMe: Boolean,
    @ColumnInfo(defaultValue = "video")
    val mediaType: String = "video"
)

@Entity(tableName = "leads")
data class LeadEntity(
    @PrimaryKey val id: String,
    val enquiryId: String,
    val brokerId: String,
    val customerName: String,
    val customerPhone: String,
    val propertyId: String,
    val propertyTitle: String,
    val propertyPriceDisplay: String,
    val locality: String,
    val stage: String,
    val notes: String,
    val scheduledVisitDate: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "saved_properties")
data class SavedPropertyEntity(
    @PrimaryKey val propertyId: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "referral_codes",
    indices = [Index(value = ["userId"], unique = true)]
)
data class ReferralCodeEntity(
    @PrimaryKey val code: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(
    tableName = "referrals",
    indices = [
        Index(value = ["referredUserId"], unique = true),
        Index(value = ["referrerId"]),
        Index(value = ["referralCode"])
    ]
)
data class ReferralEntity(
    @PrimaryKey val id: String,
    val referrerId: String,
    val referredUserId: String,
    val referralCode: String,
    val attributionSource: String, // "WHATSAPP", "DIRECT_LINK", "COPY_LINK"
    val status: String, // "PENDING", "QUALIFIED", "REWARDED"
    val createdAt: Long = System.currentTimeMillis(),
    val qualifiedAt: Long? = null,
    val rewardedAt: Long? = null,
    val notes: String? = null
)

@Entity(
    tableName = "coin_ledger",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["referenceId"])
    ]
)
data class CoinLedgerEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Int,
    val transactionType: String, // "CREDIT" or "DEBIT"
    val category: String, // "REFERRAL_REWARD", "WELCOME_REWARD", "REDEMPTION"
    val description: String,
    val referenceId: String?,
    val idempotencyKey: String,
    val timestamp: Long = System.currentTimeMillis()
)

