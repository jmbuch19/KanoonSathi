package `in`.kanoonsaathi.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.kanoonsaathi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.loggedOut) {
        if (uiState.loggedOut) onLoggedOut()
    }

    LaunchedEffect(uiState.accountDeleted) {
        if (uiState.accountDeleted) onLoggedOut()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out?") },
            text = { Text("You'll need to sign in again to access KanoonSaathi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                ) {
                    Text("Sign Out", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            userName = uiState.user?.let { user ->
                when (user.role) {
                    "STUDENT" -> user.studentProfile?.fullName
                    "FACULTY" -> user.facultyProfile?.fullName
                    "CURIOUS" -> user.curiousProfile?.displayName
                    else -> null
                }
            },
            isDeleting = uiState.isDeletingAccount,
            error = uiState.error,
            onConfirm = { viewModel.deleteAccount() },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White)
                    }
                },
                title = { Text("Profile & Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDeep),
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = NavyDeep) }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight)
                .verticalScroll(rememberScrollState()),
        ) {
            // Profile header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyDeep)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    // Avatar initials circle
                    val initials = uiState.user?.let { user ->
                        val name = when (user.role) {
                            "STUDENT" -> user.studentProfile?.fullName
                            "FACULTY" -> user.facultyProfile?.fullName
                            "CURIOUS" -> user.curiousProfile?.displayName
                            else -> null
                        }
                        name?.split(" ")?.take(2)?.joinToString("") { it.take(1).uppercase() }
                            ?: user.email.take(2).uppercase()
                    } ?: "?"

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            initials,
                            color = NavyDeep,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    val displayName = uiState.user?.let { user ->
                        when (user.role) {
                            "STUDENT" -> user.studentProfile?.fullName
                            "FACULTY" -> user.facultyProfile?.fullName
                            "CURIOUS" -> user.curiousProfile?.displayName
                            else -> null
                        }
                    }

                    if (displayName != null) {
                        Text(displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(2.dp))
                    }

                    Text(uiState.user?.email ?: "", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    // Role badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = when (uiState.user?.role) {
                                "STUDENT" -> "🎓 Law Student"
                                "FACULTY" -> "👨‍🏫 Law Faculty"
                                "CURIOUS" -> "🔍 Curious Learner"
                                else -> ""
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // Usage card
            uiState.usage?.let { usage ->
                Spacer(Modifier.height(16.dp))
                SectionHeader("Today's Usage")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        UsageStat(
                            label = "Messages",
                            value = usage.today.messages.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        VerticalDivider(modifier = Modifier.height(40.dp).align(Alignment.CenterVertically))
                        UsageStat(
                            label = "Plan",
                            value = usage.plan.replaceFirstChar(Char::uppercase),
                            modifier = Modifier.weight(1f),
                        )
                        VerticalDivider(modifier = Modifier.height(40.dp).align(Alignment.CenterVertically))
                        UsageStat(
                            label = "Tokens",
                            value = if (usage.today.tokens > 1000) "${usage.today.tokens / 1000}k" else usage.today.tokens.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Profile details
            uiState.user?.let { user ->
                Spacer(Modifier.height(16.dp))
                SectionHeader("Profile Details")
                SettingsGroup {
                    when (user.role) {
                        "STUDENT" -> user.studentProfile?.let { p ->
                            SettingsInfoRow(Icons.Outlined.School, "College", p.collegeName ?: "-")
                            SettingsInfoRow(Icons.Outlined.MenuBook, "Year / Semester", "Year ${p.yearOfStudy ?: "-"} · Sem ${p.semester ?: "-"}")
                            if (!p.examTarget.isNullOrBlank()) {
                                SettingsInfoRow(Icons.Outlined.EmojiEvents, "Exam Target", p.examTarget)
                            }
                        }
                        "FACULTY" -> user.facultyProfile?.let { p ->
                            SettingsInfoRow(Icons.Outlined.AccountBalance, "Institution", p.institutionName ?: "-")
                            SettingsInfoRow(Icons.Outlined.Work, "Designation", p.designation ?: "-")
                        }
                        "CURIOUS" -> SettingsInfoRow(Icons.Outlined.Person, "Account Type", "General User")
                    }
                    SettingsInfoRow(Icons.Outlined.Email, "Email", user.email)
                }
            }

            // App info
            Spacer(Modifier.height(16.dp))
            SectionHeader("About")
            SettingsGroup {
                SettingsInfoRow(Icons.Outlined.Info, "Version", "1.0.0")
                SettingsInfoRow(Icons.Outlined.Gavel, "Disclaimer", "Educational use only")
                SettingsNavRow(Icons.Outlined.Lock, "Privacy Policy", onClick = onPrivacyPolicy)
            }

            // Sign out
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            "Sign Out",
                            color = ErrorRed,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = ErrorRed,
                        )
                    },
                    trailingContent = {
                        if (uiState.isLoggingOut) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ErrorRed, strokeWidth = 2.dp)
                        }
                    },
                    modifier = Modifier.clickableIfEnabled(!uiState.isLoggingOut) { showLogoutDialog = true },
                )
            }

            // ── Delete Account ─────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            SectionHeader("Danger Zone")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F1)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Delete My Account",
                                fontWeight = FontWeight.SemiBold,
                                color = ErrorRed,
                                fontSize = 14.sp,
                            )
                            Text(
                                "Permanently erases all your data — chats, profile, bookmarks, and usage history. This cannot be undone. A confirmation email will be sent to you.",
                                fontSize = 12.sp,
                                color = Color(0xFF9B1C1C),
                                lineHeight = 17.sp,
                            )
                        }
                    }
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isDeletingAccount && !uiState.isLoggingOut,
                    ) {
                        if (uiState.isDeletingAccount) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Deleting…")
                        } else {
                            Icon(
                                Icons.Outlined.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Delete My Account", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, label: String, value: String) {
    ListItem(
        headlineContent = { Text(value, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = { Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted) },
        leadingContent = { Icon(icon, null, tint = NavyDeep.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 0.5.dp)
}

@Composable
private fun UsageStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyDeep)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

// Extension to conditionally add clickable
private fun Modifier.clickableIfEnabled(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) this.then(Modifier.clickable(onClick = onClick)) else this

@Composable
private fun SettingsNavRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
        },
        leadingContent = {
            Icon(icon, null, tint = NavyDeep.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        },
        trailingContent = {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 0.5.dp)
}

// ── Delete Account Confirmation Dialog ─────────────────────────────────────────

@Composable
private fun DeleteAccountDialog(
    userName: String?,
    isDeleting: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Icon(
                Icons.Outlined.DeleteForever,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                "Delete Account Permanently?",
                fontWeight = FontWeight.Bold,
                color = ErrorRed,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        },
        text = {
            Column {
                if (userName != null) {
                    Text(
                        "Hi $userName, this action is irreversible.",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    "The following will be permanently deleted:",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                val items = listOf(
                    "Profile & account information",
                    "All chat sessions & messages",
                    "Bookmarks & saved items",
                    "Usage history & analytics",
                    "Device session records",
                )
                items.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(ErrorRed, CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(item, fontSize = 13.sp, color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "A confirmation email will be sent to your registered address after deletion.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 17.sp,
                )
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        error,
                        fontSize = 12.sp,
                        color = ErrorRed,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                enabled = !isDeleting,
                shape = RoundedCornerShape(8.dp),
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Deleting…")
                } else {
                    Text("Yes, Delete Everything", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = Color.White,
    )
}
