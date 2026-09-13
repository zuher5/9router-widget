package com.ninerouter.monitor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 9Router Official Brand Colors
val NineRouterBrand = Color(0xFFFF6F59)
val NineRouterBrandDark = Color(0xFFE5533D)
val NineRouterBrandGlow = Color(0x33FF6F59)

// Cyber Obsidian Palette
val ObsidianBg = Color(0xFF0B0D14)
val ObsidianSurface = Color(0xFF131722)
val ObsidianSurfaceVariant = Color(0xFF1B2030)
val ObsidianCardElevated = Color(0xFF22283C)
val ObsidianGlassBorder = Color(0x1FFFFFFF) // 12% white
val ObsidianGlassBorderSubtle = Color(0x0FFFFFFF) // 6% white
val ObsidianGlassBorderActive = Color(0x4DFF6F59) // 30% brand

// Metric Glow & Status Colors
val ColorSuccess = Color(0xFF10B981)
val ColorSuccessDark = Color(0xFF059669)
val ColorSuccessGlow = Color(0x2E10B981)

val ColorDanger = Color(0xFFEF4444)
val ColorDangerDark = Color(0xFFDC2626)
val ColorDangerGlow = Color(0x2EEF4444)

val ColorWarning = Color(0xFFF59E0B)
val ColorWarningDark = Color(0xFFD97706)
val ColorWarningGlow = Color(0x2EF59E0B)

val ColorInfo = Color(0xFF38BDF8)
val ColorInfoDark = Color(0xFF0284C7)
val ColorInfoGlow = Color(0x2E38BDF8)

val ColorViolet = Color(0xFF818CF8)
val ColorVioletGlow = Color(0x2E818CF8)

val ColorTextPrimary = Color(0xFFF8FAFC)
val ColorTextSecondary = Color(0xFF94A3B8)
val ColorTextMuted = Color(0xFF64748B)

// Gradients
val BrandGradient = Brush.horizontalGradient(
    colors = listOf(NineRouterBrand, NineRouterBrandDark)
)

val CardGlassGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF171B27), Color(0xFF11141E))
)

val HeroCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF1C2234), Color(0xFF131622))
)

private val DarkColorScheme = darkColorScheme(
    primary = NineRouterBrand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B1812),
    onPrimaryContainer = Color(0xFFFFDCD2),
    secondary = ColorInfo,
    onSecondary = Color.Black,
    background = ObsidianBg,
    onBackground = ColorTextPrimary,
    surface = ObsidianSurface,
    onSurface = ColorTextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ColorTextSecondary,
    outline = ObsidianGlassBorder,
    outlineVariant = ObsidianGlassBorderSubtle,
    error = ColorDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = NineRouterBrandDark,
    onPrimary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
    error = ColorDanger
)

val NineRouterShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
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
