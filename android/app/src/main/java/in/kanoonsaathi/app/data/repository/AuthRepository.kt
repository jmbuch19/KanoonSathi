package `in`.kanoonsaathi.app.data.repository

import `in`.kanoonsaathi.app.data.api.*
import `in`.kanoonsaathi.app.security.TokenStore
import `in`.kanoonsaathi.app.util.Result
import `in`.kanoonsaathi.app.util.safeApiCall
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore,
) {
    private fun deviceInfo() = Triple(
        Build.ID,
        "${Build.MANUFACTURER} ${Build.MODEL}",
        "1.0.0",
    )

    suspend fun sendOtp(email: String): Result<String> = safeApiCall {
        val res = api.sendOtp(SendOtpRequest(email))
        if (res.isSuccessful && res.body()?.success == true) {
            res.body()?.data?.get("message") ?: "OTP sent"
        } else {
            throw Exception(res.body()?.error?.message ?: "Failed to send OTP")
        }
    }

    suspend fun verifyOtp(email: String, otp: String): Result<AuthResponse> = safeApiCall {
        val (id, model, version) = deviceInfo()
        val res = api.verifyOtp(VerifyOtpRequest(email, otp, id, model, version))
        if (res.isSuccessful && res.body()?.success == true) {
            val data = res.body()!!.data!!
            if (!data.isNewUser && data.accessToken != null && data.refreshToken != null) {
                // Existing user — store session tokens immediately
                tokenStore.saveTokens(data.accessToken, data.refreshToken)
            } else if (data.isNewUser && data.setupToken != null) {
                // New user — store short-lived setup token until role is selected
                tokenStore.saveSetupToken(data.setupToken)
            }
            data
        } else {
            throw Exception(res.body()?.error?.message ?: "OTP verification failed")
        }
    }

    suspend fun googleAuth(idToken: String): Result<AuthResponse> = safeApiCall {
        val (id, model, version) = deviceInfo()
        val res = api.googleAuth(GoogleAuthRequest(idToken, id, model, version))
        if (res.isSuccessful && res.body()?.success == true) {
            val data = res.body()!!.data!!
            if (!data.isNewUser && data.accessToken != null && data.refreshToken != null) {
                tokenStore.saveTokens(data.accessToken, data.refreshToken)
            } else if (data.isNewUser && data.setupToken != null) {
                tokenStore.saveSetupToken(data.setupToken)
            }
            data
        } else {
            throw Exception(res.body()?.error?.message ?: "Google sign-in failed")
        }
    }

    suspend fun setRole(role: String): Result<SetRoleResponse> = safeApiCall {
        val setupToken = tokenStore.getSetupToken()
            ?: throw Exception("No setup token found. Please sign in again.")
        val (id, model, version) = deviceInfo()
        val res = api.setRole("Bearer $setupToken", SetRoleRequest(role, id, model, version))
        if (res.isSuccessful && res.body()?.success == true) {
            val data = res.body()!!.data!!
            tokenStore.saveTokens(data.accessToken, data.refreshToken)
            tokenStore.saveRole(data.role)
            tokenStore.clearSetupToken()
            data
        } else {
            throw Exception(res.body()?.error?.message ?: "Failed to set role")
        }
    }

    suspend fun getMe(): Result<UserDto> = safeApiCall {
        val res = api.getMe()
        if (res.isSuccessful && res.body()?.success == true) {
            val user = res.body()!!.data!!
            tokenStore.saveRole(user.role)
            user
        } else {
            throw Exception(res.body()?.error?.message ?: "Failed to fetch profile")
        }
    }

    suspend fun logout(): Result<Unit> = safeApiCall {
        try { api.logout() } catch (_: Exception) {}
        tokenStore.clear()
    }

    suspend fun deleteAccount(): Result<Unit> = safeApiCall {
        val res = api.deleteAccount()
        if (res.isSuccessful && res.body()?.success == true) {
            tokenStore.clear()
        } else {
            throw Exception(res.body()?.error?.message ?: "Failed to delete account")
        }
    }

    fun isLoggedIn() = tokenStore.isLoggedIn()
    fun getRole() = tokenStore.getRole()
}
