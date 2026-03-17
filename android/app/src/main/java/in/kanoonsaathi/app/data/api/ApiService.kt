package `in`.kanoonsaathi.app.data.api

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("auth/otp/send")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<ApiResponse<Map<String, String>>>

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/role")
    suspend fun setRole(
        @Header("Authorization") authHeader: String,
        @Body request: SetRoleRequest,
    ): Response<ApiResponse<SetRoleResponse>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<RefreshTokenResponse>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Map<String, String>>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponse<UserDto>>

    // ── User ──────────────────────────────────────────────────────────────────
    @POST("user/onboarding")
    suspend fun submitStudentOnboarding(@Body request: StudentOnboardingRequest): Response<ApiResponse<Map<String, String>>>

    @POST("user/onboarding")
    suspend fun submitFacultyOnboarding(@Body request: FacultyOnboardingRequest): Response<ApiResponse<Map<String, String>>>

    @POST("user/onboarding")
    suspend fun submitCuriousOnboarding(@Body request: CuriousOnboardingRequest): Response<ApiResponse<Map<String, String>>>

    @GET("user/usage")
    suspend fun getUsage(): Response<ApiResponse<UsageResponse>>

    @DELETE("user/me")
    suspend fun deleteAccount(): Response<ApiResponse<Map<String, String>>>

    // ── Chat ──────────────────────────────────────────────────────────────────
    @GET("chat/modes")
    suspend fun getChatModes(): Response<ApiResponse<List<ChatModeDto>>>

    @GET("chat/suggested")
    suspend fun getSuggestedPrompts(): Response<ApiResponse<Map<String, List<String>>>>

    @POST("chat/sessions")
    suspend fun createSession(@Body request: CreateSessionRequest): Response<ApiResponse<ChatSessionDto>>

    @GET("chat/sessions")
    suspend fun getSessions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<SessionsResponse>>

    @GET("chat/sessions/{id}")
    suspend fun getSession(@Path("id") id: String): Response<ApiResponse<ChatSessionDto>>

    @DELETE("chat/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String): Response<ApiResponse<Map<String, String>>>

    @PUT("chat/sessions/{id}/bookmark")
    suspend fun toggleBookmark(@Path("id") id: String): Response<ApiResponse<BookmarkResponse>>

    @POST("chat/sessions/{id}/messages")
    suspend fun sendMessage(
        @Path("id") sessionId: String,
        @Body request: SendMessageRequest,
    ): Response<ApiResponse<SendMessageResponse>>

    @GET("chat/bookmarks")
    suspend fun getBookmarks(): Response<ApiResponse<List<ChatSessionDto>>>

    @POST("chat/report")
    suspend fun reportMessage(@Body request: ReportRequest): Response<ApiResponse<Map<String, String>>>

    // ── News ──────────────────────────────────────────────────────────────────
    @GET("news/feed")
    suspend fun getNewsFeed(
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null,
    ): Response<ApiResponse<NewsFeedResponse>>

    // ── Internship Opportunities ───────────────────────────────────────────────
    @GET("firms/opportunities")
    suspend fun getOpportunities(): Response<ApiResponse<List<InternshipPostingDto>>>

    @GET("firms/notifications/unread")
    suspend fun getUnreadOpportunityCount(): Response<ApiResponse<UnreadCountResponse>>
}
