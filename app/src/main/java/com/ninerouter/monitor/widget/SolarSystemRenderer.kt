package com.ninerouter.monitor.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.ninerouter.monitor.data.model.UsageStatsResponse
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object SolarSystemRenderer {

    // Palet warna tema 9Router
    private val BRAND_COLOR = Color.rgb(0xE5, 0x6A, 0x4A) // #E56A4A
    private val PLANET_COLORS = intArrayOf(
        Color.rgb(0xE5, 0x6A, 0x4A), // Brand terracotta
        Color.rgb(0x3B, 0x82, 0xF6), // Blue
        Color.rgb(0x10, 0xB9, 0x81), // Green
        Color.rgb(0xF5, 0x9E, 0x0B), // Amber
        Color.rgb(0x8B, 0x5C, 0xF6)  // Purple
    )

    fun render(
        stats: UsageStatsResponse,
        widthPx: Int = 600,
        heightPx: Int = 280,
        isDark: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerX = widthPx / 2f
        val centerY = heightPx / 2f
        val maxRadius = min(widthPx, heightPx) / 2f * 0.88f

        // Orbit rings paint
        val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.argb(55, 255, 255, 255) else Color.argb(40, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        // Matahari glow paint
        val sunGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 0xE5, 0x6A, 0x4A)
            style = Paint.Style.FILL
        }

        // Matahari paint
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BRAND_COLOR
            style = Paint.Style.FILL
        }

        // Teks pusat "9R"
        val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        // Gambar Matahari di pusat
        val sunRadius = 24f
        canvas.drawCircle(centerX, centerY, sunRadius + 12f, sunGlowPaint)
        canvas.drawCircle(centerX, centerY, sunRadius, sunPaint)

        val bounds = Rect()
        centerTextPaint.getTextBounds("9R", 0, 2, bounds)
        canvas.drawText("9R", centerX, centerY + (bounds.height() / 2f), centerTextPaint)

        // Model teratas
        val models = stats.byModel.values.toList()
            .sortedByDescending { it.totalTokens.takeIf { t -> t > 0 } ?: it.requests }
            .take(5)

        val maxMetric = models.maxOfOrNull { it.totalTokens.takeIf { t -> t > 0 } ?: it.requests } ?: 1L

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.rgb(0x9C, 0xA3, 0xAF) else Color.rgb(0x4B, 0x55, 0x63)
            textSize = 15f
            textAlign = Paint.Align.CENTER
        }

        val planetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val planetGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val orbitStep = (maxRadius - sunRadius - 16f) / (models.size.coerceAtLeast(1) + 0.3f)

        models.forEachIndexed { index, model ->
            val orbitRadius = sunRadius + 18f + (orbitStep * (index + 0.8f))

            // Lintasan orbit
            canvas.drawCircle(centerX, centerY, orbitRadius, orbitPaint)

            // Posisi planet statis di sudut berbeda
            val angleDeg = (index * 68.0) + 35.0
            val angleRad = Math.toRadians(angleDeg)
            val planetX = centerX + (orbitRadius * cos(angleRad)).toFloat()
            val planetY = centerY + (orbitRadius * sin(angleRad)).toFloat()

            val metricVal = (model.totalTokens.takeIf { it > 0 } ?: model.requests).toFloat()
            val planetRadius = 7f + ((metricVal / maxMetric.toFloat()) * 9f)
            val planetColor = PLANET_COLORS[index % PLANET_COLORS.size]

            // Glow planet
            planetGlowPaint.color = Color.argb(
                50,
                Color.red(planetColor),
                Color.green(planetColor),
                Color.blue(planetColor)
            )
            canvas.drawCircle(planetX, planetY, planetRadius + 3.5f, planetGlowPaint)

            // Badan planet
            planetPaint.color = planetColor
            canvas.drawCircle(planetX, planetY, planetRadius, planetPaint)

            // Nama model bersih di bawah planet (berjarak aman)
            val raw = model.rawModel.ifBlank { model.provider }
            val shortName = when {
                raw.contains("claude", ignoreCase = true) && raw.contains("sonnet", ignoreCase = true) -> "sonnet"
                raw.contains("claude", ignoreCase = true) && raw.contains("haiku", ignoreCase = true) -> "haiku"
                raw.contains("gpt-4", ignoreCase = true) -> "gpt-4o"
                raw.contains("gemini", ignoreCase = true) -> "gemini"
                raw.contains("o3", ignoreCase = true) -> "o3"
                else -> raw.take(7)
            }
            canvas.drawText(shortName, planetX, planetY + planetRadius + 14f, labelPaint)
        }

        return bitmap
    }
}
