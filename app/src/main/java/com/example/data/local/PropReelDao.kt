package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties ORDER BY createdAt DESC")
    fun getAllProperties(): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE id = :id LIMIT 1")
    suspend fun getPropertyById(id: String): PropertyEntity?

    @Query("SELECT * FROM properties WHERE city = :city ORDER BY createdAt DESC")
    fun getPropertiesByCity(city: String): Flow<List<PropertyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperties(properties: List<PropertyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: PropertyEntity)

    @Query("SELECT COUNT(*) FROM properties")
    suspend fun getCount(): Int

    @Query("DELETE FROM properties WHERE id = :id")
    suspend fun deletePropertyById(id: String)

    @Query("UPDATE properties SET linkedReelId = NULL WHERE linkedReelId = :reelId")
    suspend fun clearLinkedReel(reelId: String)
}

@Dao
interface ReelDao {
    @Query("SELECT * FROM reels ORDER BY id ASC")
    fun getAllReels(): Flow<List<ReelEntity>>

    @Query("SELECT * FROM reels WHERE city = :city ORDER BY id ASC")
    fun getReelsByCity(city: String): Flow<List<ReelEntity>>

    @Query("SELECT * FROM reels WHERE id = :id LIMIT 1")
    suspend fun getReelById(id: String): ReelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReels(reels: List<ReelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReel(reel: ReelEntity)

    @Update
    suspend fun updateReel(reel: ReelEntity)

    @Query("SELECT COUNT(*) FROM reels")
    suspend fun getCount(): Int

    @Query("DELETE FROM reels WHERE id LIKE 'reel_pune_%' OR id LIKE 'reel_mum_%'")
    suspend fun deleteLegacyReels()

    @Query("DELETE FROM reels WHERE id = :id")
    suspend fun deleteReelById(id: String)

    @Query("DELETE FROM reels WHERE id IN (:ids)")
    suspend fun deleteReelsByIds(ids: List<String>)

    @Query("UPDATE reels SET propertyId = NULL WHERE propertyId = :propertyId")
    suspend fun clearLinkedProperty(propertyId: String)
}

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads ORDER BY updatedAt DESC")
    fun getAllLeads(): Flow<List<LeadEntity>>

    @Query("SELECT * FROM leads WHERE stage = :stage ORDER BY updatedAt DESC")
    fun getLeadsByStage(stage: String): Flow<List<LeadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: LeadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeads(leads: List<LeadEntity>)

    @Query("UPDATE leads SET stage = :newStage, notes = :notes, updatedAt = :timestamp WHERE id = :leadId")
    suspend fun updateLeadStage(leadId: String, newStage: String, notes: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM leads")
    suspend fun getCount(): Int
}

@Dao
interface SavedPropertyDao {
    @Query("SELECT propertyId FROM saved_properties")
    fun getSavedPropertyIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProperty(item: SavedPropertyEntity)

    @Query("DELETE FROM saved_properties WHERE propertyId = :propertyId")
    suspend fun removeSavedProperty(propertyId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_properties WHERE propertyId = :propertyId)")
    suspend fun isPropertySaved(propertyId: String): Boolean
}

@Dao
interface ReferralDao {
    // --- Referral Codes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferralCode(code: ReferralCodeEntity)

    @Query("SELECT * FROM referral_codes WHERE userId = :userId LIMIT 1")
    fun getReferralCodeByUserId(userId: String): Flow<ReferralCodeEntity?>

    @Query("SELECT * FROM referral_codes WHERE userId = :userId LIMIT 1")
    suspend fun getReferralCodeByUserIdSync(userId: String): ReferralCodeEntity?

    @Query("SELECT * FROM referral_codes WHERE code = :code LIMIT 1")
    suspend fun getReferralCode(code: String): ReferralCodeEntity?

    // --- Referrals ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReferral(referral: ReferralEntity)

    @Update
    suspend fun updateReferral(referral: ReferralEntity)

    @Query("SELECT * FROM referrals WHERE referrerId = :referrerId ORDER BY createdAt DESC")
    fun getReferralsByReferrer(referrerId: String): Flow<List<ReferralEntity>>

    @Query("SELECT * FROM referrals WHERE referredUserId = :referredUserId LIMIT 1")
    suspend fun getReferralByReferredUser(referredUserId: String): ReferralEntity?

    @Query("SELECT * FROM referrals WHERE id = :id LIMIT 1")
    suspend fun getReferralById(id: String): ReferralEntity?

    @Query("SELECT COUNT(*) FROM referrals WHERE referrerId = :referrerId")
    fun getTotalReferralsCount(referrerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM referrals WHERE referrerId = :referrerId AND status = 'PENDING'")
    fun getPendingCount(referrerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM referrals WHERE referrerId = :referrerId AND status = 'QUALIFIED'")
    fun getQualifiedCount(referrerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM referrals WHERE referrerId = :referrerId AND status = 'REWARDED'")
    fun getRewardedCount(referrerId: String): Flow<Int>

    @Query("UPDATE referrals SET status = :newStatus, qualifiedAt = :qualifiedAt, notes = :notes WHERE id = :id")
    suspend fun updateQualification(id: String, newStatus: String, qualifiedAt: Long, notes: String?)

    @Query("UPDATE referrals SET status = :newStatus, rewardedAt = :rewardedAt, notes = :notes WHERE id = :id")
    suspend fun updateReward(id: String, newStatus: String, rewardedAt: Long, notes: String?)

    @Query("SELECT * FROM referrals WHERE referredUserId = :referredUserId LIMIT 1")
    fun getReferralByReferredUserStream(referredUserId: String): Flow<ReferralEntity?>
}

@Dao
interface CoinLedgerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(entry: CoinLedgerEntity)

    @Query("SELECT * FROM coin_ledger WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsByUser(userId: String): Flow<List<CoinLedgerEntity>>

    @Query("SELECT * FROM coin_ledger WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getTransactionsByUserSync(userId: String): List<CoinLedgerEntity>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN transactionType = 'CREDIT' THEN amount ELSE -amount END), 0)
        FROM coin_ledger
        WHERE userId = :userId
    """)
    fun getBalanceStream(userId: String): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN transactionType = 'CREDIT' THEN amount ELSE -amount END), 0)
        FROM coin_ledger
        WHERE userId = :userId
    """)
    suspend fun getAuthoritativeBalanceSync(userId: String): Int

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM coin_ledger
        WHERE userId = :userId AND transactionType = 'CREDIT' AND category = :category
    """)
    fun getCreditsByCategoryStream(userId: String, category: String): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM coin_ledger
        WHERE userId = :userId AND transactionType = 'DEBIT'
    """)
    fun getTotalDebitsStream(userId: String): Flow<Int>

    @Query("SELECT * FROM coin_ledger WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getByIdempotencyKey(key: String): CoinLedgerEntity?

    @Query("SELECT COUNT(*) FROM coin_ledger WHERE userId = :userId AND referenceId = :ruleId AND category = 'REDEMPTION' AND transactionType = 'DEBIT'")
    suspend fun countRedemptionsForRule(userId: String, ruleId: String): Int
}

