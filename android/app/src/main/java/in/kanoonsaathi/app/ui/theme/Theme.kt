package `in`.kanoonsaathi.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── KanoonSaathi Brand Colors ─────────────────────────────────────────────────
val NavyDeep    = Color(0xFF1A1A2E)   // Primary — dark navy, authority
val NavyMedium  = Color(0xFF16213E)   // Secondary
val NavyLight   = Color(0xFF0F3460)   // Accent
val GoldPrimary = Color(0xFFE8B86D)   // CTA / accent — legal gold
val GoldLight   = Color(0xFFF5D78E)
val SurfaceLight = Color(0xFFF8F9FA)
val SurfaceCard  = Color(0xFFFFFFFF)
val TextPrimary  = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF6B7280)
val TextMuted    = Color(0xFF9CA3AF)
val ErrorRed     = Color(0xFFDC2626)
val SuccessGreen = Color(0xFF16A34A)
val WarningAmber = Color(0xFFF59E0B)

// Role colors
val StudentBlue  = Color(0xFF2563EB)
val FacultyGreen = Color(0xFF059669)
val CuriousOrange = Color(0xFFD97706)

private val LightColorScheme = lightColorScheme(
    primary          = NavyDeep,
    onPrimary        = Color.White,
    primaryContainer = NavyLight,
    onPrimaryContainer = Color.White,
    secondary        = GoldPrimary,
    onSecondary      = NavyDeep,
    secondaryContainer = GoldLight,
    onSecondaryContainer = NavyDeep,
    background       = SurfaceLight,
    onBackground     = TextPrimary,
    surface          = SurfaceCard,
    onSurface        = TextPrimary,
    surfaceVariant   = Color(0xFFEEF2FF),
    onSurfaceVariant = TextSecondary,
    error            = ErrorRed,
    onError          = Color.White,
    outline          = Color(0xFFD1D5DB),
)

@Composable
fun KanoonSaathiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = KanoonTypography,
        content = content,
    )
}
