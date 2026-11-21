package com.example.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.example.data.local.CoinLedgerEntity
import com.example.data.local.LeadEntity
import com.example.data.local.PropReelDatabase
import com.example.data.local.PropertyEntity
import com.example.data.local.ReelEntity
import com.example.data.local.ReferralCodeEntity
import com.example.data.local.ReferralEntity
import com.example.data.local.SavedPropertyEntity
import com.example.data.remote.ApiClient
import com.example.data.remote.EnquiryRequest
import com.example.data.remote.PropReelApiService
import com.example.data.remote.RedeemCoinsRequest
import com.example.data.remote.SendOtpRequest
import com.example.data.remote.UpdateLeadStageRequest
import com.example.data.remote.UpdateRoleRequest
import com.example.data.remote.VerifyAndRewardReferralRequest
import com.example.data.remote.VerifyOtpRequest
import com.example.domain.model.AttributionSource
import com.example.domain.model.CoinEconomyConfig
import com.example.domain.model.CoinOperationResult
import com.example.domain.model.CoinRewardProcessResult
import com.example.domain.model.CoinTransaction
import com.example.domain.model.CoinTransactionCategory
import com.example.domain.model.CoinTransactionType
import com.example.domain.model.CoinWalletSummary
import com.example.domain.model.Enquiry
import com.example.domain.model.EnquiryIntent
import com.example.domain.model.FurnishingStatus
import com.example.domain.model.Lead
import com.example.domain.model.LeadStage
import com.example.domain.model.PossessionStatus
import com.example.domain.model.Property
import com.example.domain.model.PropertyCategory
import com.example.domain.model.RedemptionRule
import com.example.domain.model.Reel
import com.example.domain.model.Referral
import com.example.domain.model.ReferralApplyResult
import com.example.domain.model.ReferralCode
import com.example.domain.model.ReferralRewardListener
import com.example.domain.model.ReferralStats
import com.example.domain.model.ReferralStatus
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.model.VerificationBadge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class PropReelRepository(context: Context) {

    private val appContext = context.applicationContext
    private val seedPrefs = appContext.getSharedPreferences("propreel_seed_prefs", Context.MODE_PRIVATE)
    private val db = PropReelDatabase.getInstance(appContext)
    private val propertyDao = db.propertyDao()
    private val reelDao = db.reelDao()
    private val leadDao = db.leadDao()
    private val savedPropertyDao = db.savedPropertyDao()
    private val referralDao = db.referralDao()
    private val coinLedgerDao = db.coinLedgerDao()
    private val apiService: PropReelApiService = ApiClient.apiService

    companion object {
        private val seedMutex = Mutex()
        private val coinLedgerMutex = Mutex()
    }

    // Current active user simulation with easy role-switching for verification
    private val _currentUser = MutableStateFlow(
        User(
            id = "usr_pune_broker_01",
            mobile = "+91 98230 45678",
            fullName = "Vikram Deshmukh",
            role = UserRole.BROKER,
            avatarUrl = "https://images.unsplash.com/photo-1560250097-0b93528c311a?w=400&q=80",
            city = "Pune",
            isVerified = true,
            verificationBadge = VerificationBadge.RERA_VERIFIED
        )
    )
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val _selectedCity = MutableStateFlow("Pune")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private var currentAccessToken: String? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDatabaseIfEmpty()
        }
    }

    fun setCity(city: String) {
        _selectedCity.value = city
    }

    suspend fun sendOtp(mobile: String): Pair<Boolean, Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.sendOtp(SendOtpRequest(mobile))
            Pair(response.success, response.data?.isSandbox ?: true)
        } catch (_: Exception) {
            // Local fallback for offline/sandbox testing
            val isSandbox = mobile.endsWith("45678") || mobile.endsWith("23456") || mobile.endsWith("9999")
            Pair(true, isSandbox)
        }
    }

    suspend fun verifyOtp(mobile: String, otp: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.verifyOtp(VerifyOtpRequest(mobile = mobile, otp = otp))
            if (response.success && response.data != null) {
                currentAccessToken = response.data.accessToken
                _isLoggedIn.value = true
                return@withContext true
            }
        } catch (_: Exception) {
            // Graceful fallback for sandbox verification
        }

        if (otp == "999999") {
            _isLoggedIn.value = true
            return@withContext true
        }
        false
    }

    fun updateUserRoleWithDetails(role: UserRole, reraNumber: String?, agencyName: String?) {
        val current = _currentUser.value
        val badge = when (role) {
            UserRole.BROKER -> VerificationBadge.RERA_VERIFIED
            UserRole.OWNER -> VerificationBadge.OWNER_VERIFIED
            UserRole.BUILDER -> VerificationBadge.BUILDER_PARTNER
            else -> VerificationBadge.NONE
        }

        _currentUser.value = current.copy(
            role = role,
            verificationBadge = badge,
            fullName = if (agencyName != null && role == UserRole.BROKER) "$agencyName (${current.fullName})" else current.fullName
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.updateRole(
                    UpdateRoleRequest(
                        role = role.name,
                        businessDetails = mapOf(
                            "companyName" to (agencyName ?: ""),
                            "reraNumber" to (reraNumber ?: "")
                        )
                    )
                )
            } catch (_: Exception) {
                // Offline fallback
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        currentAccessToken = null
    }

    fun switchRole(role: UserRole) {
        updateUserRoleWithDetails(role, null, null)
    }

    // --- REELS ---
    fun getReelsStream(): Flow<List<Reel>> {
        return reelDao.getAllReels().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun toggleLikeReel(reelId: String) = withContext(Dispatchers.IO) {
        val entity = reelDao.getReelById(reelId) ?: return@withContext
        val newLiked = !entity.isLikedByMe
        val newCount = if (newLiked) entity.likesCount + 1 else (entity.likesCount - 1).coerceAtLeast(0)
        reelDao.updateReel(entity.copy(isLikedByMe = newLiked, likesCount = newCount))
    }

    suspend fun toggleSaveReel(reelId: String) = withContext(Dispatchers.IO) {
        val entity = reelDao.getReelById(reelId) ?: return@withContext
        val newSaved = !entity.isSavedByMe
        val newCount = if (newSaved) entity.savesCount + 1 else (entity.savesCount - 1).coerceAtLeast(0)
        reelDao.updateReel(entity.copy(isSavedByMe = newSaved, savesCount = newCount))
    }

    suspend fun createReel(reel: Reel) = withContext(Dispatchers.IO) {
        reelDao.insertReel(reel.toEntity())
    }

    suspend fun deleteReel(reelId: String) = withContext(Dispatchers.IO) {
        val existing = reelDao.getReelById(reelId) ?: return@withContext
        db.withTransaction {
            propertyDao.clearLinkedReel(reelId)
            reelDao.deleteReelById(reelId)
        }
        deleteOwnedLocalMediaIfApplicable(existing.videoUrl)
        deleteOwnedLocalMediaIfApplicable(existing.thumbnailUrl)
    }

    private fun deleteOwnedLocalMediaIfApplicable(uriString: String) {
        if (!uriString.startsWith("file://")) return
        try {
            val filePath = android.net.Uri.parse(uriString).path ?: return
            val file = java.io.File(filePath)
            val uploadsDir = java.io.File(appContext.filesDir, "uploaded_reels")
            if (file.exists() && file.canonicalPath.startsWith(uploadsDir.canonicalPath)) {
                file.delete()
            }
        } catch (_: Exception) {
            // Best-effort cleanup
        }
    }

    // --- PROPERTIES ---
    fun getPropertiesStream(
        categoryFilter: PropertyCategory? = null,
        localityFilter: String? = null,
        maxPriceFilter: Double? = null,
        bhkFilter: Int? = null,
        verifiedOnly: Boolean = false
    ): Flow<List<Property>> {
        return propertyDao.getAllProperties().map { entities ->
            entities
                .map { it.toDomain() }
                .filter { property ->
                    (categoryFilter == null || property.category == categoryFilter) &&
                    (localityFilter == null || property.locality.equals(localityFilter, ignoreCase = true)) &&
                    (maxPriceFilter == null || property.price <= maxPriceFilter) &&
                    (bhkFilter == null || property.bhk == bhkFilter) &&
                    (!verifiedOnly || property.sellerBadge != VerificationBadge.NONE)
                }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getPropertyById(propertyId: String): Property? = withContext(Dispatchers.IO) {
        propertyDao.getPropertyById(propertyId)?.toDomain()
    }

    suspend fun createProperty(property: Property) = withContext(Dispatchers.IO) {
        propertyDao.insertProperty(property.toEntity())
    }

    suspend fun deleteProperty(propertyId: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            savedPropertyDao.removeSavedProperty(propertyId)
            reelDao.clearLinkedProperty(propertyId)
            propertyDao.deletePropertyById(propertyId)
        }
    }

    // --- SAVED PROPERTIES ---
    fun getSavedPropertiesStream(): Flow<List<Property>> {
        return combine(
            propertyDao.getAllProperties(),
            savedPropertyDao.getSavedPropertyIds()
        ) { properties, savedIds ->
            val set = savedIds.toSet()
            properties.filter { set.contains(it.id) }.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun toggleSaveProperty(propertyId: String): Boolean = withContext(Dispatchers.IO) {
        val isSaved = savedPropertyDao.isPropertySaved(propertyId)
        if (isSaved) {
            savedPropertyDao.removeSavedProperty(propertyId)
            false
        } else {
            savedPropertyDao.saveProperty(SavedPropertyEntity(propertyId))
            true
        }
    }

    fun isPropertySavedStream(propertyId: String): Flow<Boolean> {
        return savedPropertyDao.getSavedPropertyIds().map { it.contains(propertyId) }
    }

    // --- LEADS & ENQUIRIES ---
    fun getLeadsStream(stageFilter: LeadStage? = null): Flow<List<Lead>> {
        return leadDao.getAllLeads().map { entities ->
            entities
                .map { it.toDomain() }
                .filter { stageFilter == null || it.stage == stageFilter }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun submitEnquiry(
        property: Property,
        customerName: String,
        customerPhone: String,
        message: String,
        intent: EnquiryIntent
    ): String = withContext(Dispatchers.IO) {
        val enquiryId = "enq_" + UUID.randomUUID().toString().take(8)
        val leadId = "lead_" + UUID.randomUUID().toString().take(8)

        val newLead = LeadEntity(
            id = leadId,
            enquiryId = enquiryId,
            brokerId = property.userId,
            customerName = customerName,
            customerPhone = customerPhone,
            propertyId = property.id,
            propertyTitle = property.title,
            propertyPriceDisplay = property.priceDisplay,
            locality = property.locality,
            stage = LeadStage.NEW.name,
            notes = "Customer Intent: ${intent.label}. Message: $message",
            scheduledVisitDate = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        leadDao.insertLead(newLead)

        // Increment property enquiry count in local database
        val currentEntity = propertyDao.getPropertyById(property.id)
        if (currentEntity != null) {
            propertyDao.insertProperty(
                currentEntity.copy(enquiriesCount = currentEntity.enquiriesCount + 1)
            )
        }

        // Fire-and-forget asynchronous remote sync to backend server
        try {
            apiService.submitEnquiry(
                EnquiryRequest(
                    propertyId = property.id,
                    reelId = property.linkedReelId,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    intent = intent.name,
                    message = message
                )
            )
        } catch (_: Exception) {
            // Graceful fallback to offline local Room persistence
        }

        leadId
    }

    suspend fun updateLeadStage(leadId: String, newStage: LeadStage, notes: String) = withContext(Dispatchers.IO) {
        leadDao.updateLeadStage(leadId, newStage.name, notes, System.currentTimeMillis())

        try {
            apiService.updateLeadStage(
                leadId = leadId,
                request = UpdateLeadStageRequest(stage = newStage.name, notes = notes)
            )
        } catch (_: Exception) {
            // Graceful offline fallback
        }
    }

    // --- REFER & EARN (PHASE 3) ---

    // Configurable attribution window in days (default 30 days)
    var attributionWindowDays: Long = 30L
    val attributionWindowMs: Long
        get() = attributionWindowDays * 24 * 60 * 60 * 1000L

    private var rewardListener: ReferralRewardListener? = null

    fun setReferralRewardListener(listener: ReferralRewardListener) {
        rewardListener = listener
    }

    fun getReferralLink(code: String): String = "https://propreel.in/refer/$code"

    fun generateReferralCodeForUser(userId: String): String {
        val clean = userId.replace("[^A-Za-z0-9]".toRegex(), "").uppercase()
        val suffix = if (clean.length >= 4) clean.takeLast(4) else clean.padEnd(4, '8')
        return "PROP$suffix"
    }

    fun getReferralCodeStream(userId: String): Flow<ReferralCode> {
        return referralDao.getReferralCodeByUserId(userId).map { entity ->
            if (entity != null) {
                entity.toDomain()
            } else {
                val code = generateReferralCodeForUser(userId)
                val newEntity = ReferralCodeEntity(code = code, userId = userId)
                referralDao.insertReferralCode(newEntity)
                newEntity.toDomain()
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getOrCreateReferralCode(userId: String): ReferralCode = withContext(Dispatchers.IO) {
        val existing = referralDao.getReferralCodeByUserIdSync(userId)
        if (existing != null) {
            existing.toDomain()
        } else {
            val code = generateReferralCodeForUser(userId)
            val newEntity = ReferralCodeEntity(code = code, userId = userId)
            referralDao.insertReferralCode(newEntity)
            newEntity.toDomain()
        }
    }

    fun getReferralsStream(userId: String): Flow<List<Referral>> {
        return referralDao.getReferralsByReferrer(userId).map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    fun getReferralStatsStream(userId: String): Flow<ReferralStats> {
        return combine(
            getReferralCodeStream(userId),
            referralDao.getTotalReferralsCount(userId),
            referralDao.getPendingCount(userId),
            referralDao.getQualifiedCount(userId),
            referralDao.getRewardedCount(userId)
        ) { refCode, total, pending, qualified, rewarded ->
            ReferralStats(
                referralCode = refCode.code,
                referralLink = getReferralLink(refCode.code),
                totalReferrals = total,
                pendingCount = pending,
                qualifiedCount = qualified,
                rewardedCount = rewarded
            )
        }.flowOn(Dispatchers.IO)
    }

    suspend fun applyReferralCode(
        referredUserId: String,
        code: String,
        source: AttributionSource,
        notes: String? = null
    ): ReferralApplyResult = withContext(Dispatchers.IO) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) {
            return@withContext ReferralApplyResult.Error("Referral code cannot be blank")
        }

        // 1. Validate referral code exists
        val codeEntity = referralDao.getReferralCode(cleanCode)
            ?: return@withContext ReferralApplyResult.Error("Invalid referral code: '$cleanCode' not found")

        if (!codeEntity.isActive) {
            return@withContext ReferralApplyResult.Error("This referral code is no longer active")
        }

        val referrerId = codeEntity.userId

        // 2. Prevent self-referral
        if (referrerId == referredUserId) {
            return@withContext ReferralApplyResult.Error("Self-referral is strictly prohibited. You cannot use your own referral code.")
        }

        // 3. Prevent duplicate attribution (user already referred)
        val existingReferral = referralDao.getReferralByReferredUser(referredUserId)
        if (existingReferral != null) {
            return@withContext ReferralApplyResult.Error("User has already applied referral code '${existingReferral.referralCode}'")
        }

        // 4. Create and persist referral record in PENDING state
        val referralId = "ref_" + UUID.randomUUID().toString().take(8)
        val referralEntity = ReferralEntity(
            id = referralId,
            referrerId = referrerId,
            referredUserId = referredUserId,
            referralCode = cleanCode,
            attributionSource = source.name,
            status = ReferralStatus.PENDING.name,
            createdAt = System.currentTimeMillis(),
            qualifiedAt = null,
            rewardedAt = null,
            notes = notes ?: "Attributed via ${source.displayName} (Demo / Device-Local Mode)"
        )

        try {
            referralDao.insertReferral(referralEntity)
            ReferralApplyResult.Success(referralEntity.toDomain())
        } catch (e: Exception) {
            ReferralApplyResult.Error("Failed to record referral: ${e.message}")
        }
    }

    suspend fun qualifyReferral(
        referredUserId: String,
        qualificationNote: String = "User qualified by completing property interaction"
    ): Boolean = withContext(Dispatchers.IO) {
        val referral = referralDao.getReferralByReferredUser(referredUserId) ?: return@withContext false

        // Check if already qualified or rewarded
        if (referral.status != ReferralStatus.PENDING.name) {
            return@withContext false // Prevent duplicate qualification
        }

        // Check attribution window expiration
        val now = System.currentTimeMillis()
        if (now - referral.createdAt > attributionWindowMs) {
            val expiredNote = (referral.notes ?: "") + " | Attribution window expired (${attributionWindowDays} days exceeded)"
            referralDao.updateReferral(referral.copy(notes = expiredNote))
            return@withContext false
        }

        val updatedNotes = (referral.notes ?: "") + " | Qualified: $qualificationNote"
        referralDao.updateQualification(
            id = referral.id,
            newStatus = ReferralStatus.QUALIFIED.name,
            qualifiedAt = now,
            notes = updatedNotes
        )

        val qualifiedDomain = referral.copy(
            status = ReferralStatus.QUALIFIED.name,
            qualifiedAt = now,
            notes = updatedNotes
        ).toDomain()

        // Notify listener (prepared for Phase 4 Coin Economy integration)
        rewardListener?.onReferralQualified(qualifiedDomain)
        true
    }

    suspend fun markReferralRewarded(referralId: String, rewardNotes: String): Boolean {
        val result = processReferralReward(referralId = referralId, rewardNotes = rewardNotes)
        return result is CoinRewardProcessResult.Success
    }

    // --- COIN ECONOMY ENGINE (PHASE 4 - LOCAL PROTOTYPE LEDGER) ---
    // NOTE: In production, coin issuance, referral verification, and redemptions MUST be
    // executed by the authoritative backend server. Device-local Room persistence cannot
    // prevent tampering on rooted/compromised devices.

    private val _redemptionRules = MutableStateFlow(
        listOf(
            RedemptionRule(
                id = "rule_lead_alert_pass",
                title = "Priority Buyer Lead Pass",
                description = "Receive instant priority alert routing for your next incoming property enquiry.",
                coinCost = 25,
                minBalanceRequired = 25,
                maxRedemptionsPerUser = 20,
                categoryLabel = "CRM Perk"
            ),
            RedemptionRule(
                id = "rule_reel_spotlight_24h",
                title = "24h Reel Discovery Spotlight",
                description = "Feature one property reel in your city's top discovery queue for 24 hours.",
                coinCost = 50,
                minBalanceRequired = 50,
                maxRedemptionsPerUser = 10,
                categoryLabel = "Discovery Boost"
            ),
            RedemptionRule(
                id = "rule_featured_tag_7d",
                title = "Featured Listing Badge (7 Days)",
                description = "Highlight one verified property listing with a Featured Tag in Search.",
                coinCost = 100,
                minBalanceRequired = 100,
                maxRedemptionsPerUser = 5,
                categoryLabel = "Listing Perk"
            )
        )
    )
    val redemptionRules: StateFlow<List<RedemptionRule>> = _redemptionRules.asStateFlow()

    fun configureRedemptionRules(rules: List<RedemptionRule>) {
        _redemptionRules.value = rules
    }

    suspend fun getAuthoritativeCoinBalance(userId: String = _currentUser.value.id): Int =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                coinLedgerDao.getAuthoritativeBalanceSync(userId)
            }
        }

    fun getCoinTransactionsStream(userId: String): Flow<List<CoinTransaction>> {
        return coinLedgerDao.getTransactionsByUser(userId).map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    fun getCoinWalletSummaryStream(userId: String): Flow<CoinWalletSummary> {
        val baseBalances = combine(
            coinLedgerDao.getBalanceStream(userId),
            coinLedgerDao.getCreditsByCategoryStream(userId, CoinTransactionCategory.REFERRAL_REWARD.name),
            coinLedgerDao.getCreditsByCategoryStream(userId, CoinTransactionCategory.WELCOME_REWARD.name),
            coinLedgerDao.getTotalDebitsStream(userId)
        ) { balance, referralCoins, welcomeCoins, debits ->
            listOf(balance, referralCoins, welcomeCoins, debits)
        }

        return combine(
            baseBalances,
            referralDao.getReferralsByReferrer(userId),
            referralDao.getReferralByReferredUserStream(userId)
        ) { balances, outgoingReferrals, incomingReferral ->
            val pendingCount = outgoingReferrals.count { it.status == ReferralStatus.PENDING.name }
            val qualifiedUnrewarded = outgoingReferrals.filter { it.status == ReferralStatus.QUALIFIED.name }
            val pendingWelcomeCoins = if (
                incomingReferral != null &&
                (incomingReferral.status == ReferralStatus.PENDING.name ||
                    incomingReferral.status == ReferralStatus.QUALIFIED.name)
            ) {
                CoinEconomyConfig.INVITEE_WELCOME_COINS
            } else {
                0
            }

            CoinWalletSummary(
                authoritativeBalance = balances[0],
                referralRewardsCoins = balances[1],
                welcomeRewardsCoins = balances[2],
                totalRedeemedCoins = balances[3],
                pendingReferralRewardsCoins = pendingCount * CoinEconomyConfig.REFERRER_REWARD_COINS,
                qualifiedAwaitingCreditCoins = qualifiedUnrewarded.size * CoinEconomyConfig.REFERRER_REWARD_COINS,
                pendingWelcomeRewardCoins = pendingWelcomeCoins,
                qualifiedReferralsAwaitingId = qualifiedUnrewarded.firstOrNull()?.id,
                isLocalPrototypeLedger = true
            )
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Atomically transitions a referral from QUALIFIED -> REWARDED and credits:
     * - 50 APP_COINS (REFERRAL_REWARD) to the referrer
     * - 25 APP_COINS (WELCOME_REWARD) to the referred user
     * Uses a Kotlin Mutex + Room DB transaction + unique idempotency keys to guarantee
     * no duplicate or partial rewards can occur even under concurrent invocations.
     */
    suspend fun processReferralReward(
        referralId: String,
        rewardNotes: String = "Phase 4 Coin Reward Credited (Local Prototype)"
    ): CoinRewardProcessResult = withContext(Dispatchers.IO) {
        var callbackReferral: Referral? = null

        val result = coinLedgerMutex.withLock {
            db.withTransaction {
                val referral = referralDao.getReferralById(referralId)
                    ?: return@withTransaction CoinRewardProcessResult.Rejected(
                        referralId = referralId,
                        reason = "Referral record not found"
                    )

                if (referral.status == ReferralStatus.REWARDED.name) {
                    return@withTransaction CoinRewardProcessResult.AlreadyProcessed(
                        referralId = referralId,
                        reason = "Referral rewards already credited"
                    )
                }

                if (referral.status != ReferralStatus.QUALIFIED.name) {
                    return@withTransaction CoinRewardProcessResult.Rejected(
                        referralId = referralId,
                        reason = "Referral must be QUALIFIED before rewards can be issued (current: ${referral.status})"
                    )
                }

                val referrerKey = "reward_referrer_${referral.id}"
                val inviteeKey = "reward_invitee_${referral.id}"

                if (coinLedgerDao.getByIdempotencyKey(referrerKey) != null ||
                    coinLedgerDao.getByIdempotencyKey(inviteeKey) != null
                ) {
                    return@withTransaction CoinRewardProcessResult.AlreadyProcessed(
                        referralId = referralId,
                        reason = "Duplicate idempotency key detected for referral ${referral.id}"
                    )
                }

                val now = System.currentTimeMillis()
                val referrerTx = CoinLedgerEntity(
                    id = "tx_" + UUID.randomUUID().toString().take(12),
                    userId = referral.referrerId,
                    amount = CoinEconomyConfig.REFERRER_REWARD_COINS,
                    transactionType = CoinTransactionType.CREDIT.name,
                    category = CoinTransactionCategory.REFERRAL_REWARD.name,
                    description = "Referral reward (+${CoinEconomyConfig.REFERRER_REWARD_COINS} ${CoinEconomyConfig.CURRENCY_CODE}) for inviting ${referral.referredUserId.takeLast(8)}",
                    referenceId = referral.id,
                    idempotencyKey = referrerKey,
                    timestamp = now
                )

                val inviteeTx = CoinLedgerEntity(
                    id = "tx_" + UUID.randomUUID().toString().take(12),
                    userId = referral.referredUserId,
                    amount = CoinEconomyConfig.INVITEE_WELCOME_COINS,
                    transactionType = CoinTransactionType.CREDIT.name,
                    category = CoinTransactionCategory.WELCOME_REWARD.name,
                    description = "Welcome bonus (+${CoinEconomyConfig.INVITEE_WELCOME_COINS} ${CoinEconomyConfig.CURRENCY_CODE}) for referral code ${referral.referralCode}",
                    referenceId = referral.id,
                    idempotencyKey = inviteeKey,
                    timestamp = now + 1L
                )

                coinLedgerDao.insertTransaction(referrerTx)
                coinLedgerDao.insertTransaction(inviteeTx)

                val updatedNotes = (referral.notes ?: "") + " | Rewarded: $rewardNotes"
                referralDao.updateReward(
                    id = referral.id,
                    newStatus = ReferralStatus.REWARDED.name,
                    rewardedAt = now,
                    notes = updatedNotes
                )

                callbackReferral = referral.copy(
                    status = ReferralStatus.REWARDED.name,
                    rewardedAt = now,
                    notes = updatedNotes
                ).toDomain()

                val referrerBalance = coinLedgerDao.getAuthoritativeBalanceSync(referral.referrerId)
                val inviteeBalance = coinLedgerDao.getAuthoritativeBalanceSync(referral.referredUserId)

                CoinRewardProcessResult.Success(
                    referralId = referral.id,
                    referrerTransaction = referrerTx.toDomain(),
                    inviteeTransaction = inviteeTx.toDomain(),
                    referrerNewBalance = referrerBalance,
                    inviteeNewBalance = inviteeBalance
                )
            }
        }

        if (result is CoinRewardProcessResult.Success && callbackReferral != null) {
            rewardListener?.onReferralRewarded(callbackReferral!!)
            try {
                apiService.verifyAndRewardReferral(
                    referralId = referralId,
                    idempotencyKey = "reward_api_$referralId",
                    request = VerifyAndRewardReferralRequest(
                        referralId = referralId,
                        referredUserId = callbackReferral!!.referredUserId
                    )
                )
            } catch (_: Exception) {
                // Offline prototype fallback
            }
        }

        result
    }

    /**
     * Qualifies a PENDING referral (validating attribution window) and atomically credits
     * 50 APP_COINS to the referrer + 25 APP_COINS to the referred user.
     */
    suspend fun qualifyAndRewardReferral(
        referredUserId: String,
        qualificationNote: String = "Verified property engagement completed"
    ): CoinRewardProcessResult = withContext(Dispatchers.IO) {
        val referral = referralDao.getReferralByReferredUser(referredUserId)
            ?: return@withContext CoinRewardProcessResult.Rejected(
                referralId = "unknown",
                reason = "No referral found for user $referredUserId"
            )

        if (referral.status == ReferralStatus.REWARDED.name) {
            return@withContext CoinRewardProcessResult.AlreadyProcessed(
                referralId = referral.id,
                reason = "Referral already qualified and rewarded"
            )
        }

        if (referral.status == ReferralStatus.PENDING.name) {
            val qualified = qualifyReferral(referredUserId, qualificationNote)
            if (!qualified) {
                return@withContext CoinRewardProcessResult.Rejected(
                    referralId = referral.id,
                    reason = "Referral could not be qualified (attribution window expired or invalid state)"
                )
            }
        }

        processReferralReward(
            referralId = referral.id,
            rewardNotes = "Auto-credited on qualification: $qualificationNote"
        )
    }

    /**
     * Inserts a ledger transaction idempotently. Rejects duplicate idempotency keys
     * using Room's unique index and transactional checks.
     */
    suspend fun creditCoinsIdempotent(
        userId: String,
        amount: Int,
        category: CoinTransactionCategory,
        description: String,
        referenceId: String?,
        idempotencyKey: String
    ): CoinOperationResult = withContext(Dispatchers.IO) {
        val cleanKey = idempotencyKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext CoinOperationResult.Error("Idempotency key cannot be blank")
        }
        if (amount <= 0) {
            return@withContext CoinOperationResult.Error("Credit amount must be positive")
        }

        coinLedgerMutex.withLock {
            try {
                db.withTransaction {
                    val existing = coinLedgerDao.getByIdempotencyKey(cleanKey)
                    if (existing != null) {
                        val currentBalance = coinLedgerDao.getAuthoritativeBalanceSync(userId)
                        return@withTransaction CoinOperationResult.DuplicateRejected(
                            reason = "Duplicate idempotency key '$cleanKey' rejected",
                            currentBalance = currentBalance
                        )
                    }

                    val entity = CoinLedgerEntity(
                        id = "tx_" + UUID.randomUUID().toString().take(12),
                        userId = userId,
                        amount = amount,
                        transactionType = CoinTransactionType.CREDIT.name,
                        category = category.name,
                        description = description,
                        referenceId = referenceId,
                        idempotencyKey = cleanKey,
                        timestamp = System.currentTimeMillis()
                    )
                    coinLedgerDao.insertTransaction(entity)
                    val newBalance = coinLedgerDao.getAuthoritativeBalanceSync(userId)

                    CoinOperationResult.Success(
                        transaction = entity.toDomain(),
                        newBalance = newBalance,
                        message = "Credited +$amount ${CoinEconomyConfig.CURRENCY_CODE}"
                    )
                }
            } catch (e: SQLiteConstraintException) {
                val currentBalance = coinLedgerDao.getAuthoritativeBalanceSync(userId)
                CoinOperationResult.DuplicateRejected(
                    reason = "Unique idempotency constraint rejected key '$cleanKey'",
                    currentBalance = currentBalance
                )
            }
        }
    }

    /**
     * Redeems coins against a configurable in-app RedemptionRule as an atomic DEBIT
     * ledger transaction. Prevents negative balances, insufficient balances, and duplicate requests.
     */
    suspend fun redeemCoins(
        userId: String = _currentUser.value.id,
        ruleId: String,
        idempotencyKey: String = "redeem_${userId}_${ruleId}_${UUID.randomUUID().toString().take(8)}"
    ): CoinOperationResult = withContext(Dispatchers.IO) {
        val cleanKey = idempotencyKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext CoinOperationResult.Error("Redemption idempotency key cannot be blank")
        }

        val rule = _redemptionRules.value.find { it.id == ruleId }
            ?: return@withContext CoinOperationResult.Error("Selected redemption perk does not exist")

        if (!rule.isEnabled) {
            return@withContext CoinOperationResult.Error("This redemption perk is currently disabled")
        }

        if (rule.coinCost <= 0) {
            return@withContext CoinOperationResult.Error("Invalid redemption coin cost")
        }

        val result = coinLedgerMutex.withLock {
            try {
                db.withTransaction {
                    val existing = coinLedgerDao.getByIdempotencyKey(cleanKey)
                    val currentBalance = coinLedgerDao.getAuthoritativeBalanceSync(userId)
                    if (existing != null) {
                        return@withTransaction CoinOperationResult.DuplicateRejected(
                            reason = "Duplicate redemption request '$cleanKey' rejected",
                            currentBalance = currentBalance
                        )
                    }

                    val existingCount = coinLedgerDao.countRedemptionsForRule(userId, rule.id)
                    if (existingCount >= rule.maxRedemptionsPerUser) {
                        return@withTransaction CoinOperationResult.Error(
                            message = "Redemption limit reached (${rule.maxRedemptionsPerUser} max) for ${rule.title}",
                            currentBalance = currentBalance
                        )
                    }

                    if (currentBalance < rule.minBalanceRequired ||
                        currentBalance < rule.coinCost ||
                        (currentBalance - rule.coinCost) < 0
                    ) {
                        return@withTransaction CoinOperationResult.Error(
                            message = "Insufficient balance. Need ${rule.coinCost} ${CoinEconomyConfig.CURRENCY_CODE}, available $currentBalance.",
                            currentBalance = currentBalance
                        )
                    }

                    val debitEntity = CoinLedgerEntity(
                        id = "tx_" + UUID.randomUUID().toString().take(12),
                        userId = userId,
                        amount = rule.coinCost,
                        transactionType = CoinTransactionType.DEBIT.name,
                        category = CoinTransactionCategory.REDEMPTION.name,
                        description = "Redeemed: ${rule.title} (-${rule.coinCost} ${CoinEconomyConfig.CURRENCY_CODE})",
                        referenceId = rule.id,
                        idempotencyKey = cleanKey,
                        timestamp = System.currentTimeMillis()
                    )
                    coinLedgerDao.insertTransaction(debitEntity)

                    val newBalance = coinLedgerDao.getAuthoritativeBalanceSync(userId)
                    if (newBalance < 0) {
                        throw IllegalStateException("Ledger invariant violation: negative balance ($newBalance)")
                    }

                    CoinOperationResult.Success(
                        transaction = debitEntity.toDomain(),
                        newBalance = newBalance,
                        message = "${rule.title} activated! (-${rule.coinCost} ${CoinEconomyConfig.CURRENCY_CODE})"
                    )
                }
            } catch (e: SQLiteConstraintException) {
                val currentBalance = coinLedgerDao.getAuthoritativeBalanceSync(userId)
                CoinOperationResult.DuplicateRejected(
                    reason = "Duplicate redemption key rejected by ledger constraint",
                    currentBalance = currentBalance
                )
            }
        }

        if (result is CoinOperationResult.Success) {
            try {
                apiService.redeemCoins(
                    idempotencyKey = cleanKey,
                    request = RedeemCoinsRequest(ruleId = rule.id, expectedCoinCost = rule.coinCost)
                )
            } catch (_: Exception) {
                // Offline prototype fallback
            }
        }

        result
    }

    // --- SEED DATA ENGINE ---
    suspend fun seedInitialDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        seedMutex.withLock {
            seedInitialDatabaseIfEmptyInternal()
        }
    }

    private suspend fun seedInitialDatabaseIfEmptyInternal() {
        // One-time cleanup ensuring removed placeholder reels (1st & 3rd) are purged on existing installs too
        if (!seedPrefs.getBoolean("removed_v1_v3_reels_v5", false)) {
            db.withTransaction {
                propertyDao.clearLinkedReel("reel_01_video")
                propertyDao.clearLinkedReel("reel_03_video")
                reelDao.deleteReelsByIds(listOf("reel_01_video", "reel_03_video"))
            }
            seedPrefs.edit().putBoolean("removed_v1_v3_reels_v5", true).apply()
        }

        val hasMumbaiProp = propertyDao.getPropertyById("prop_mum_01") != null
        val hasReferralCode = referralDao.getReferralCode("PROP9823") != null
        val alreadySeededCatalog = seedPrefs.getBoolean("seeded_initial_catalog_v5", false)
        if (alreadySeededCatalog || (propertyDao.getCount() > 0 && hasMumbaiProp && hasReferralCode)) {
            seedPrefs.edit().putBoolean("seeded_initial_catalog_v5", true).apply()
            seedBaner4BhkReelIfMissing()
            seedDemoCoinEconomyIfMissing()
            return
        }

        val initialProperties = listOf(
            PropertyEntity(
                id = "prop_pune_01",
                userId = "usr_pune_broker_01",
                sellerName = "Vikram Deshmukh",
                sellerRole = UserRole.BROKER.name,
                sellerPhone = "+91 98230 45678",
                sellerBadge = VerificationBadge.RERA_VERIFIED.name,
                title = "VTP Earth One — Luxury 3 BHK with Panoramic Valley View",
                description = "Magnificent 3 BHK in prime Mahalunge-Baner link road. Features imported marble flooring, acoustic sliding glass windows, modular kitchen with chimney, home automation, and 2 designated covered parking slots. Resort-style clubhouse, infinity swimming pool, squash court, and reflexology garden.",
                category = PropertyCategory.RESIDENTIAL_BUY.name,
                price = 14500000.0,
                priceDisplay = "₹1.45 Cr",
                isNegotiable = true,
                maintenanceCharge = 4500.0,
                city = "Pune",
                locality = "Baner",
                landmark = "Near High Street & Expressway",
                bhk = 3,
                bathrooms = 3,
                carpetAreaSqft = 1150.0,
                floorNumber = 16,
                totalFloors = 32,
                furnishing = FurnishingStatus.SEMI_FURNISHED.name,
                possession = PossessionStatus.READY_TO_MOVE.name,
                reraId = "P52100029892",
                amenitiesJson = "Lift,Security,Power Backup,Gym,Swimming Pool,Covered Parking,Clubhouse,Jogging Track",
                imageUrlsJson = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=80,https://images.unsplash.com/photo-1600566753376-12c8ab7fb75b?w=1000&q=80,https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1000&q=80",
                linkedReelId = null,
                isFeatured = true,
                viewsCount = 1840,
                enquiriesCount = 28,
                createdAt = System.currentTimeMillis() - 86400000L * 2
            ),
            PropertyEntity(
                id = "prop_pune_02",
                userId = "usr_pune_owner_02",
                sellerName = "Priya Kulkarni",
                sellerRole = UserRole.OWNER.name,
                sellerPhone = "+91 98901 23456",
                sellerBadge = VerificationBadge.OWNER_VERIFIED.name,
                title = "Direct Owner: Elegant 2 BHK in Pristine Grandeur Wakad",
                description = "Spacious East-facing 2 BHK flat available for sale directly from owner. No brokerage. Well-ventilated living room, dry balcony, piped gas, and RO water system. Located 5 minutes away from Phoenix Marketcity Wakad and Hinjawadi IT Park Phase 1.",
                category = PropertyCategory.RESIDENTIAL_BUY.name,
                price = 8200000.0,
                priceDisplay = "₹82 Lakh",
                isNegotiable = false,
                maintenanceCharge = 2800.0,
                city = "Pune",
                locality = "Wakad",
                landmark = "Opposite Indira College",
                bhk = 2,
                bathrooms = 2,
                carpetAreaSqft = 780.0,
                floorNumber = 7,
                totalFloors = 14,
                furnishing = FurnishingStatus.FURNISHED.name,
                possession = PossessionStatus.READY_TO_MOVE.name,
                reraId = null,
                amenitiesJson = "Lift,Security,Piped Gas,Children Play Area,Covered Parking",
                imageUrlsJson = "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=1000&q=80,https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1000&q=80",
                linkedReelId = "reel_pune_02",
                isFeatured = false,
                viewsCount = 920,
                enquiriesCount = 15,
                createdAt = System.currentTimeMillis() - 86400000L * 3
            ),
            PropertyEntity(
                id = "prop_pune_03",
                userId = "usr_pune_builder_03",
                sellerName = "Godrej Properties Pune",
                sellerRole = UserRole.BUILDER.name,
                sellerPhone = "+91 80000 11223",
                sellerBadge = VerificationBadge.BUILDER_PARTNER.name,
                title = "Godrej Rivergreens — Nature-Centric 2 & 3 BHK Waterfront Homes",
                description = "Master-planned 100-acre township along the Mula-Mutha river. Features over 5 acres of open greens, 1 km riverfront boulevard, retail plaza, and wellness sports arena. Easy connectivity to EON IT Park and World Trade Centre Kharadi.",
                category = PropertyCategory.NEW_PROJECTS.name,
                price = 6900000.0,
                priceDisplay = "₹69 Lakh Onwards",
                isNegotiable = false,
                maintenanceCharge = 3200.0,
                city = "Pune",
                locality = "Kharadi",
                landmark = "Near EON IT Park & Riverside Road",
                bhk = 2,
                bathrooms = 2,
                carpetAreaSqft = 720.0,
                floorNumber = 4,
                totalFloors = 22,
                furnishing = FurnishingStatus.UNFURNISHED.name,
                possession = PossessionStatus.UNDER_CONSTRUCTION.name,
                reraId = "P52100022411",
                amenitiesJson = "Lift,24x7 Security,Clubhouse,Cricket Pitch,Swimming Pool,Tennis Court,EV Charging",
                imageUrlsJson = "https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1000&q=80,https://images.unsplash.com/photo-1580587771525-78b9dba3b914?w=1000&q=80",
                linkedReelId = "reel_pune_03",
                isFeatured = true,
                viewsCount = 3450,
                enquiriesCount = 62,
                createdAt = System.currentTimeMillis() - 86400000L * 1
            ),
            PropertyEntity(
                id = "prop_pune_04",
                userId = "usr_pune_broker_01",
                sellerName = "Vikram Deshmukh",
                sellerRole = UserRole.BROKER.name,
                sellerPhone = "+91 98230 45678",
                sellerBadge = VerificationBadge.RERA_VERIFIED.name,
                title = "Fully Furnished 2 BHK Rental Near Megapolis Hinjawadi",
                description = "Prime ready-to-move rental home for IT professionals. Includes double door refrigerator, smart TV, 3-seater sofa, queen beds with orthopaedic mattresses, modular kitchen with piped gas, washing machine, and high-speed fiber setup.",
                category = PropertyCategory.RESIDENTIAL_RENT.name,
                price = 28000.0,
                priceDisplay = "₹28,000 / mo",
                isNegotiable = true,
                maintenanceCharge = 2000.0,
                city = "Pune",
                locality = "Hinjawadi",
                landmark = "Phase 3, Megapolis Circle",
                bhk = 2,
                bathrooms = 2,
                carpetAreaSqft = 680.0,
                floorNumber = 9,
                totalFloors = 18,
                furnishing = FurnishingStatus.FURNISHED.name,
                possession = PossessionStatus.READY_TO_MOVE.name,
                reraId = null,
                amenitiesJson = "Lift,Power Backup,Security,High-Speed Wi-Fi Ready,Gym,Reserved Car Parking",
                imageUrlsJson = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=1000&q=80,https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1000&q=80",
                linkedReelId = "reel_pune_04",
                isFeatured = false,
                viewsCount = 760,
                enquiriesCount = 19,
                createdAt = System.currentTimeMillis() - 86400000L * 4
            ),
            PropertyEntity(
                id = "prop_pune_05",
                userId = "usr_pune_builder_05",
                sellerName = "Panchshil Realty Pune",
                sellerRole = UserRole.BUILDER.name,
                sellerPhone = "+91 88888 22114",
                sellerBadge = VerificationBadge.BUILDER_PARTNER.name,
                title = "Panchshil One — 4 BHK Presidential High-Rise Residence",
                description = "Ultra-luxury living in Kothrud with panoramic hillside vistas. Features private high-speed elevator opening into residence, 11-foot finished ceilings, German Poggenpohl kitchen, marble decks, and 5-star concierge lobby.",
                category = PropertyCategory.RESIDENTIAL_BUY.name,
                price = 38500000.0,
                priceDisplay = "₹3.85 Cr",
                isNegotiable = false,
                maintenanceCharge = 12000.0,
                city = "Pune",
                locality = "Kothrud",
                landmark = "Near MIT College & Paud Road",
                bhk = 4,
                bathrooms = 4,
                carpetAreaSqft = 2450.0,
                floorNumber = 16,
                totalFloors = 24,
                furnishing = FurnishingStatus.SEMI_FURNISHED.name,
                possession = PossessionStatus.READY_TO_MOVE.name,
                reraId = "P52100029801",
                amenitiesJson = "Private Elevator,Infinity Pool,Spa,Valet Parking,Clubhouse,Smart Home Automation,24x7 Concierge",
                imageUrlsJson = "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?w=1000&q=80,https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?w=1000&q=80",
                linkedReelId = null,
                isFeatured = true,
                viewsCount = 4210,
                enquiriesCount = 78,
                createdAt = System.currentTimeMillis() - 86400000L * 2
            ),
            PropertyEntity(
                id = "prop_pune_06",
                userId = "usr_pune_broker_01",
                sellerName = "Vikram Deshmukh",
                sellerRole = UserRole.BROKER.name,
                sellerPhone = "+91 98230 45678",
                sellerBadge = VerificationBadge.RERA_VERIFIED.name,
                title = "Riverfront Duplex Penthouse with Private Terrace & Plunge Pool",
                description = "East Pune's crown jewel. 4,200 sq.ft duplex penthouse overlooking the Mula-Mutha river with a private 800 sq.ft wooden deck, temperature-controlled plunge pool, double-height ceiling living hall, and 3 reserved basement parking bays.",
                category = PropertyCategory.RESIDENTIAL_BUY.name,
                price = 55000000.0,
                priceDisplay = "₹5.50 Cr",
                isNegotiable = true,
                maintenanceCharge = 18000.0,
                city = "Pune",
                locality = "Kalyani Nagar",
                landmark = "Jogger's Park Lane, Kalyani Nagar",
                bhk = 4,
                bathrooms = 5,
                carpetAreaSqft = 3400.0,
                floorNumber = 14,
                totalFloors = 14,
                furnishing = FurnishingStatus.FURNISHED.name,
                possession = PossessionStatus.READY_TO_MOVE.name,
                reraId = "P52100031102",
                amenitiesJson = "Private Pool,Terrace Garden,Jacuzzi,River View,Helipad Access,Designer Interiors,Servant Quarters",
                imageUrlsJson = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1000&q=80,https://images.unsplash.com/photo-1600585154526-990dced4db0d?w=1000&q=80",
                linkedReelId = "reel_02_image",
                isFeatured = true,
                viewsCount = 5900,
                enquiriesCount = 104,
                createdAt = System.currentTimeMillis() - 86400000L * 3
            ),
            PropertyEntity(
                id = "prop_pune_07",
                userId = "usr_pune_owner_04",
                sellerName = "Aditi Rao",
                sellerRole = UserRole.OWNER.name,
                sellerPhone = "+91 98225 99001",
                sellerBadge = VerificationBadge.OWNER_VERIFIED.name,
                title = "Garden Facing 3 BHK Flat in Balewadi Near High Street",
                description = "Vastu compliant 3 BHK in prime Balewadi, 2 minutes from Balewadi High Street dining and retail. Well maintained by direct owner with complete Italian marble flooring, 3 balconies, piped gas, and 2 covered car parks.",
                category = PropertyCategory.RESIDENTIAL_BUY.name,
                price = 17200000.0,
                priceDisplay = "₹1.72 Cr",
                isNegotiable = true,
                maintenanceCharge = 4500.0,
                city = "Pune",
                locality = "Balewadi",
                landmark = "2 mins from Balewadi High Street",
                bhk = 3,
                bathrooms = 3,
                carpetAreaSqft = 1250.0,
                floorNumber = 7,
                totalFloors = 15,
                furnishing = FurnishingStatus.SEMI_FURNISHED.name,
                possession = PossessionStatus.READY_TO_MOVE.name,
                reraId = "P52100024410",
                amenitiesJson = "Clubhouse,Swimming Pool,Garden,Kids Play Area,Jogging Track,Power Backup,CCTV",
                imageUrlsJson = "https://images.unsplash.com/photo-1600566753376-12c8ab7fb75b?w=1000&q=80,https://images.unsplash.com/photo-1600585152220-90363fe7e115?w=1000&q=80",
                linkedReelId = "reel_pune_07",
                isFeatured = false,
                viewsCount = 1890,
                enquiriesCount = 38,
                createdAt = System.currentTimeMillis() - 86400000L * 2
            ),
            PropertyEntity(
                id = "prop_pune_08",
                userId = "usr_pune_broker_02",
                sellerName = "Rohan Deshpande",
                sellerRole = UserRole.BROKER.name,
                sellerPhone = "+91 97650 44332",
                sellerBadge = VerificationBadge.RERA_VERIFIED.name,
                title = "Executive 3 BHK Apartment in Viman Nagar near Phoenix Marketcity",
                description = "Top investment & rental yield opportunity in prime Viman Nagar. Walking distance to Phoenix Marketcity, Symbiosis campus, and 10 mins to Pune Airport. High rental demand from airline & IT executives.",
                category = PropertyCategory.RESIDENTIAL_BUY.name,
                price = 18500000.0,
                priceDisplay = "₹1.85 Cr",
                isNegotiable = true,
                maintenanceCharge = 5000.0,
                city = "Pune",
                locality = "Viman Nagar",
                landmark = "Behind Phoenix Marketcity",
                bhk = 3,
                bathrooms = 3,
                carpetAreaSqft = 1320.0,
                floorNumber = 5,
                totalFloors = 12,
                furnishing = FurnishingStatus.SEMI_FURNISHED.name,
                possession = PossessionStatus.READY_TO_MOVE.name,
                reraId = "P52100021980",
                amenitiesJson = "Gym,Lift,Power Backup,24x7 Security,Clubhouse,Covered Parking,Gas Pipeline",
                imageUrlsJson = "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1000&q=80,https://images.unsplash.com/photo-1600573472550-8090b5e0745e?w=1000&q=80",
                linkedReelId = "reel_pune_08",
                isFeatured = false,
                viewsCount = 2340,
                enquiriesCount = 49,
                createdAt = System.currentTimeMillis() - 86400000L * 1
            ),
            PropertyEntity(
                id = "prop_mum_01",
                userId = "usr_mum_builder_01",
                sellerName = "Lodha Luxury Mumbai",
                sellerRole = UserRole.BUILDER.name,
                sellerPhone = "+91 98200 11223",
                sellerBadge = VerificationBadge.BUILDER_PARTNER.name,
                title = "Lodha World One — 3 BHK Sea View Residence in Worli",
                description = "Iconic ultra-luxury high-rise tower standing tall over the Arabian Sea. Features private sun decks, Italian marble finishes, Armani/Casa custom fittings, 7-acre private park, and high-speed private elevator lobby.",
                category = PropertyCategory.RESIDENTIAL_BUY.name,
                price = 72500000.0,
                priceDisplay = "₹7.25 Cr",
                isNegotiable = false,
                maintenanceCharge = 25000.0,
                city = "Mumbai",
                locality = "Worli",
                landmark = "Worli Sea Face / Lower Parel",
                bhk = 3,
                bathrooms = 4,
                carpetAreaSqft = 1950.0,
                floorNumber = 42,
                totalFloors = 76,
                furnishing = FurnishingStatus.FURNISHED.name,
                possession = PossessionStatus.READY_TO_MOVE.name,
                reraId = "P51900008962",
                amenitiesJson = "Sea View,Private Elevator,Concierge,Infinity Pool,Helipad,Spa,Gym,Cricket Pitch",
                imageUrlsJson = "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=1000&q=80,https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1000&q=80",
                linkedReelId = "reel_04_image",
                isFeatured = true,
                viewsCount = 7890,
                enquiriesCount = 145,
                createdAt = System.currentTimeMillis() - 86400000L * 1
            ),
            PropertyEntity(
                id = "prop_mum_02",
                userId = "usr_mum_broker_01",
                sellerName = "Kunal Mehta",
                sellerRole = UserRole.BROKER.name,
                sellerPhone = "+91 98210 55667",
                sellerBadge = VerificationBadge.RERA_VERIFIED.name,
                title = "Rustomjee Seasons — 3 BHK Luxury Living in Bandra West",
                description = "Prestigious residential enclave near BKC and Western Express Highway. 3 BHK with bespoke interiors, floor-to-ceiling glass apertures, open city skyline views, indoor games lounge, and temperature controlled lap pool.",
                category = PropertyCategory.RESIDENTIAL_BUY.name,
                price = 58000000.0,
                priceDisplay = "₹5.80 Cr",
                isNegotiable = true,
                maintenanceCharge = 16000.0,
                city = "Mumbai",
                locality = "Bandra West",
                landmark = "Near Bandra-Kurla Complex (BKC)",
                bhk = 3,
                bathrooms = 3,
                carpetAreaSqft = 1420.0,
                floorNumber = 18,
                totalFloors = 28,
                furnishing = FurnishingStatus.SEMI_FURNISHED.name,
                possession = PossessionStatus.READY_TO_MOVE.name,
                reraId = "P51800001433",
                amenitiesJson = "Clubhouse,Pool,Gym,Tennis Court,Mini Theatre,Valet Parking,24x7 Security",
                imageUrlsJson = "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?w=1000&q=80,https://images.unsplash.com/photo-1600585154526-990dced4db0d?w=1000&q=80",
                linkedReelId = "reel_mum_02",
                isFeatured = true,
                viewsCount = 6320,
                enquiriesCount = 112,
                createdAt = System.currentTimeMillis() - 86400000L * 2
            ),
            PropertyEntity(
                id = "prop_mum_03",
                userId = "usr_mum_builder_02",
                sellerName = "Oberoi Realty Mumbai",
                sellerRole = UserRole.BUILDER.name,
                sellerPhone = "+91 98205 33441",
                sellerBadge = VerificationBadge.BUILDER_PARTNER.name,
                title = "Oberoi Sky City — 2 BHK Elegant High-Rise in Andheri West",
                description = "Modern living right off Link Road and Lokhandwala. High carpet efficiency 2 BHK with smart automation, modular kitchen, landscaped podium gardens, jogging track, and metro connectivity within 200 meters.",
                category = PropertyCategory.RESIDENTIAL_BUY.name,
                price = 26500000.0,
                priceDisplay = "₹2.65 Cr",
                isNegotiable = false,
                maintenanceCharge = 8000.0,
                city = "Mumbai",
                locality = "Andheri West",
                landmark = "Near Lokhandwala Complex & Metro Station",
                bhk = 2,
                bathrooms = 2,
                carpetAreaSqft = 880.0,
                floorNumber = 22,
                totalFloors = 45,
                furnishing = FurnishingStatus.UNFURNISHED.name,
                possession = PossessionStatus.UNDER_CONSTRUCTION.name,
                reraId = "P51800003582",
                amenitiesJson = "Metro Access,Podium Garden,Gym,Kids Play Area,Jogging Track,Power Backup,CCTV",
                imageUrlsJson = "https://images.unsplash.com/photo-1600566753376-12c8ab7fb75b?w=1000&q=80,https://images.unsplash.com/photo-1580587771525-78b9dba3b914?w=1000&q=80",
                linkedReelId = null,
                isFeatured = false,
                viewsCount = 4120,
                enquiriesCount = 89,
                createdAt = System.currentTimeMillis() - 86400000L * 3
            ),
            PropertyEntity(
                id = "prop_mum_04",
                userId = "usr_mum_owner_01",
                sellerName = "Sanjay Singhania",
                sellerRole = UserRole.OWNER.name,
                sellerPhone = "+91 98201 88990",
                sellerBadge = VerificationBadge.OWNER_VERIFIED.name,
                title = "Hiranandani Gardens — 2 BHK Lake View Rental in Powai",
                description = "Neoclassical architecture apartment in prime Powai Hiranandani township. Fully furnished with Italian marble floors, air conditioning in all rooms, washing machine, refrigerator, facing Powai lake. Zero brokerage directly from owner.",
                category = PropertyCategory.RESIDENTIAL_RENT.name,
                price = 75000.0,
                priceDisplay = "₹75,000 / mo",
                isNegotiable = true,
                maintenanceCharge = 5000.0,
                city = "Mumbai",
                locality = "Powai",
                landmark = "Near Powai Lake & Galleria Shopping Mall",
                bhk = 2,
                bathrooms = 2,
                carpetAreaSqft = 920.0,
                floorNumber = 12,
                totalFloors = 24,
                furnishing = FurnishingStatus.FURNISHED.name,
                possession = PossessionStatus.READY_TO_MOVE.name,
                reraId = null,
                amenitiesJson = "Lake View,Clubhouse,Supermarket,Covered Parking,Piped Gas,Power Backup,Gym",
                imageUrlsJson = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=1000&q=80,https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1000&q=80",
                linkedReelId = null,
                isFeatured = false,
                viewsCount = 2890,
                enquiriesCount = 64,
                createdAt = System.currentTimeMillis() - 86400000L * 2
            )
        )
        propertyDao.insertProperties(initialProperties)

        reelDao.deleteLegacyReels()

        val initialReels = listOf(
            ReelEntity(
                id = "reel_02_image",
                userId = "usr_pune_broker_01",
                creatorName = "Vikram Deshmukh",
                creatorBadge = VerificationBadge.RERA_VERIFIED.name,
                creatorRole = UserRole.BROKER.name,
                propertyId = "prop_pune_06",
                propertyTitle = "Riverfront Duplex Penthouse",
                propertyPriceDisplay = "₹5.50 Cr",
                propertyLocality = "Kalyani Nagar, Pune",
                propertyBhk = 4,
                caption = "📸 High-res photo tour of Pune's most exclusive riverfront address! 4,200 sq.ft duplex penthouse with private plunge pool and terrace garden. Tap Enquire for private preview.",
                videoUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1080&q=85",
                thumbnailUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1000&q=80",
                durationSeconds = 5,
                city = "Pune",
                locality = "Kalyani Nagar",
                isSponsored = true,
                likesCount = 2180,
                commentsCount = 142,
                savesCount = 870,
                sharesCount = 512,
                isLikedByMe = true,
                isSavedByMe = true,
                mediaType = "image"
            ),
            ReelEntity(
                id = "reel_04_image",
                userId = "usr_mum_builder_01",
                creatorName = "Lodha Luxury Mumbai",
                creatorBadge = VerificationBadge.BUILDER_PARTNER.name,
                creatorRole = UserRole.BUILDER.name,
                propertyId = "prop_mum_01",
                propertyTitle = "Lodha World One — 3 BHK Sea View",
                propertyPriceDisplay = "₹7.25 Cr",
                propertyLocality = "Worli, Mumbai",
                propertyBhk = 3,
                caption = "🌊 Live above the clouds with panoramic Arabian Sea views in Worli! Private sun decks, Armani/Casa custom fittings, and 7-acre private park. Verified MahaRERA: P51900008962.",
                videoUrl = "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1080&q=85",
                thumbnailUrl = "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=1000&q=80",
                durationSeconds = 5,
                city = "Mumbai",
                locality = "Worli",
                isSponsored = true,
                likesCount = 3450,
                commentsCount = 210,
                savesCount = 1200,
                sharesCount = 680,
                isLikedByMe = false,
                isSavedByMe = false,
                mediaType = "image"
            )
        )
        reelDao.insertReels(initialReels)
        seedPrefs.edit().putBoolean("seeded_initial_catalog_v5", true).apply()

        val initialLeads = listOf(
            LeadEntity(
                id = "lead_pune_01",
                enquiryId = "enq_101",
                brokerId = "usr_pune_broker_01",
                customerName = "Rohan Sharma",
                customerPhone = "+91 98221 11222",
                propertyId = "prop_pune_01",
                propertyTitle = "VTP Earth One — Luxury 3 BHK",
                propertyPriceDisplay = "₹1.45 Cr",
                locality = "Baner",
                stage = LeadStage.SITE_VISIT.name,
                notes = "Customer requested weekend site visit with family. Verified budget ₹1.5 Cr approved loan from HDFC.",
                scheduledVisitDate = "Saturday, 11:30 AM",
                createdAt = System.currentTimeMillis() - 86400000L * 1,
                updatedAt = System.currentTimeMillis() - 3600000L * 4
            ),
            LeadEntity(
                id = "lead_pune_02",
                enquiryId = "enq_102",
                brokerId = "usr_pune_broker_01",
                customerName = "Ananya Patwardhan",
                customerPhone = "+91 97654 33211",
                propertyId = "prop_pune_04",
                propertyTitle = "Furnished 2 BHK Rental Hinjawadi",
                propertyPriceDisplay = "₹28,000 / mo",
                locality = "Hinjawadi",
                stage = LeadStage.CONTACTED.name,
                notes = "Works at Infosys Phase 2. Moving from Bangalore next month. Wants video walkthrough call tomorrow.",
                scheduledVisitDate = null,
                createdAt = System.currentTimeMillis() - 3600000L * 8,
                updatedAt = System.currentTimeMillis() - 3600000L * 2
            ),
            LeadEntity(
                id = "lead_pune_03",
                enquiryId = "enq_103",
                brokerId = "usr_pune_broker_01",
                customerName = "Amitabh Sen",
                customerPhone = "+91 99234 88776",
                propertyId = "prop_pune_01",
                propertyTitle = "VTP Earth One — Luxury 3 BHK",
                propertyPriceDisplay = "₹1.45 Cr",
                locality = "Baner",
                stage = LeadStage.NEW.name,
                notes = "Inquired via Reel CTA: 'Schedule Site Visit & Request Brochure'.",
                scheduledVisitDate = null,
                createdAt = System.currentTimeMillis() - 1800000L,
                updatedAt = System.currentTimeMillis() - 1800000L
            )
        )
        leadDao.insertLeads(initialLeads)

        // Seed initial Referral Code for demo user (Vikram Deshmukh: usr_pune_broker_01)
        val defaultCode = ReferralCodeEntity(
            code = "PROP9823",
            userId = "usr_pune_broker_01",
            createdAt = System.currentTimeMillis() - 86400000L * 7,
            isActive = true
        )
        referralDao.insertReferralCode(defaultCode)

        // Seed demonstration referrals for Vikram Deshmukh
        val demoReferrals = listOf(
            ReferralEntity(
                id = "ref_demo_01",
                referrerId = "usr_pune_broker_01",
                referredUserId = "usr_pune_owner_02", // Priya Kulkarni
                referralCode = "PROP9823",
                attributionSource = AttributionSource.WHATSAPP.name,
                status = ReferralStatus.QUALIFIED.name,
                createdAt = System.currentTimeMillis() - 86400000L * 5,
                qualifiedAt = System.currentTimeMillis() - 86400000L * 3,
                rewardedAt = null,
                notes = "Attributed via WhatsApp share | Qualified: Verified owner listing added"
            ),
            ReferralEntity(
                id = "ref_demo_02",
                referrerId = "usr_pune_broker_01",
                referredUserId = "usr_client_03",
                referralCode = "PROP9823",
                attributionSource = AttributionSource.COPY_LINK.name,
                status = ReferralStatus.PENDING.name,
                createdAt = System.currentTimeMillis() - 86400000L * 2,
                qualifiedAt = null,
                rewardedAt = null,
                notes = "Attributed via Copied Link | Waiting for first property interaction"
            ),
            ReferralEntity(
                id = "ref_demo_03",
                referrerId = "usr_pune_broker_01",
                referredUserId = "usr_client_04",
                referralCode = "PROP9823",
                attributionSource = AttributionSource.DIRECT_LINK.name,
                status = ReferralStatus.PENDING.name,
                createdAt = System.currentTimeMillis() - 3600000L * 6,
                qualifiedAt = null,
                rewardedAt = null,
                notes = "Attributed via Direct Link (propreel.in/refer/PROP9823)"
            )
        )
        demoReferrals.forEach { referral ->
            if (referralDao.getReferralById(referral.id) == null) {
                referralDao.insertReferral(referral)
            }
        }

        seedBaner4BhkReelIfMissing()
        seedDemoCoinEconomyIfMissing()
    }

    private suspend fun seedBaner4BhkReelIfMissing() {
        if (seedPrefs.getBoolean("seeded_baner_4bhk_v5", false)) {
            return
        }
        val now = System.currentTimeMillis()
        if (propertyDao.getPropertyById("prop_pune_baner_4bhk") == null) {
            propertyDao.insertProperty(
                PropertyEntity(
                    id = "prop_pune_baner_4bhk",
                    userId = "usr_pune_vastu_01",
                    sellerName = "Pune Vastu",
                    sellerRole = UserRole.BROKER.name,
                    sellerPhone = "+91 91194 87956",
                    sellerBadge = VerificationBadge.RERA_VERIFIED.name,
                    title = "Ready Possession 4 BHK — Baner (2000 sq.ft)",
                    description = "Ready possession 4 BHK flat in prime Baner, Pune spanning 2000 sq.ft. Vastu-compliant layout with spacious covered swing balcony, glass railings, skyline view, and immediate possession. Direct enquiry: +91 91194 87956.",
                    category = PropertyCategory.RESIDENTIAL_BUY.name,
                    price = 24500000.0,
                    priceDisplay = "₹2.45 Cr",
                    isNegotiable = true,
                    maintenanceCharge = 5500.0,
                    city = "Pune",
                    locality = "Baner",
                    landmark = "Prime Baner Road Corridor",
                    bhk = 4,
                    bathrooms = 4,
                    carpetAreaSqft = 2000.0,
                    floorNumber = 9,
                    totalFloors = 18,
                    furnishing = FurnishingStatus.SEMI_FURNISHED.name,
                    possession = PossessionStatus.READY_TO_MOVE.name,
                    reraId = "P52100048192",
                    amenitiesJson = "Lift,Security,Covered Parking,Power Backup,Vaastu Compliant,Balcony Swing Deck,Clubhouse",
                    imageUrlsJson = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=80,https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?w=1000&q=80",
                    linkedReelId = "reel_05_video",
                    isFeatured = true,
                    viewsCount = 640,
                    enquiriesCount = 19,
                    createdAt = now - 3600000L * 2
                )
            )
        }
        if (reelDao.getReelById("reel_05_video") == null) {
            reelDao.insertReel(
                ReelEntity(
                    id = "reel_05_video",
                    userId = "usr_pune_vastu_01",
                    creatorName = "Pune Vastu",
                    creatorBadge = VerificationBadge.RERA_VERIFIED.name,
                    creatorRole = UserRole.BROKER.name,
                    propertyId = "prop_pune_baner_4bhk",
                    propertyTitle = "Ready Possession 4 BHK — Baner",
                    propertyPriceDisplay = "₹2.45 Cr",
                    propertyLocality = "Baner, Pune",
                    propertyBhk = 4,
                    caption = "Ready Possession 4 BHK in Baner, Pune | 2000 sq.ft Flat with spacious balcony & skyline view. Enquiry: 9119487956",
                    videoUrl = "https://assets.propreel.in/videos/demo_reel_baner_4bhk.mp4",
                    thumbnailUrl = "https://assets.propreel.in/posters/poster_baner_4bhk.jpg",
                    durationSeconds = 10,
                    city = "Pune",
                    locality = "Baner",
                    isSponsored = false,
                    likesCount = 184,
                    commentsCount = 22,
                    savesCount = 67,
                    sharesCount = 31,
                    isLikedByMe = false,
                    isSavedByMe = false,
                    mediaType = "video"
                )
            )
        }
        seedPrefs.edit().putBoolean("seeded_baner_4bhk_v5", true).apply()
    }

    private suspend fun seedDemoCoinEconomyIfMissing() {
        // Seed a second referral code so active user can also test applying a peer code
        if (referralDao.getReferralCode("PROP0002") == null) {
            referralDao.insertReferralCode(
                ReferralCodeEntity(
                    code = "PROP0002",
                    userId = "usr_mum_builder_01",
                    createdAt = System.currentTimeMillis() - 86400000L * 10,
                    isActive = true
                )
            )
        }

        // Seed one REWARDED demonstration referral + its atomic 50 + 25 coin ledger entries
        if (referralDao.getReferralById("ref_demo_00") == null &&
            referralDao.getReferralByReferredUser("usr_client_00") == null
        ) {
            val now = System.currentTimeMillis()
            referralDao.insertReferral(
                ReferralEntity(
                    id = "ref_demo_00",
                    referrerId = "usr_pune_broker_01",
                    referredUserId = "usr_client_00",
                    referralCode = "PROP9823",
                    attributionSource = AttributionSource.WHATSAPP.name,
                    status = ReferralStatus.REWARDED.name,
                    createdAt = now - 86400000L * 6,
                    qualifiedAt = now - 86400000L * 4,
                    rewardedAt = now - 86400000L * 4,
                    notes = "Attributed via WhatsApp | Qualified & Credited 50 APP_COINS"
                )
            )
        }

        if (coinLedgerDao.getByIdempotencyKey("reward_referrer_ref_demo_00") == null) {
            val now = System.currentTimeMillis()
            db.withTransaction {
                if (coinLedgerDao.getByIdempotencyKey("reward_referrer_ref_demo_00") == null) {
                    coinLedgerDao.insertTransaction(
                        CoinLedgerEntity(
                            id = "tx_demo_ref_00",
                            userId = "usr_pune_broker_01",
                            amount = CoinEconomyConfig.REFERRER_REWARD_COINS,
                            transactionType = CoinTransactionType.CREDIT.name,
                            category = CoinTransactionCategory.REFERRAL_REWARD.name,
                            description = "Referral reward (+50 APP_COINS) for inviting client_00",
                            referenceId = "ref_demo_00",
                            idempotencyKey = "reward_referrer_ref_demo_00",
                            timestamp = now - 86400000L * 4
                        )
                    )
                }
                if (coinLedgerDao.getByIdempotencyKey("reward_invitee_ref_demo_00") == null) {
                    coinLedgerDao.insertTransaction(
                        CoinLedgerEntity(
                            id = "tx_demo_wel_00",
                            userId = "usr_client_00",
                            amount = CoinEconomyConfig.INVITEE_WELCOME_COINS,
                            transactionType = CoinTransactionType.CREDIT.name,
                            category = CoinTransactionCategory.WELCOME_REWARD.name,
                            description = "Welcome bonus (+25 APP_COINS) via code PROP9823",
                            referenceId = "ref_demo_00",
                            idempotencyKey = "reward_invitee_ref_demo_00",
                            timestamp = now - 86400000L * 4
                        )
                    )
                }
            }
        }
    }

    // --- MAPPERS ---
    private fun PropertyEntity.toDomain(): Property {
        return Property(
            id = id,
            userId = userId,
            sellerName = sellerName,
            sellerRole = UserRole.valueOf(sellerRole),
            sellerPhone = sellerPhone,
            sellerBadge = VerificationBadge.valueOf(sellerBadge),
            title = title,
            description = description,
            category = PropertyCategory.valueOf(category),
            price = price,
            priceDisplay = priceDisplay,
            isNegotiable = isNegotiable,
            maintenanceCharge = maintenanceCharge,
            city = city,
            locality = locality,
            landmark = landmark,
            bhk = bhk,
            bathrooms = bathrooms,
            carpetAreaSqft = carpetAreaSqft,
            floorNumber = floorNumber,
            totalFloors = totalFloors,
            furnishing = FurnishingStatus.valueOf(furnishing),
            possession = PossessionStatus.valueOf(possession),
            reraId = reraId,
            amenities = amenitiesJson.split(",").filter { it.isNotBlank() },
            imageUrls = imageUrlsJson.split(",").filter { it.isNotBlank() },
            linkedReelId = linkedReelId,
            isFeatured = isFeatured,
            viewsCount = viewsCount,
            enquiriesCount = enquiriesCount,
            createdAt = createdAt
        )
    }

    private fun Property.toEntity(): PropertyEntity {
        return PropertyEntity(
            id = id,
            userId = userId,
            sellerName = sellerName,
            sellerRole = sellerRole.name,
            sellerPhone = sellerPhone,
            sellerBadge = sellerBadge.name,
            title = title,
            description = description,
            category = category.name,
            price = price,
            priceDisplay = priceDisplay,
            isNegotiable = isNegotiable,
            maintenanceCharge = maintenanceCharge,
            city = city,
            locality = locality,
            landmark = landmark,
            bhk = bhk,
            bathrooms = bathrooms,
            carpetAreaSqft = carpetAreaSqft,
            floorNumber = floorNumber,
            totalFloors = totalFloors,
            furnishing = furnishing.name,
            possession = possession.name,
            reraId = reraId,
            amenitiesJson = amenities.joinToString(","),
            imageUrlsJson = imageUrls.joinToString(","),
            linkedReelId = linkedReelId,
            isFeatured = isFeatured,
            viewsCount = viewsCount,
            enquiriesCount = enquiriesCount,
            createdAt = createdAt
        )
    }

    private fun ReelEntity.toDomain(): Reel {
        return Reel(
            id = id,
            userId = userId,
            creatorName = creatorName,
            creatorBadge = VerificationBadge.valueOf(creatorBadge),
            creatorRole = UserRole.valueOf(creatorRole),
            propertyId = propertyId,
            propertyTitle = propertyTitle,
            propertyPriceDisplay = propertyPriceDisplay,
            propertyLocality = propertyLocality,
            propertyBhk = propertyBhk,
            caption = caption,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            durationSeconds = durationSeconds,
            city = city,
            locality = locality,
            isSponsored = isSponsored,
            likesCount = likesCount,
            commentsCount = commentsCount,
            savesCount = savesCount,
            sharesCount = sharesCount,
            isLikedByMe = isLikedByMe,
            isSavedByMe = isSavedByMe,
            mediaType = when {
                mediaType.equals("image", ignoreCase = true) -> "image"
                mediaType.equals("video", ignoreCase = true) -> "video"
                videoUrl.endsWith(".jpg", ignoreCase = true) || videoUrl.endsWith(".png", ignoreCase = true) || videoUrl.contains("images.unsplash.com") -> "image"
                else -> "video"
            }
        )
    }

    private fun Reel.toEntity(): ReelEntity {
        return ReelEntity(
            id = id,
            userId = userId,
            creatorName = creatorName,
            creatorBadge = creatorBadge.name,
            creatorRole = creatorRole.name,
            propertyId = propertyId,
            propertyTitle = propertyTitle,
            propertyPriceDisplay = propertyPriceDisplay,
            propertyLocality = propertyLocality,
            propertyBhk = propertyBhk,
            caption = caption,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            durationSeconds = durationSeconds,
            city = city,
            locality = locality,
            isSponsored = isSponsored,
            likesCount = likesCount,
            commentsCount = commentsCount,
            savesCount = savesCount,
            sharesCount = sharesCount,
            isLikedByMe = isLikedByMe,
            isSavedByMe = isSavedByMe,
            mediaType = mediaType
        )
    }

    private fun LeadEntity.toDomain(): Lead {
        return Lead(
            id = id,
            enquiryId = enquiryId,
            brokerId = brokerId,
            customerName = customerName,
            customerPhone = customerPhone,
            propertyId = propertyId,
            propertyTitle = propertyTitle,
            propertyPriceDisplay = propertyPriceDisplay,
            locality = locality,
            stage = LeadStage.valueOf(stage),
            notes = notes,
            scheduledVisitDate = scheduledVisitDate,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun ReferralCodeEntity.toDomain(): ReferralCode =
        ReferralCode(code = code, userId = userId, createdAt = createdAt, isActive = isActive)

    private fun ReferralEntity.toDomain(): Referral =
        Referral(
            id = id,
            referrerId = referrerId,
            referredUserId = referredUserId,
            referralCode = referralCode,
            attributionSource = AttributionSource.valueOf(attributionSource),
            status = ReferralStatus.valueOf(status),
            createdAt = createdAt,
            qualifiedAt = qualifiedAt,
            rewardedAt = rewardedAt,
            notes = notes
        )

    private fun CoinLedgerEntity.toDomain(): CoinTransaction =
        CoinTransaction(
            id = id,
            userId = userId,
            amount = amount,
            transactionType = CoinTransactionType.valueOf(transactionType),
            category = CoinTransactionCategory.valueOf(category),
            description = description,
            referenceId = referenceId,
            idempotencyKey = idempotencyKey,
            timestamp = timestamp
        )
}
