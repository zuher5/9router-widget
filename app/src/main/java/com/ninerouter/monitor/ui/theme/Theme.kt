package com.ninerouter.monitor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val ObsidianGlassBorderActive = Color(0x4DFF6F59) // 30% brand


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
