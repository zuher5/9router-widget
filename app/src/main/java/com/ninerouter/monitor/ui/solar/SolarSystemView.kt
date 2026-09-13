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

// Warna planet tema 9Router
val PlanetColors = listOf(
    Color(0xFFE56A4A), // 9Router brand terracotta
    Color(0xFF3B82F6), // Blue
    Color(0xFF10B981), // Emerald green
    Color(0xFFF59E0B), // Amber
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899)  // Pink
)

@Composable
fun SolarSystemView(
    stats: UsageStatsResponse,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onSurface
    val orbitColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    // Animasi rotasi orbit halus
    val infiniteTransition = rememberInfiniteTransition(label = "orbit_animation")
    val angleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = minOf(size.width, size.height) / 2f * 0.88f

        // Matahari di tengah = 9Router brand color / warm amber
        val sunRadius = 26f
        // Outer glow
        drawCircle(
            color = NineRouterBrand.copy(alpha = 0.15f),
            radius = sunRadius + 14f,
            center = center
        )
        // Mid glow
        drawCircle(
            color = NineRouterBrand.copy(alpha = 0.35f),
            radius = sunRadius + 6f,
            center = center
        )
        // Sun body
        drawCircle(
            color = NineRouterBrand,
            radius = sunRadius,
            center = center
        )

        // Label "9R" di dalam matahari
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

        // Ambil top 5 model berdasarkan token atau requests
        val models = stats.byModel.values.toList()
            .sortedByDescending { it.totalTokens.takeIf { t -> t > 0 } ?: it.requests }
            .take(5)

        val maxTokens = models.maxOfOrNull { it.totalTokens.takeIf { t -> t > 0 } ?: it.requests } ?: 1L

        models.forEachIndexed { index, model ->
            val orbitRadius = (maxRadius / (models.size + 1)) * (index + 1) + (sunRadius * 0.6f)

            // Lingkaran lintasan orbit
            drawCircle(
                color = orbitColor,
                radius = orbitRadius,
                center = center,
                style = Stroke(width = 1.2f)
            )

            // Posisi planet dengan kecepatan orbit berbeda-beda (planet dalam lebih cepat)
            val speedFactor = 1f / (index + 0.8f)
            val angle = Math.toRadians((angleOffset * speedFactor + (index * 65)).toDouble())
            val planetX = center.x + (orbitRadius * cos(angle)).toFloat()
            val planetY = center.y + (orbitRadius * sin(angle)).toFloat()
            val planetCenter = Offset(planetX, planetY)

            val metricVal = (model.totalTokens.takeIf { it > 0 } ?: model.requests).toFloat()
            val planetRadius = 7f + ((metricVal / maxTokens.toFloat()) * 11f)
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

            // Nama model singkat di bawah planet
            val shortName = model.rawModel.ifBlank { model.provider }.take(9)
            val textLayoutResult = textMeasurer.measure(
                text = AnnotatedString(shortName),
                style = TextStyle(color = textColor, fontSize = 9.sp)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    planetX - (textLayoutResult.size.width / 2f),
                    planetY + planetRadius + 2f
                )
            )
        }
    }
}
