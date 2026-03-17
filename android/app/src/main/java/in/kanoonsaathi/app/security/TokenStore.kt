package `in`.kanoonsaathi.app.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure token storage using Android Keystore-backed EncryptedSharedPreferences.
 * AES256-GCM encryption. Keys never leave the Keystore hardware.
 * This is the ONLY place tokens are read or written — never in plain SharedPreferences.
 */
@Singleton
class TokenStore @Inject constructor(@ApplicationContext context: Context) {

    private val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs = EncryptedSharedPreferences.create(
        "ks_auth_secure",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply()
    }

    fun saveUserId(userId: String) = prefs.edit().putString(KEY_USER_ID, userId).apply()
    fun saveRole(role: String) = prefs.edit().putString(KEY_ROLE, role).apply()

    // Setup token — short-lived JWT for new users before role selection
    fun saveSetupToken(token: String) = prefs.edit().putString(KEY_SETUP_TOKEN, token).apply()
    fun getSetupToken(): String? = prefs.getString(KEY_SETUP_TOKEN, null)
    fun clearSetupToken() = prefs.edit().remove(KEY_SETUP_TOKEN).apply()

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun isLoggedIn(): Boolean = getAccessToken() != null && getRefreshToken() != null

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS      = "access_token"
        private const val KEY_REFRESH     = "refresh_token"
        private const val KEY_USER_ID     = "user_id"
        private const val KEY_ROLE        = "user_role"
        private const val KEY_SETUP_TOKEN = "setup_token"
    }
}
