package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PropReelApiService {

    @GET("api/v1/reels/feed")
    suspend fun getReelsFeed(
        @Query("city") city: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<List<NetworkReel>>

    @GET("api/v1/properties")
    suspend fun getProperties(
        @Query("city") city: String,
        @Query("locality") locality: String? = null,
        @Query("category") category: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("bhk") bhk: Int? = null,
        @Query("verifiedOnly") verifiedOnly: Boolean? = null
    ): ApiResponse<List<NetworkProperty>>

    @GET("api/v1/properties/{id}")
    suspend fun getPropertyDetail(
        @Path("id") id: String
    ): ApiResponse<NetworkProperty>

    @POST("api/v1/enquiries")
    suspend fun submitEnquiry(
        @Body request: EnquiryRequest
    ): ApiResponse<EnquiryResponse>

    @GET("api/v1/leads")
    suspend fun getLeads(
        @Query("brokerId") brokerId: String? = null,
        @Query("stage") stage: String? = null
    ): ApiResponse<List<NetworkLead>>

    @PATCH("api/v1/leads/{id}/stage")
    suspend fun updateLeadStage(
        @Path("id") leadId: String,
        @Body request: UpdateLeadStageRequest
    ): ApiResponse<NetworkLead>

    @POST("api/v1/auth/send-otp")
    suspend fun sendOtp(
        @Body request: SendOtpRequest
    ): ApiResponse<SendOtpResponse>

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): ApiResponse<AuthResponse>

    @POST("api/v1/auth/update-role")
    suspend fun updateRole(
        @Body request: UpdateRoleRequest
    ): ApiResponse<Map<String, Any>>

    // --- PHASE 4: SERVER-AUTHORITATIVE COIN ECONOMY ENDPOINTS ---

    @GET("api/v1/coins/ledger")
    suspend fun getCoinLedger(
        @Query("userId") userId: String
    ): ApiResponse<CoinLedgerResponse>

    @POST("api/v1/referrals/{id}/reward")
    suspend fun verifyAndRewardReferral(
        @Path("id") referralId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: VerifyAndRewardReferralRequest
    ): ApiResponse<VerifyAndRewardReferralResponse>

    @POST("api/v1/coins/redeem")
    suspend fun redeemCoins(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: RedeemCoinsRequest
    ): ApiResponse<RedeemCoinsResponse>
}
