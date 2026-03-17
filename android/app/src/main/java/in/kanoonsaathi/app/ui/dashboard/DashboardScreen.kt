package `in`.kanoonsaathi.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import android.net.Uri
import `in`.kanoonsaathi.app.data.api.ChatModeDto
import `in`.kanoonsaathi.app.data.api.InternshipPostingDto
import `in`.kanoonsaathi.app.data.api.NewsItemDto
import `in`.kanoonsaathi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onStartChat: (modeId: String, modeName: String) -> Unit,
    onChatHistory: () -> Unit,
    onBookmarks: () -> Unit,
    onSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "KanoonSaathi",
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            uiState.roleLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onBookmarks) {
                        Icon(Icons.Outlined.Bookmarks, "Bookmarks", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, "Settings", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDeep),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Outlined.Home, "Home") },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onChatHistory,
                    icon = { Icon(Icons.Outlined.ChatBubbleOutline, "History") },
                    label = { Text("History") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onBookmarks,
                    icon = { Icon(Icons.Outlined.Bookmarks, "Saved") },
                    label = { Text("Saved") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onSettings,
                    icon = { Icon(Icons.Outlined.Person, "Profile") },
                    label = { Text("Profile") },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // Greeting header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyDeep)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Column {
                        Text(
                            uiState.greeting,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "What would you like to explore today?",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )

                        // Usage pill
                        if (uiState.todayMessages > 0) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    "  ${uiState.todayMessages} messages today  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Chat modes grid
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "  Start a New Chat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(12.dp))
            }

            if (uiState.isLoadingModes) {
                item {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NavyDeep)
                    }
                }
            } else {
                items(uiState.modes.chunked(2)) { rowModes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowModes.forEach { mode ->
                            ChatModeCard(
                                mode = mode,
                                modifier = Modifier.weight(1f),
                                onClick = { onStartChat(mode.id, mode.name) },
                            )
                        }
                        if (rowModes.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Suggested prompts
            if (uiState.suggestedPrompts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "  Suggested Questions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        uiState.suggestedPrompts.forEach { (modeId, prompts) ->
                            items(prompts.take(3)) { prompt ->
                                SuggestedPromptChip(
                                    text = prompt,
                                    onClick = {
                                        val mode = uiState.modes.find { it.id == modeId }
                                        if (mode != null) onStartChat(mode.id, mode.name)
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            // Recent chats
            if (uiState.recentSessions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Recent Chats",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = onChatHistory) {
                            Text("See all", color = NavyDeep)
                        }
                    }
                }
                items(uiState.recentSessions.take(3)) { session ->
                    RecentChatItem(
                        title = session.title ?: session.chatMode.replace("_", " ").replaceFirstChar { it.uppercase() },
                        mode = session.chatMode,
                        messageCount = session.messageCount,
                        onClick = { onStartChat(session.chatMode, session.chatMode) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            // ── Career Opportunities ────────────────────────────────────────
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💼", fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Internship Opportunities",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                    }
                    if (uiState.unreadOpportunities > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFD32F2F),
                        ) {
                            Text(
                                "  ${uiState.unreadOpportunities} NEW  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 3.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Feature intro banner — permanent, explains the bridge concept
                OpportunityFeatureBanner()
                Spacer(Modifier.height(10.dp))
            }

            if (uiState.isLoadingOpportunities) {
                items(2) {
                    OpportunityCardSkeleton(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    )
                }
            } else if (uiState.opportunities.isNotEmpty()) {
                items(uiState.opportunities.take(5)) { opp ->
                    OpportunityCard(
                        posting = opp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No opportunities yet — check back soon!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Legal News Feed ──────────────────────────────────────────────
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📰", fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Legal News",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NavyDeep.copy(alpha = 0.08f),
                    ) {
                        Text(
                            "  LIVE  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = NavyDeep,
                            modifier = Modifier.padding(vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (uiState.isLoadingNews) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(3) {
                            NewsCardSkeleton()
                        }
                    }
                }
            } else if (uiState.newsFeed.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.newsFeed) { newsItem ->
                            NewsCard(
                                item = newsItem,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.url))
                                    context.startActivity(intent)
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "News unavailable. Check back later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatModeCard(mode: ChatModeDto, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(mode.icon, fontSize = 28.sp)
            Column {
                Text(
                    mode.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Text(
                    mode.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun SuggestedPromptChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NavyDeep.copy(alpha = 0.08f),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .widthIn(max = 200.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = NavyDeep,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun RecentChatItem(
    title: String,
    mode: String,
    messageCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = NavyDeep.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                Text("$messageCount messages", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Category badge colour ──────────────────────────────────────────────────────
private fun categoryColor(category: String): Color = when (category) {
    "Supreme Court"  -> Color(0xFF1A237E)  // deep indigo
    "High Court"     -> Color(0xFF006064)  // teal
    "Constitutional" -> Color(0xFF4A148C)  // deep purple
    "Exam"           -> Color(0xFFE65100)  // deep orange
    "Criminal"       -> Color(0xFFB71C1C)  // deep red
    "Corporate"      -> Color(0xFF1B5E20)  // deep green
    "Family"         -> Color(0xFF880E4F)  // pink
    "Labour"         -> Color(0xFF0D47A1)  // blue
    else             -> Color(0xFF37474F)  // blue-grey
}

private fun timeAgo(isoDate: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(isoDate) ?: return ""
        val diffMs = System.currentTimeMillis() - date.time
        val mins = diffMs / 60_000
        when {
            mins < 1   -> "just now"
            mins < 60  -> "${mins}m ago"
            mins < 1440 -> "${mins / 60}h ago"
            else       -> "${mins / 1440}d ago"
        }
    } catch (_: Exception) { "" }
}

@Composable
private fun NewsCard(item: NewsItemDto, onClick: () -> Unit) {
    val catColor = categoryColor(item.category)

    Card(
        modifier = Modifier
            .width(270.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Category + source row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = catColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = catColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(
                    timeAgo(item.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Title
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 3,
            )

            Spacer(Modifier.height(6.dp))

            // Summary
            Text(
                item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
            )

            Spacer(Modifier.height(10.dp))

            // Footer: source + read icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    item.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = NavyDeep.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Read",
                        style = MaterialTheme.typography.labelSmall,
                        color = NavyDeep,
                    )
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = "Open in browser",
                        tint = NavyDeep,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NewsCardSkeleton() {
    Card(
        modifier = Modifier
            .width(270.dp)
            .height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .width(80.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.15f))
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.12f))
            )
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.10f))
            )
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.08f))
            )
        }
    }
}

// ── Internship Opportunity Composables ────────────────────────────────────────

@Composable
private fun OpportunityFeatureBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🏛️", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Verified Law Firms. Real Opportunities.",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4A148C),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "KanoonSaathi bridges serious law students with verified legal practices. " +
                    "We are a direct academic-to-professional bridge — not a manpower provider.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6A1B9A).copy(alpha = 0.85f),
                    maxLines = 3,
                )
            }
        }
    }
}

