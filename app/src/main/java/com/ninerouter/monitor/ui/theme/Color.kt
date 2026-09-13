package com.ninerouter.monitor.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Cyber Obsidian Theme Core (Soft Slate Navy - not pitch black)
val ObsidianBg = Color(0xFF131826)          // Background layar utama
val ObsidianSurface = Color(0xFF1C2333)     // Background Card & Containers
val ObsidianSurfaceVariant = Color(0xFF242D40) // Elevated Surface / Pills
val ObsidianCardElevated = Color(0xFF2B364C) // High Elevated Card / Dialogs
val ObsidianBorder = Color(0xFF334155)      // Slate frosted glass border stroke
val ObsidianBorderSubtle = Color(0xFF334155).copy(alpha = 0.6f)
val ObsidianGlassBorder = Color(0x33FFFFFF) // 20% white
val ObsidianGlassBorderSubtle = Color(0x1AFFFFFF) // 10% white

// Neon Accent Palette
val NeonCoral = Color(0xFFFF6F59)           // Primary Brand, Alert/Error
val NeonAmber = Color(0xFFF59E0B)           // Cost indicator, Warning
val NeonCyan = Color(0xFF22D3EE)            // Live status, Total Requests
val NeonEmerald = Color(0xFF10B981)         // Output tokens, OK / Healthy status
val NeonViolet = Color(0xFF8B5CF6)          // Input tokens, Primary provider node

// Text & Neutral Hierarchy
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

// Brand and Backward-compatibility aliases
val NineRouterBrand = NeonCoral
val NineRouterBrandDark = Color(0xFFE5533D)
val NineRouterBrandGlow = NeonCoral.copy(alpha = 0.20f)

val ColorSuccess = NeonEmerald
val ColorSuccessDark = Color(0xFF059669)
val ColorSuccessGlow = NeonEmerald.copy(alpha = 0.18f)

val ColorDanger = Color(0xFFEF4444)
val ColorDangerDark = Color(0xFFDC2626)
val ColorDangerGlow = Color(0xFFEF4444).copy(alpha = 0.18f)

val ColorWarning = NeonAmber
val ColorWarningDark = Color(0xFFD97706)
val ColorWarningGlow = NeonAmber.copy(alpha = 0.18f)

val ColorInfo = NeonCyan
val ColorInfoDark = Color(0xFF0284C7)
val ColorInfoGlow = NeonCyan.copy(alpha = 0.18f)

val ColorViolet = NeonViolet
val ColorVioletGlow = NeonViolet.copy(alpha = 0.18f)

val ColorTextPrimary = TextPrimary
val ColorTextSecondary = TextSecondary
val ColorTextMuted = TextTertiary

// Gradients
val BrandGradient = Brush.horizontalGradient(
    colors = listOf(NeonCoral, NineRouterBrandDark)
)

val CardGlassGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF222B3D), Color(0xFF182030))
)

val HeroCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF253045), Color(0xFF1A2234))
)
