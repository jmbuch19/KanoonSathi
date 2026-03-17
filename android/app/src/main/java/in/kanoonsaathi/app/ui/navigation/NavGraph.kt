package `in`.kanoonsaathi.app.ui.navigation

object Routes {
    const val WELCOME        = "welcome"
    const val LOGIN          = "login/{role}"        // role: STUDENT | FACULTY | CURIOUS | NONE
    const val OTP_VERIFY     = "otp_verify/{email}/{role}"
    const val ROLE_SELECT    = "role_select/{userId}"
    const val ONBOARDING     = "onboarding"
    const val DASHBOARD      = "dashboard"
    const val CHAT           = "chat/{sessionId}/{chatMode}"
    const val CHAT_HISTORY   = "chat_history"
    const val BOOKMARKS      = "bookmarks"
    const val SETTINGS       = "settings"
    const val PRIVACY_POLICY = "privacy_policy"

    fun login(role: String = "NONE") = "login/$role"
    fun otpVerify(email: String, role: String = "NONE") = "otp_verify/$email/$role"
    fun roleSelect(userId: String) = "role_select/$userId"
    fun chat(sessionId: String, chatMode: String) = "chat/$sessionId/$chatMode"
}
