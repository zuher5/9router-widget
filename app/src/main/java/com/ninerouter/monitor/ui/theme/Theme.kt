package com.ninerouter.monitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Palet warna resmi 9Router diambil dari src/app/globals.css
// Brand color: #E56A4A (Warm terracotta / coral)
val NineRouterBrand = Color(0xFFE56A4A)

// Status colors
val ColorSuccess = Color(0xFF10B981)
val ColorSuccessDark = Color(0xFF22C55E)
val ColorDanger = Color(0xFFCF222E)
val ColorDangerDark = Color(0xFFEF4444)
val ColorWarning = Color(0xFFF59E0B)
val ColorWarningDark = Color(0xFFFBBF24)
val ColorInfo = Color(0xFF3B82F6)
val ColorInfoDark = Color(0xFF60A5FA)

// Light theme: bg #FDFAF6, surface #FFFFFF, text #0A0A0A, muted #6B7280, border #E5E7EB
private val LightColorScheme = lightColorScheme(
    primary = NineRouterBrand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBE5),
    onPrimaryContainer = Color(0xFF5A1E10),
    secondary = Color(0xFF3B82F6),
    onSecondary = Color.White,
    background = Color(0xFFFDFAF6),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFF7F3EE),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE5E7EB),
    outlineVariant = Color(0xFFE5E7EB),
    error = ColorDanger,
    onError = Color.White
)

// Dark theme: bg #1A1A1A, surface #262626, text #EDEDED, muted #9CA3AF, border #333333
private val DarkColorScheme = darkColorScheme(
    primary = NineRouterBrand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A1F16),
    onPrimaryContainer = Color(0xFFFFDCD2),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color.Black,
    background = Color(0xFF1A1A1A),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF262626),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF1F1F1E),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF2E2E2E),
    error = ColorDangerDark,
    onError = Color.White
)

val NineRouterShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun NineRouterTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = NineRouterShapes,
        content = content
    )
}
