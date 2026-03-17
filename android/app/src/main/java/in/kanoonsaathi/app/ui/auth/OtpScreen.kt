package `in`.kanoonsaathi.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.kanoonsaathi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    email: String,
    role: String = "NONE",
    onNavigation: (OtpNavigation) -> Unit,
    onBack: () -> Unit,
    viewModel: OtpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.setEmailAndRole(email, role)
        focusRequester.requestFocus()
    }

    LaunchedEffect(uiState.navigation) {
        uiState.navigation?.let { nav ->
            viewModel.clearNavigation()
            onNavigation(nav)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            Text("⚖️", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))

            Text("Check your email", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "We sent a 6-digit code to\n$email",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            // OTP input — hidden field drives 6 visible boxes
            BasicTextField(
                value = uiState.otp,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) viewModel.onOtpChange(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.focusRequester(focusRequester),
                cursorBrush = SolidColor(NavyDeep),
                decorationBox = {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(6) { index ->
                            val char = uiState.otp.getOrNull(index)
                            val isCurrent = index == uiState.otp.length
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = when {
                                            uiState.isError -> ErrorRed
                                            isCurrent      -> NavyDeep
                                            char != null   -> GoldPrimary
                                            else           -> Color(0xFFD1D5DB)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .background(
                                        color = if (char != null) NavyDeep.copy(alpha = 0.04f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = char?.toString() ?: "",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDeep,
                                )
                            }
                        }
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            if (uiState.isError) {
                Text("Incorrect code. Please try again.", color = ErrorRed, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { viewModel.verifyOtp() },
                enabled = uiState.otp.length == 6 && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = GoldPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Verify & Continue", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Didn't receive it? ", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                TextButton(
                    onClick = { viewModel.resendOtp() },
                    enabled = uiState.resendCooldown == 0,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        if (uiState.resendCooldown > 0) "Resend in ${uiState.resendCooldown}s" else "Resend code",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.resendCooldown > 0) TextMuted else NavyDeep,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Code expires in 5 minutes", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}
