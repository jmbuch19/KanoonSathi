package `in`.kanoonsaathi.app.data.repository

import `in`.kanoonsaathi.app.data.api.*
import `in`.kanoonsaathi.app.util.Result
import `in`.kanoonsaathi.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(private val api: ApiService) {

    suspend fun getModes(): Result<List<ChatModeDto>> = safeApiCall {
        val res = api.getChatModes()
        if (res.isSuccessful) res.body()!!.data!! else throw Exception(res.body()?.error?.message ?: "Failed")
    }

    suspend fun getSuggestedPrompts(): Result<Map<String, List<String>>> = safeApiCall {
        val res = api.getSuggestedPrompts()
        if (res.isSuccessful) res.body()!!.data!! else throw Exception("Failed")
    }

    suspend fun createSession(chatMode: String): Result<ChatSessionDto> = safeApiCall {
        val res = api.createSession(CreateSessionRequest(chatMode))
        if (res.isSuccessful && res.body()?.success == true) res.body()!!.data!!
        else throw Exception(res.body()?.error?.message ?: "Failed to create session")
    }

    suspend fun getSessions(page: Int = 1, limit: Int = 20): Result<SessionsResponse> = safeApiCall {
        val res = api.getSessions(page, limit)
        if (res.isSuccessful) res.body()!!.data!! else throw Exception("Failed")
    }

    suspend fun getSession(id: String): Result<ChatSessionDto> = safeApiCall {
        val res = api.getSession(id)
        if (res.isSuccessful) res.body()!!.data!! else throw Exception("Session not found")
    }

    suspend fun deleteSession(id: String): Result<Unit> = safeApiCall {
        api.deleteSession(id)
    }

    suspend fun toggleBookmark(id: String): Result<Boolean> = safeApiCall {
        val res = api.toggleBookmark(id)
        if (res.isSuccessful) res.body()!!.data!!.isBookmarked else throw Exception("Failed")
    }

    suspend fun sendMessage(sessionId: String, content: String): Result<SendMessageResponse> = safeApiCall {
        val res = api.sendMessage(sessionId, SendMessageRequest(content))
        if (res.isSuccessful && res.body()?.success == true) res.body()!!.data!!
        else throw Exception(res.body()?.error?.message ?: "Failed to send message")
    }

    suspend fun getBookmarks(): Result<List<ChatSessionDto>> = safeApiCall {
        val res = api.getBookmarks()
        if (res.isSuccessful) res.body()!!.data!! else throw Exception("Failed")
    }

    suspend fun reportMessage(messageId: String, reason: String): Result<Unit> = safeApiCall {
        api.reportMessage(ReportRequest(messageId, reason))
    }
}
