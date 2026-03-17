package `in`.kanoonsaathi.app.ui.welcome

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import `in`.kanoonsaathi.app.ui.theme.*

@Composable
fun WelcomeScreen(
    onRoleSelected: (role: String) -> Unit,
) {
    val scrollState = rememberScrollState()
    var disclaimerExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {

            // ── Hero ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(NavyDeep, NavyMedium),
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚖️", fontSize = 56.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "KanoonSaathi",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        "Your AI Legal Companion",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = GoldPrimary.copy(alpha = 0.15f),
                    ) {
                        Text(
                            "  Educational Use Only · Not Legal Advice  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // ── Info card ─────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(SurfaceLight)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {

                InfoSection(
                    emoji = "🇮🇳",
                    title = "What is KanoonSaathi?",
                    content = "KanoonSaathi (\"Law Companion\") is an AI-powered legal education platform built for India. " +
                        "We make Indian law accessible, understandable, and useful for everyone — " +
                        "from law students cramming for exams to ordinary citizens who want to know their rights.",
                )

                InfoSection(
                    emoji = "✅",
                    title = "What we do",
                    bullets = listOf(
                        "Explain Indian laws in simple, clear language",
                        "Help law students study IPC, CrPC, Constitution, and more",
                        "Assist faculty with research and case study materials",
                        "Answer everyday legal questions for curious citizens",
                        "Summarise landmark Supreme Court judgments",
                        "Help you understand FIRs, contracts, and legal documents",
                    ),
                )

                // Expandable "What we do NOT do" section
                InfoSectionExpandable(
                    emoji = "🚫",
                    title = "What we do NOT do",
                    expanded = disclaimerExpanded,
                    onToggle = { disclaimerExpanded = !disclaimerExpanded },
                    bullets = listOf(
                        "We do NOT provide legal advice for your specific situation",
                        "We do NOT represent you in any legal matter",
                        "We do NOT replace a qualified advocate or lawyer",
                        "We do NOT guarantee accuracy for your personal legal case",
                        "We do NOT connect you with lawyers or courts",
                        "We are NOT a legal services platform under the Advocates Act",
                    ),
                )

                // Legal disclaimer box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarningAmber.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.3f)),
                ) {
                    Row(modifier = Modifier.padding(14.dp)) {
                        Text("⚠️", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Legal Disclaimer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF92400E),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "All information on KanoonSaathi is for educational and general awareness purposes only. " +
                                    "It does not constitute legal advice. For legal matters affecting you personally, " +
                                    "always consult a qualified advocate registered with the Bar Council of India.",
                                fontSize = 12.sp,
                                color = Color(0xFF78350F),
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                // ── CTA section ───────────────────────────────────────────────
                Text(
                    "Choose how you want to use KanoonSaathi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Your experience is personalised to your role.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(4.dp))

                RoleCard(
                    emoji = "🎓",
                    title = "I'm a Law Student",
                    subtitle = "Study IPC, CrPC, Constitution & more. Ace your exams with AI.",
                    features = listOf("Case analysis", "Exam prep", "Moot court help"),
                    color = StudentBlue,
                    onClick = { onRoleSelected("STUDENT") },
                )

                RoleCard(
                    emoji = "👨‍🏫",
                    title = "I'm Law Faculty",
                    subtitle = "Research assistance, lesson planning & academic references.",
                    features = listOf("Research tool", "Judgment summaries", "Teaching notes"),
                    color = FacultyGreen,
                    onClick = { onRoleSelected("FACULTY") },
                )

                RoleCard(
                    emoji = "🔍",
                    title = "I'm Curious About Law",
                    subtitle = "Understand your rights, legal processes & everyday law.",
                    features = listOf("Know your rights", "Legal concepts", "Plain language"),
                    color = CuriousOrange,
                    onClick = { onRoleSelected("CURIOUS") },
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InfoSection(
    emoji: String,
    title: String,
    content: String? = null,
    bullets: List<String>? = null,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
        }
        Spacer(Modifier.height(6.dp))
        content?.let {
            Text(it, fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp)
        }
        bullets?.forEach { bullet ->
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text("•  ", fontSize = 13.sp, color = NavyDeep, fontWeight = FontWeight.Bold)
                Text(bullet, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun InfoSectionExpandable(
    emoji: String,
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    bullets: List<String>,
) {
    Column {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggle)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                bullets.forEach { bullet ->
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        Text("•  ", fontSize = 13.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
                        Text(bullet, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp)
                    }
                }
            }
        }

        if (!expanded) {
            TextButton(
                onClick = onToggle,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            ) {
                Text("Read more ↓", style = MaterialTheme.typography.labelSmall, color = NavyDeep)
            }
        }
    }
}

@Composable
private fun RoleCard(
    emoji: String,
    title: String,
    subtitle: String,
    features: List<String>,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left — Emoji + accent bar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 26.sp)
            }

            Spacer(Modifier.width(14.dp))

            // Middle — text
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    features.forEach { feature ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = color.copy(alpha = 0.1f),
                        ) {
                            Text(
                                feature,
                                fontSize = 10.sp,
                                color = color,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Right — arrow
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