@Composable
private fun OpportunityCard(posting: InternshipPostingDto, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val tierLabel = when (posting.tier) {
        0    -> "📍 ${posting.firm.city}"
        1    -> "📍 ${posting.firm.state}"
        else -> "🇮🇳 Nationwide"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Header: firm name + title + NEW badge ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        posting.firm.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = NavyDeep.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        posting.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (posting.isNew) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD32F2F),
                        ) {
                            Text(
                                "NEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        tierLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Specialty chips ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                posting.specialtyAreas.take(3).forEach { area ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NavyDeep.copy(alpha = 0.08f),
                    ) {
                        Text(
                            area,
                            style = MaterialTheme.typography.labelSmall,
                            color = NavyDeep,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Year + deadline ────────────────────────────────────────────────
            val yearText    = posting.yearOfStudyMin?.let { "Year $it+ students" } ?: "All years welcome"
            val deadlineText = posting.applicationDeadline?.take(10)?.let { "Deadline: $it" } ?: "Open"
            Text(
                "$yearText  •  $deadlineText",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )

            // ── Description preview ────────────────────────────────────────────
            if (posting.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    posting.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Footer: firm specialties + visit link ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    posting.firm.specialties.take(2).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                if (!posting.firm.website.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(posting.firm.website))
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            "Visit Firm ›",
                            style = MaterialTheme.typography.labelMedium,
                            color = NavyDeep,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpportunityCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(126.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = 0.12f)))
            Box(Modifier.fillMaxWidth(0.7f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = 0.15f)))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(70.dp).height(22.dp).clip(RoundedCornerShape(6.dp)).background(Color.Gray.copy(alpha = 0.10f)))
                Box(Modifier.width(90.dp).height(22.dp).clip(RoundedCornerShape(6.dp)).background(Color.Gray.copy(alpha = 0.08f)))
            }
            Box(Modifier.fillMaxWidth(0.5f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = 0.07f)))
        }
    }
}
