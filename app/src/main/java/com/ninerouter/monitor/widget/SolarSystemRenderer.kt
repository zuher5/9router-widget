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

    // Palet warna tema 9Router dalam representasi int Color Android
    private val BRAND_COLOR = Color.rgb(0xE5, 0x6A, 0x4A) // #E56A4A
    private val PLANET_COLORS = intArrayOf(
        Color.rgb(0xE5, 0x6A, 0x4A), // Brand terracotta
        Color.rgb(0x3B, 0x82, 0xF6), // Blue
        Color.rgb(0x10, 0xB9, 0x81), // Green
        Color.rgb(0xF5, 0x9E, 0x0B), // Amber
        Color.rgb(0x8B, 0x5C, 0xF6)  // Purple
    )

    /**
     * Menggambar grafik solar system ke Bitmap agar bisa ditampilkan di Glance via ImageProvider.
     * Glance 1.1.1 tidak memiliki Canvas Composable, jadi render Bitmap adalah solusi resmi.
     */
    fun render(
        stats: UsageStatsResponse,
        widthPx: Int = 600,
        heightPx: Int = 300,
        isDark: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerX = widthPx / 2f
        val centerY = heightPx / 2f
        val maxRadius = min(widthPx, heightPx) / 2f * 0.85f

        // Background transparan (widget card sudah punya container)
        // Paint untuk orbit rings
        val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.argb(60, 255, 255, 255) else Color.argb(40, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        // Paint glow matahari
        val sunGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 0xE5, 0x6A, 0x4A)
            style = Paint.Style.FILL
        }

        // Paint matahari (pusat)
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BRAND_COLOR
            style = Paint.Style.FILL
        }

        // Text paint label tengah
        val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        // Gambar Matahari di pusat
        val sunRadius = 28f
        canvas.drawCircle(centerX, centerY, sunRadius + 14f, sunGlowPaint)
        canvas.drawCircle(centerX, centerY, sunRadius, sunPaint)

        // Label teks "9R" di tengah matahari
        val bounds = Rect()
        centerTextPaint.getTextBounds("9R", 0, 2, bounds)
        canvas.drawText("9R", centerX, centerY + (bounds.height() / 2f), centerTextPaint)

        // Model teratas
        val models = stats.byModel.values.toList()
            .sortedByDescending { it.totalTokens.takeIf { t -> t > 0 } ?: it.requests }
            .take(5)

        val maxTokens = models.maxOfOrNull { it.totalTokens.takeIf { t -> t > 0 } ?: it.requests } ?: 1L

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.rgb(0xED, 0xED, 0xED) else Color.rgb(0x0A, 0x0A, 0x0A)
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }

        val planetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val planetGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        models.forEachIndexed { index, model ->
            val orbitRadius = (maxRadius / (models.size + 1)) * (index + 1) + (sunRadius * 0.5f)

            // Gambar lingkaran lintasan
            canvas.drawCircle(centerX, centerY, orbitRadius, orbitPaint)

            // Posisi planet tetap berdasarkan indeks (snapshot tidak animasi agar hemat render widget)
            val angleDeg = (index * 72.0) + 30.0
            val angleRad = Math.toRadians(angleDeg)
            val planetX = centerX + (orbitRadius * cos(angleRad)).toFloat()
            val planetY = centerY + (orbitRadius * sin(angleRad)).toFloat()

            val metricVal = (model.totalTokens.takeIf { it > 0 } ?: model.requests).toFloat()
            val planetRadius = 8f + ((metricVal / maxTokens.toFloat()) * 12f)
            val planetColor = PLANET_COLORS[index % PLANET_COLORS.size]

            // Glow planet
            planetGlowPaint.color = Color.argb(
                60,
                Color.red(planetColor),
                Color.green(planetColor),
                Color.blue(planetColor)
            )
            canvas.drawCircle(planetX, planetY, planetRadius + 4f, planetGlowPaint)

            // Badan planet
            planetPaint.color = planetColor
            canvas.drawCircle(planetX, planetY, planetRadius, planetPaint)

            // Nama model singkat
            val shortName = model.rawModel.ifBlank { model.provider }.take(8)
            canvas.drawText(shortName, planetX, planetY + planetRadius + 18f, labelPaint)
        }

        return bitmap
    }
}
