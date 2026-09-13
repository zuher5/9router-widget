package com.ninerouter.monitor.ui.solar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.sp
import com.ninerouter.monitor.data.model.UsageStatsResponse
import com.ninerouter.monitor.ui.theme.NineRouterBrand
import kotlin.math.cos
import kotlin.math.sin

// Palet warna planet sesuai aksen 9Router web
val PlanetColors = listOf(
    Color(0xFFE56A4A), // Brand warm terracotta
    Color(0xFF3B82F6), // Info blue
    Color(0xFF10B981), // Success green
    Color(0xFFF59E0B), // Warning amber
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899)  // Pink
)

@Composable
fun SolarSystemView(
    stats: UsageStatsResponse,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val orbitLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    // Animasi rotasi orbit halus
    val infiniteTransition = rememberInfiniteTransition(label = "orbit_animation")
    val angleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val availableRadius = minOf(size.width, size.height) / 2f * 0.90f

        // Matahari di tengah (9Router core)
        val sunRadius = 24f

        // Outer glow
        drawCircle(
            color = NineRouterBrand.copy(alpha = 0.12f),
            radius = sunRadius + 14f,
            center = center
        )
        // Mid glow
        drawCircle(
            color = NineRouterBrand.copy(alpha = 0.28f),
            radius = sunRadius + 6f,
            center = center
        )
        // Sun body
        drawCircle(
            color = NineRouterBrand,
            radius = sunRadius,
            center = center
        )

        // Label "9R" di tengah matahari
        val centerLabel = textMeasurer.measure(
            text = AnnotatedString("9R"),
            style = TextStyle(color = Color.White, fontSize = 11.sp)
        )
        drawText(
            textLayoutResult = centerLabel,
            topLeft = Offset(
                center.x - (centerLabel.size.width / 2f),
                center.y - (centerLabel.size.height / 2f)
            )
        )

        // Urutkan model berdasarkan bobot token/requests (top 5)
        val models = stats.byModel.values.toList()
            .sortedByDescending { it.totalTokens.takeIf { t -> t > 0 } ?: it.requests }
            .take(5)

        val maxMetric = models.maxOfOrNull { it.totalTokens.takeIf { t -> t > 0 } ?: it.requests } ?: 1L

        val orbitStep = (availableRadius - sunRadius - 16f) / (models.size.coerceAtLeast(1) + 0.3f)

        models.forEachIndexed { index, model ->
            val orbitRadius = sunRadius + 20f + (orbitStep * (index + 0.8f))

            // Lingkaran lintasan orbit
            drawCircle(
                color = orbitLineColor,
                radius = orbitRadius,
                center = center,
                style = Stroke(width = 1f)
            )

            // Posisi planet
            val speedFactor = 1f / (index + 0.85f)
            val angle = Math.toRadians((angleOffset * speedFactor + (index * 72.0)).toDouble())
            val planetX = center.x + (orbitRadius * cos(angle)).toFloat()
            val planetY = center.y + (orbitRadius * sin(angle)).toFloat()
            val planetCenter = Offset(planetX, planetY)

            val metricVal = (model.totalTokens.takeIf { it > 0 } ?: model.requests).toFloat()
            val planetRadius = 6.5f + ((metricVal / maxMetric.toFloat()) * 9.5f)
            val planetColor = PlanetColors[index % PlanetColors.size]

            // Glow planet
            drawCircle(
                color = planetColor.copy(alpha = 0.25f),
                radius = planetRadius + 3f,
                center = planetCenter
            )
            // Badan planet
            drawCircle(
                color = planetColor,
                radius = planetRadius,
                center = planetCenter
            )

            // Label nama model bersih
            val raw = model.rawModel.ifBlank { model.provider }
            val shortName = when {
                raw.contains("claude", ignoreCase = true) && raw.contains("sonnet", ignoreCase = true) -> "sonnet"
                raw.contains("claude", ignoreCase = true) && raw.contains("haiku", ignoreCase = true) -> "haiku"
                raw.contains("gpt-4", ignoreCase = true) -> "gpt-4o"
                raw.contains("gemini", ignoreCase = true) -> "gemini"
                raw.contains("o3", ignoreCase = true) -> "o3-mini"
                else -> raw.take(8)
            }

            val textLayoutResult = textMeasurer.measure(
                text = AnnotatedString(shortName),
                style = TextStyle(color = textColor, fontSize = 9.sp)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    planetX - (textLayoutResult.size.width / 2f),
                    planetY + planetRadius + 3f
                )
            )
        }
    }
}
