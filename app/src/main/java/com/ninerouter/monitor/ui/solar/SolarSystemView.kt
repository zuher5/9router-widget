package com.ninerouter.monitor.ui.solar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.sp
import com.ninerouter.monitor.data.model.UsageStatsResponse
import kotlin.math.cos
import kotlin.math.sin

private val PlanetColors = listOf(
    Color(0xFF42A5F5), // Biru
    Color(0xFF66BB6A), // Hijau
    Color(0xFFFFA726), // Oranye
    Color(0xFFAB47BC), // Ungu
    Color(0xFF26C6DA), // Cyan
    Color(0xFFFF7043)  // Merah bata
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun SolarSystemView(
    stats: UsageStatsResponse,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Animasi rotasi planet perlahan
    val infiniteTransition = rememberInfiniteTransition(label = "orbit_animation")
    val angleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = minOf(size.width, size.height) / 2f * 0.85f

        // Gambar Matahari di tengah (Total Usage)
        val sunRadius = 24f
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = sunRadius,
            center = center
        )
        // Glow effect matahari
        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = 0.2f),
            radius = sunRadius + 8f,
            center = center
        )

        // Urutkan model berdasarkan total token terbesar
        val models = stats.byModel.values.toList()
            .sortedByDescending { it.totalTokens }
            .take(5) // Ambil top 5 model

        val maxTokens = models.maxOfOrNull { it.totalTokens } ?: 1L

        models.forEachIndexed { index, model ->
            val orbitRadius = (maxRadius / (models.size + 1)) * (index + 1) + sunRadius
            val orbitColor = Color.Gray.copy(alpha = 0.25f)

            // Gambar lingkaran orbit
            drawCircle(
                color = orbitColor,
                radius = orbitRadius,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Hitung posisi planet berdasarkan orbit & animasi sudut
            val angle = Math.toRadians((angleOffset * (1f / (index + 1)) + (index * 72)).toDouble())
            val planetX = center.x + (orbitRadius * cos(angle)).toFloat()
            val planetY = center.y + (orbitRadius * sin(angle)).toFloat()
            val planetCenter = Offset(planetX, planetY)

            // Ukuran planet proporsional dengan token
            val planetRadius = 8f + ((model.totalTokens.toFloat() / maxTokens) * 12f)
            val planetColor = PlanetColors[index % PlanetColors.size]

            drawCircle(
                color = planetColor,
                radius = planetRadius,
                center = planetCenter
            )

            // Tampilkan nama model ringkas
            val shortName = model.rawModel.ifBlank { model.provider }.take(10)
            val textLayoutResult = textMeasurer.measure(
                text = AnnotatedString(shortName),
                style = TextStyle(color = Color.White, fontSize = 9.sp)
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
