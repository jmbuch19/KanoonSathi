package `in`.kanoonsaathi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import `in`.kanoonsaathi.app.security.TokenStore
import `in`.kanoonsaathi.app.ui.auth.*
import `in`.kanoonsaathi.app.ui.bookmarks.BookmarksScreen
import `in`.kanoonsaathi.app.ui.chat.ChatCreateScreen
import `in`.kanoonsaathi.app.ui.chat.ChatScreen
import `in`.kanoonsaathi.app.ui.dashboard.DashboardScreen
import `in`.kanoonsaathi.app.ui.history.ChatHistoryScreen
import `in`.kanoonsaathi.app.ui.navigation.Routes
import `in`.kanoonsaathi.app.ui.onboarding.OnboardingScreen
import `in`.kanoonsaathi.app.ui.settings.SettingsScreen
import `in`.kanoonsaathi.app.ui.legal.PrivacyPolicyScreen
import `in`.kanoonsaathi.app.ui.theme.KanoonSaathiTheme
import `in`.kanoonsaathi.app.ui.welcome.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination = if (tokenStore.isLoggedIn()) Routes.DASHBOARD else Routes.WELCOME

        setContent {
            KanoonSaathiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                    ) {

                        // ── Welcome ───────────────────────────────────────────
                        composable(Routes.WELCOME) {
                            WelcomeScreen(
                                onRoleSelected = { role ->
                                    navController.navigate(Routes.login(role))
                                },
                            )
                        }

                        // ── Login ─────────────────────────────────────────────
                        composable(Routes.LOGIN) { backStack ->
                            val role = backStack.arguments?.getString("role") ?: "NONE"
                            LoginScreen(
                                role = role,
                                onOtpSent = { email ->
                                    navController.navigate(Routes.otpVerify(email, role))
                                },
                                onGoogleSignIn = {
                                    // Wire Google Credential Manager here later
                                },
                            )
                        }

                        // ── OTP ───────────────────────────────────────────────
                        composable(Routes.OTP_VERIFY) { backStack ->
                            val email = backStack.arguments?.getString("email") ?: ""
                            val role  = backStack.arguments?.getString("role") ?: "NONE"
                            OtpScreen(
                                email = email,
                                role = role,
                                onNavigation = { nav ->
                                    when (nav) {
                                        is OtpNavigation.Dashboard -> navController.navigate(Routes.DASHBOARD) {
                                            popUpTo(Routes.WELCOME) { inclusive = true }
                                        }
                                        is OtpNavigation.Onboarding -> navController.navigate(Routes.ONBOARDING) {
                                            popUpTo(Routes.WELCOME) { inclusive = true }
                                        }
                                        is OtpNavigation.RoleSelect -> navController.navigate(Routes.roleSelect(nav.userId)) {
                                            popUpTo(Routes.WELCOME) { inclusive = true }
                                        }
                                    }
                                },
                                onBack = { navController.popBackStack() },
                            )
                        }

                        // ── Role select (fallback if no pre-selected role) ─────
                        composable(Routes.ROLE_SELECT) { backStack ->
                            val userId = backStack.arguments?.getString("userId") ?: ""
                            RoleSelectScreen(
                                userId = userId,
                                onRoleSelected = {
                                    navController.navigate(Routes.ONBOARDING) {
                                        popUpTo(Routes.ROLE_SELECT) { inclusive = true }
                                    }
                                },
                            )
                        }

                        // ── Onboarding ────────────────────────────────────────
                        composable(Routes.ONBOARDING) {
                            OnboardingScreen(
                                onComplete = {
                                    navController.navigate(Routes.DASHBOARD) {
                                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                                    }
                                },
                            )
                        }

                        // ── Dashboard ─────────────────────────────────────────
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                onStartChat = { modeId, modeName ->
                                    navController.navigate("chat_create/$modeId/$modeName")
                                },
                                onChatHistory = { navController.navigate(Routes.CHAT_HISTORY) },
                                onBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                                onSettings = { navController.navigate(Routes.SETTINGS) },
                            )
                        }

                        // ── Chat create / chat ────────────────────────────────
                        composable("chat_create/{modeId}/{modeName}") { backStack ->
                            val modeId = backStack.arguments?.getString("modeId") ?: ""
                            val modeName = backStack.arguments?.getString("modeName") ?: ""
                            ChatCreateScreen(
                                modeId = modeId,
                                modeName = modeName,
                                onSessionCreated = { sessionId ->
                                    navController.navigate(Routes.chat(sessionId, modeId)) {
                                        popUpTo("chat_create/$modeId/$modeName") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(Routes.CHAT) { backStack ->
                            val sessionId = backStack.arguments?.getString("sessionId") ?: ""
                            val chatMode  = backStack.arguments?.getString("chatMode") ?: ""
                            ChatScreen(
                                sessionId = sessionId,
                                chatModeName = chatMode,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        // ── History ───────────────────────────────────────────
                        composable(Routes.CHAT_HISTORY) {
                            ChatHistoryScreen(
                                onBack = { navController.popBackStack() },
                                onOpenChat = { sessionId, chatMode ->
                                    navController.navigate(Routes.chat(sessionId, chatMode))
                                },
                            )
                        }

                        // ── Bookmarks ─────────────────────────────────────────
                        composable(Routes.BOOKMARKS) {
                            BookmarksScreen(
                                onBack = { navController.popBackStack() },
                                onOpenChat = { sessionId, chatMode ->
                                    navController.navigate(Routes.chat(sessionId, chatMode))
                                },
                            )
                        }

                        // ── Settings ──────────────────────────────────────────
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onLoggedOut = {
                                    navController.navigate(Routes.WELCOME) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onPrivacyPolicy = {
                                    navController.navigate(Routes.PRIVACY_POLICY)
                                },
                            )
                        }

                        // ── Privacy Policy ────────────────────────────────────
                        composable(Routes.PRIVACY_POLICY) {
                            PrivacyPolicyScreen(
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
