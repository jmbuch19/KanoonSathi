package `in`.kanoonsaathi.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.kanoonsaathi.app.ui.theme.*

@Composable
fun LoginScreen(
    role: String = "NONE",
    onOtpSent: (email: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDeep, NavyMedium, NavyLight),
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo
            Text("⚖️", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(8.dp))
            Text("KanoonSaathi", style = MaterialTheme.typography.displayMedium, color = GoldPrimary)
            Text(
                "AI-powered Legal Education",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            )

            // Role badge — shown when coming from WelcomeScreen
            if (role != "NONE") {
                Spacer(Modifier.height(16.dp))
                val (emoji, label, color) = when (role) {
                    "STUDENT" -> Triple("🎓", "Law Student", StudentBlue)
                    "FACULTY" -> Triple("👨‍🏫", "Law Faculty", FacultyGreen)
                    else      -> Triple("🔍", "Curious Learner", CuriousOrange)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = color.copy(alpha = 0.2f),
                ) {
                    Text(
                        "  $emoji  Signing in as $label  ",
                        modifier = Modifier.padding(vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (role == "NONE") "Sign In" else "Create Account",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Enter your email to receive a one-time password",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text("Email address") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboard?.hide()
                            if (uiState.isEmailValid) viewModel.sendOtp(onOtpSent)
                        }),
                        isError = uiState.emailError != null,
                        supportingText = uiState.emailError?.let { { Text(it) } },
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            keyboard?.hide()
                            viewModel.sendOtp(onOtpSent)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = uiState.isEmailValid && !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = GoldPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Send OTP", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text("  or  ", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onGoogleSignIn,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isLoading,
                    ) {
                        Text("🔵  Continue with Google", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "For legal education only. Not legal advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
        }

        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = viewModel::clearError) { Text("Dismiss") } },
            ) { Text(error) }
        }
    }
}
