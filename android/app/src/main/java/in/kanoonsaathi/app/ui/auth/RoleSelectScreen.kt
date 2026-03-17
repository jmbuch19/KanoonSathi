package `in`.kanoonsaathi.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.kanoonsaathi.app.ui.theme.*

data class RoleOption(
    val role: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val features: List<String>,
    val color: Color,
)

val roleOptions = listOf(
    RoleOption(
        role = "STUDENT",
        emoji = "📚",
        title = "I am a Law Student",
        subtitle = "LLB / 5-year integrated law",
        features = listOf("Concept explanations", "Case summaries", "Exam prep & quizzes", "Bare Act simplification"),
        color = StudentBlue,
    ),
    RoleOption(
        role = "FACULTY",
        emoji = "🎓",
        title = "I am Law Faculty",
        subtitle = "Professor / Lecturer / Advocate",
        features = listOf("Lecture planning", "Quiz & assignment generation", "Syllabus mapping", "Teaching resources"),
        color = FacultyGreen,
    ),
    RoleOption(
        role = "CURIOUS",
        emoji = "🏛️",
        title = "I am Curious About Law",
        subtitle = "General citizen / Non-lawyer",
        features = listOf("Know your rights", "Legal terms explained", "Everyday law", "Educational only"),
        color = CuriousOrange,
    ),
)

@Composable
fun RoleSelectScreen(
    userId: String,
    onRoleSelected: () -> Unit,
    viewModel: RoleSelectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigateToOnboarding) {
        if (uiState.navigateToOnboarding) {
            viewModel.clearNavigation()
            onRoleSelected()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(52.dp))

        Text("⚖️", fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Who are you?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Text(
            "Choose your role to get a personalised\nlearning experience",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        roleOptions.forEach { option ->
            RoleCard(
                option = option,
                isSelected = uiState.selectedRole == option.role,
                isLoading = uiState.isLoading && uiState.selectedRole == option.role,
                onClick = { viewModel.selectRole(userId, option.role) },
            )
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Your role determines your dashboard and AI behaviour.\nYou can only set this once.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )

        uiState.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RoleCard(
    option: RoleOption,
    isSelected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) option.color else Color(0xFFE5E7EB)
    val bgColor = if (isSelected) option.color.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isLoading, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = CardDefaults.outlinedCardBorder().copy(
            // manually draw border-like appearance via elevation + bg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = option.color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = option.color,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(option.emoji, fontSize = 26.sp)
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    option.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) option.color else TextPrimary,
                )
                Text(
                    option.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(6.dp))
                option.features.take(2).forEach { feature ->
                    Text(
                        "• $feature",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}
