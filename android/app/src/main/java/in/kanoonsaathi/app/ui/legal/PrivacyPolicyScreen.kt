package `in`.kanoonsaathi.app.ui.legal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.kanoonsaathi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val policy = KANOONSAATHI_PRIVACY_POLICY

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                title = {
                    Text(
                        "Privacy Policy",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDeep),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            // ── Header card ────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyDeep),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Outlined.GppGood,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            policy.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            policy.platform,
                            color = GoldPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(12.dp))
                        // Meta chips row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MetaChip("v${policy.version}")
                            MetaChip("Effective: ${policy.effectiveDate}")
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Governed by: ${policy.governingLaw}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // ── Compliance notice ──────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Outlined.GppGood,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp).padding(top = 2.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "DPDP Act 2023 Compliant",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D4ED8),
                            )
                            Text(
                                "This policy is prepared in accordance with the Digital Personal Data Protection Act, 2023 (India). Last updated: ${policy.lastUpdated}.",
                                fontSize = 12.sp,
                                color = Color(0xFF1E40AF),
                                lineHeight = 17.sp,
                            )
                        }
                    }
                }
            }

            // ── Policy sections ────────────────────────────────────────────────
            items(policy.sections) { section ->
                PolicySectionBlock(section)
            }
        }
    }
}

// ── Section block ──────────────────────────────────────────────────────────────

@Composable
private fun PolicySectionBlock(section: PolicySection) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section number + title
            Row(verticalAlignment = Alignment.Top) {
                // Number bubble
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(NavyDeep, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        section.id,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    section.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    lineHeight = 21.sp,
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(Modifier.height(10.dp))

            // Content paragraphs
            section.content.forEach { para ->
                Text(
                    para,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp,
                )
                Spacer(Modifier.height(8.dp))
            }

            // Subsections
            section.subsections.forEach { sub ->
                if (sub.title != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        sub.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyLight,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                sub.points.forEach { point ->
                    Row(
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            "•",
                            fontSize = 13.sp,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 1.dp, end = 8.dp),
                        )
                        Text(
                            point,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Contact block (Section 12)
            section.contact?.let { contact ->
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        ContactRow(
                            icon = { Icon(Icons.Outlined.Person, null, tint = SuccessGreen, modifier = Modifier.size(16.dp)) },
                            label = "Contact",
                            value = contact.name,
                        )
                        Spacer(Modifier.height(8.dp))
                        ContactRow(
                            icon = { Icon(Icons.Outlined.Email, null, tint = SuccessGreen, modifier = Modifier.size(16.dp)) },
                            label = "Email",
                            value = contact.email,
                            isClickable = true,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${contact.email}")
                                    putExtra(Intent.EXTRA_SUBJECT, "Privacy Request — KanoonSaathi")
                                }
                                context.startActivity(intent)
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        ContactRow(
                            icon = { Icon(Icons.Outlined.LocationOn, null, tint = SuccessGreen, modifier = Modifier.size(16.dp)) },
                            label = "Address",
                            value = contact.address,
                        )
                        Spacer(Modifier.height(8.dp))
                        ContactRow(
                            icon = { Icon(Icons.Outlined.Schedule, null, tint = SuccessGreen, modifier = Modifier.size(16.dp)) },
                            label = "Response Time",
                            value = contact.responseTimeframe,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    isClickable: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.Top) {
        icon()
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                label,
                fontSize = 10.sp,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
            if (isClickable && onClick != null) {
                TextButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(20.dp),
                ) {
                    Text(
                        value,
                        fontSize = 13.sp,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Medium,
                    )
                }
            } else {
                Text(
                    value,
                    fontSize = 13.sp,
                    color = Color(0xFF15803D),
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.15f),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
