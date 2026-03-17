package `in`.kanoonsaathi.app.data.api

import com.google.gson.annotations.SerializedName

// ── Generic API wrapper ───────────────────────────────────────────────────────

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
)

data class ApiError(
    val code: String,
    val message: String,
    val details: Any? = null,
)

// ── Auth ──────────────────────────────────────────────────────────────────────

data class SendOtpRequest(val email: String)

data class VerifyOtpRequest(
    val email: String,
    val otp: String,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("deviceModel") val deviceModel: String? = null,
    @SerializedName("appVersion") val appVersion: String? = null,
)

data class GoogleAuthRequest(
    @SerializedName("idToken") val idToken: String,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("deviceModel") val deviceModel: String? = null,
    @SerializedName("appVersion") val appVersion: String? = null,
)

data class AuthResponse(
    @SerializedName("isNewUser") val isNewUser: Boolean,
    @SerializedName("requiresRoleSelection") val requiresRoleSelection: Boolean = false,
    // New users receive a short-lived setup token instead of a userId in plain text
    @SerializedName("setupToken") val setupToken: String? = null,
    // Existing users receive full session tokens
    @SerializedName("accessToken") val accessToken: String? = null,
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("sessionId") val sessionId: String? = null,
)

data class SetRoleRequest(
    @SerializedName("role") val role: String,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("deviceModel") val deviceModel: String? = null,
    @SerializedName("appVersion") val appVersion: String? = null,
)

data class SetRoleResponse(
    val role: String,
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("sessionId") val sessionId: String,
)

data class RefreshTokenRequest(@SerializedName("refreshToken") val refreshToken: String)

data class RefreshTokenResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
)

// ── User / Profile ─────────────────────────────────────────────────────────────

data class UserDto(
    val id: String,
    val email: String,
    val role: String,
    val status: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("studentProfile") val studentProfile: StudentProfileDto? = null,
    @SerializedName("facultyProfile") val facultyProfile: FacultyProfileDto? = null,
    @SerializedName("curiousProfile") val curiousProfile: CuriousProfileDto? = null,
    val subscription: SubscriptionDto? = null,
)

data class StudentProfileDto(
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("collegeName") val collegeName: String?,
    @SerializedName("yearOfStudy") val yearOfStudy: Int?,
    val semester: Int?,
    @SerializedName("examTarget") val examTarget: String?,
    @SerializedName("subjectsOfInterest") val subjectsOfInterest: List<String>,
    @SerializedName("onboardingComplete") val onboardingComplete: Boolean,
)

data class FacultyProfileDto(
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("institutionName") val institutionName: String?,
    val designation: String?,
    @SerializedName("subjectsTaught") val subjectsTaught: List<String>,
    @SerializedName("onboardingComplete") val onboardingComplete: Boolean,
    val verified: Boolean,
)

data class CuriousProfileDto(
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("disclaimerAccepted") val disclaimerAccepted: Boolean,
    @SerializedName("onboardingComplete") val onboardingComplete: Boolean,
)

data class SubscriptionDto(
    val plan: String,
    val status: String,
    @SerializedName("expiresAt") val expiresAt: String?,
)

// ── Onboarding ─────────────────────────────────────────────────────────────────

data class StudentOnboardingRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("collegeName") val collegeName: String,
    @SerializedName("yearOfStudy") val yearOfStudy: Int,
    val semester: Int,
    @SerializedName("subjectsOfInterest") val subjectsOfInterest: List<String>,
    @SerializedName("examTarget") val examTarget: String?,
)

data class FacultyOnboardingRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("institutionName") val institutionName: String,
    val designation: String,
    @SerializedName("subjectsTaught") val subjectsTaught: List<String>,
    @SerializedName("barCouncilId") val barCouncilId: String?,
)

data class CuriousOnboardingRequest(
    @SerializedName("displayName") val displayName: String,
    @SerializedName("areasOfInterest") val areasOfInterest: List<String>,
    @SerializedName("disclaimerAccepted") val disclaimerAccepted: Boolean,
)

// ── Chat ───────────────────────────────────────────────────────────────────────

data class ChatModeDto(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val roles: List<String>,
)

data class CreateSessionRequest(@SerializedName("chatMode") val chatMode: String)

data class ChatSessionDto(
    val id: String,
    @SerializedName("chatMode") val chatMode: String,
    val title: String?,
    @SerializedName("isBookmarked") val isBookmarked: Boolean,
    @SerializedName("messageCount") val messageCount: Int,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    val messages: List<ChatMessageDto>? = null,
)

data class ChatMessageDto(
    val id: String,
    val role: String,   // "user" | "assistant"
    val content: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("wasRefused") val wasRefused: Boolean = false,
)

data class SendMessageRequest(@SerializedName("content") val content: String)

data class SendMessageResponse(
    @SerializedName("userMessage") val userMessage: ChatMessageDto,
    @SerializedName("aiMessage") val aiMessage: ChatMessageDto,
    val refused: Boolean,
    val warning: String? = null,
)

data class SessionsResponse(
    val sessions: List<ChatSessionDto>,
    val pagination: PaginationDto,
)

data class PaginationDto(
    val page: Int,
    val limit: Int,
    val total: Int,
    val pages: Int,
)

data class BookmarkResponse(@SerializedName("isBookmarked") val isBookmarked: Boolean)

data class ReportRequest(
    @SerializedName("messageId") val messageId: String,
    val reason: String,
)

// ── Usage ──────────────────────────────────────────────────────────────────────

data class UsageResponse(
    val today: TodayUsage,
    val plan: String,
)

data class TodayUsage(
    val messages: Int,
    val tokens: Int,
)

// ── News ───────────────────────────────────────────────────────────────────────

data class NewsItemDto(
    val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val source: String,
    val category: String,
    @SerializedName("publishedAt") val publishedAt: String,
)

data class NewsFeedResponse(
    val items: List<NewsItemDto>,
    val total: Int,
    @SerializedName("cachedAt") val cachedAt: String,
)

// ── Internship Marketplace ─────────────────────────────────────────────────────

data class InternshipFirmDto(
    val name: String,
    val city: String,
    val state: String,
    val specialties: List<String>,
    val website: String?,
)

data class InternshipPostingDto(
    val id: String,
    val title: String,
    val description: String,
    @SerializedName("specialtyAreas") val specialtyAreas: List<String>,
    @SerializedName("yearOfStudyMin") val yearOfStudyMin: Int?,
    @SerializedName("eligibilityCriteria") val eligibilityCriteria: String?,
    @SerializedName("applicationDeadline") val applicationDeadline: String?,
    val firm: InternshipFirmDto,
    val isNew: Boolean,
    val tier: Int,           // 0=city, 1=state, 2=national
    @SerializedName("postedAt") val postedAt: String,
)

data class UnreadCountResponse(val count: Int)
