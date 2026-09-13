package com.ninerouter.monitor.widget

import android.graphics.*
import com.ninerouter.monitor.data.model.TopologyProvider
import com.ninerouter.monitor.data.model.UsageStatsResponse
import com.ninerouter.monitor.ui.solar.EdgeStatus
import com.ninerouter.monitor.ui.solar.ProviderCatalog
import com.ninerouter.monitor.ui.solar.TopologyGeometry
import com.ninerouter.monitor.ui.solar.TopologyLayout
import java.text.DecimalFormat

object SolarSystemRenderer {

    private val BRAND_COLOR = Color.rgb(0xFF, 0x6F, 0x59) // #FF6F59

    fun render(
        stats: UsageStatsResponse,
        providers: List<TopologyProvider> = emptyList(),
        widthPx: Int = 460,
        heightPx: Int = 220,
        isDark: Boolean = true,
        drawSummary: Boolean = false,
        densityDpi: Int = 0
    ): Bitmap {
        val safeW = widthPx.coerceIn(240, 800)
        val safeH = heightPx.coerceIn(120, 450)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        if (densityDpi > 0) {
            bitmap.density = densityDpi
        }
        val canvas = Canvas(bitmap)
        canvas.drawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Saring provider relevan untuk widget (prioritas: active -> recent -> top usage -> catalog)
        val activeSet = stats.activeRequests.map { it.provider.lowercase() }.toSet()
        val lastProvider = stats.recentRequests.firstOrNull()?.provider.orEmpty().lowercase()
        val errorProvider = stats.errorProvider.orEmpty().lowercase()

        val rawCatalog = if (providers.isNotEmpty()) {
            providers
        } else {
            stats.byProvider.keys.map { TopologyProvider(id = it, provider = it, name = it) }
        }

        // Urutkan & ambil maksimal 8 node agar muat proporsional dan teksnya besar di widget
        val effectiveProviders = rawCatalog
            .distinctBy { it.provider.lowercase() }
            .sortedWith(
                compareByDescending<TopologyProvider> { activeSet.contains(it.provider.lowercase()) }
                    .thenByDescending { it.provider.lowercase() == lastProvider }
                    .thenByDescending { stats.byProvider[it.provider]?.requests ?: 0L }
            )
            .take(8)
            .let { list ->
                if (list.isEmpty()) {
                    listOf(TopologyProvider(id = "opencode", provider = "opencode", name = "OpenCode"))
                } else {
                    list
                }
            }

        // Summary bar height jika drawSummary aktif
        val summaryBarHeight = if (drawSummary) (safeH * 0.15f).coerceIn(24f, 36f) else 0f
        val topologyHeight = safeH - summaryBarHeight
        val cx = safeW / 2f
        val cy = topologyHeight / 2f

        // Palet Dark Slate Navy (tidak gelap pekat)
        val borderColor = if (isDark) Color.rgb(0x33, 0x41, 0x55) else Color.rgb(0xDC, 0xDC, 0xDC)
        val textColor = if (isDark) Color.rgb(0xF8, 0xFA, 0xFC) else Color.rgb(0x0A, 0x0A, 0x0A)
        val textMutedColor = if (isDark) Color.rgb(0x94, 0xA3, 0xB8) else Color.rgb(0x6B, 0x72, 0x80)
        val nodeBgColor = if (isDark) Color.rgb(0x1C, 0x23, 0x33) else Color.rgb(0xFF, 0xFF, 0xFF)

        // Dimensi proporsional canvas langsung (ZOOMED-IN, CLOSE-UP & BESAR)
        val count = effectiveProviders.size
        val nodeW = (safeW * 0.175f).coerceIn(98f, 126f)
        val nodeH = (topologyHeight * 0.165f).coerceIn(32f, 42f)
        val routerW = (safeW * 0.21f).coerceIn(116f, 144f)
        val routerH = (topologyHeight * 0.18f).coerceIn(38f, 48f)

        // Radius orbit mendekati batas canvas (padding minimal 12px agar tampak dekat dan jelas)
        val rx = ((safeW / 2f) - (nodeW / 2f) - 14f).coerceAtLeast(80f)
        val ry = ((topologyHeight / 2f) - (nodeH / 2f) - 10f).coerceAtLeast(45f)

        // 0. Radar Scope Rings & Crosshairs
        val radarDashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(32, 0x22, 0xD3, 0xEE) // Soft Neon Cyan
            strokeWidth = 1.2f
            pathEffect = DashPathEffect(floatArrayOf(5f, 7f), 0f)
        }
        val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(20, 0x94, 0xA3, 0xB8)
            strokeWidth = 0.9f
            pathEffect = DashPathEffect(floatArrayOf(4f, 8f), 0f)
        }

        // Ticks Crosshairs
        canvas.drawLine(cx - rx * 1.08f, cy, cx + rx * 1.08f, cy, crosshairPaint)
        canvas.drawLine(cx, cy - ry * 1.08f, cx, cy + ry * 1.08f, crosshairPaint)

        // Concentric Radar Rings
        val fractions = floatArrayOf(0.48f, 0.76f, 1.0f)
        fractions.forEach { f ->
            val rect = RectF(cx - rx * f, cy - ry * f, cx + rx * f, cy + ry * f)
            canvas.drawOval(rect, radarDashedPaint)
        }

        // Pre-kalkulasi posisi node provider
        data class RenderNode(
            val provider: TopologyProvider,
            val rect: RectF,
            val centerX: Float,
            val centerY: Float,
            val status: EdgeStatus,
            val colorInt: Int
        )

        val renderNodes = effectiveProviders.mapIndexed { i, p ->
            val pid = p.provider.lowercase().trim()
            val meta = ProviderCatalog.get(p.provider)
            val pColorInt = Color.rgb(
                (meta.color.red * 255).toInt(),
                (meta.color.green * 255).toInt(),
                (meta.color.blue * 255).toInt()
            )

            val status = when {
                activeSet.contains(pid) -> EdgeStatus.ACTIVE
                errorProvider.isNotEmpty() && errorProvider == pid -> EdgeStatus.ERROR
                lastProvider.isNotEmpty() && lastProvider == pid -> EdgeStatus.LAST
                else -> EdgeStatus.IDLE
            }

            val angle = -Math.PI / 2.0 + (2.0 * Math.PI * i) / count.toDouble()
            val nodeCx = cx + (rx * Math.cos(angle)).toFloat()
            val nodeCy = cy + (ry * Math.sin(angle)).toFloat()
            val rect = RectF(nodeCx - nodeW / 2f, nodeCy - nodeH / 2f, nodeCx + nodeW / 2f, nodeCy + nodeH / 2f)

            RenderNode(p, rect, nodeCx, nodeCy, status, pColorInt)
        }

        // 1. Gambar EDGES (Garis Balok Antariksa / Plasma Beam)
        renderNodes.forEach { node ->
            val androidPath = Path().apply {
                moveTo(cx, cy)
                val cp1X = cx + (node.centerX - cx) * 0.35f
                val cp1Y = cy + (node.centerY - cy) * 0.15f
                val cp2X = cx + (node.centerX - cx) * 0.70f
                val cp2Y = cy + (node.centerY - cy) * 0.85f
                cubicTo(cp1X, cp1Y, cp2X, cp2Y, node.centerX, node.centerY)
            }

            when (node.status) {
                EdgeStatus.ACTIVE -> {
                    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(90, 0x22, 0xD3, 0xEE)
                        style = Paint.Style.STROKE
                        strokeWidth = 11f
                        strokeCap = Paint.Cap.ROUND
                    }
                    val plasmaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(220, 0x10, 0xB9, 0x81)
                        style = Paint.Style.STROKE
                        strokeWidth = 5f
                        strokeCap = Paint.Cap.ROUND
                    }
                    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = 2.2f
                        strokeCap = Paint.Cap.ROUND
                    }
                    canvas.drawPath(androidPath, haloPaint)
                    canvas.drawPath(androidPath, plasmaPaint)
                    canvas.drawPath(androidPath, corePaint)
                }
                EdgeStatus.LAST -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(210, 0xF5, 0x9E, 0x0B)
                        style = Paint.Style.STROKE
                        strokeWidth = 2.8f
                    }
                    canvas.drawPath(androidPath, paint)
                }
                EdgeStatus.ERROR -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(230, 0xEF, 0x44, 0x44)
                        style = Paint.Style.STROKE
                        strokeWidth = 3.2f
                    }
                    canvas.drawPath(androidPath, paint)
                }
                EdgeStatus.IDLE -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(95, 0x64, 0x74, 0x8B)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.6f
                    }
                    canvas.drawPath(androidPath, paint)
                }
            }
        }

        // 2. Gambar ROUTER NODE (Pusat Tata Surya)
        val powering = activeSet.isNotEmpty()
        val routerRect = RectF(cx - routerW / 2f, cy - routerH / 2f, cx + routerW / 2f, cy + routerH / 2f)
        val rRadius = 10f

        val routerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            if (powering) {
                shader = LinearGradient(
                    routerRect.left, routerRect.top, routerRect.right, routerRect.bottom,
                    Color.argb(90, 0xFF, 0x6F, 0x59),
                    Color.argb(80, 0x22, 0xD3, 0xEE),
                    Shader.TileMode.CLAMP
                )
            } else {
                color = Color.argb(if (isDark) 45 else 22, 0xFF, 0x6F, 0x59)
            }
        }
        val routerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (powering) Color.rgb(0xFD, 0xE0, 0x47) else BRAND_COLOR
            strokeWidth = 2.2f
        }
        canvas.drawRoundRect(routerRect, rRadius, rRadius, routerBgPaint)
        canvas.drawRoundRect(routerRect, rRadius, rRadius, routerBorderPaint)

        val routerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (powering) Color.rgb(0xFD, 0xE0, 0x47) else BRAND_COLOR
            textSize = (routerH * 0.38f).coerceIn(14f, 17f)
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }

        val activeCount = activeSet.size
        if (activeCount > 0) {
            val labelW = routerTextPaint.measureText("9Router")
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = (routerH * 0.28f).coerceIn(10.5f, 13f)
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val badgeText = activeCount.toString()
            val badgeTextW = badgePaint.measureText(badgeText)
            val badgeW = (badgeTextW + 10f).coerceAtLeast(18f)
            val badgeH = (routerH * 0.44f).coerceIn(16f, 22f)
            val gap = 6f

            val totalW = labelW + gap + badgeW
            val startX = cx - totalW / 2f
            val textY = cy + (routerTextPaint.textSize / 3.2f)

            canvas.drawText("9Router", startX, textY, routerTextPaint)

            val badgeLeft = startX + labelW + gap
            val badgeTop = cy - badgeH / 2f
            val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeW, badgeTop + badgeH)
            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(0xFA, 0xCC, 0x15)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, badgeBgPaint)
            val badgeTextY = badgeTop + (badgeH / 2f) + (badgePaint.textSize / 3.2f)
            canvas.drawText(badgeText, badgeLeft + (badgeW / 2f), badgeTextY, badgePaint)
        } else {
            val textPaintCenter = Paint(routerTextPaint).apply { textAlign = Paint.Align.CENTER }
            val textY = cy + (textPaintCenter.textSize / 3.2f)
            canvas.drawText("9Router", cx, textY, textPaintCenter)
        }

        // 3. Gambar PROVIDER NODES (Besar, Bold, Jelas Terbaca)
        renderNodes.forEach { node ->
            val pRadius = 8f
            val isActive = node.status == EdgeStatus.ACTIVE
            val meta = ProviderCatalog.get(node.provider.provider)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = nodeBgColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(node.rect, pRadius, pRadius, bgPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = if (isActive) node.colorInt else borderColor
                strokeWidth = if (isActive) 2.4f else 1.4f
            }
            canvas.drawRoundRect(node.rect, pRadius, pRadius, borderPaint)

            // Icon tile
            val tileSize = nodeH - 10f
            val tileLeft = node.rect.left + 5f
            val tileTop = node.rect.top + 5f
            val tileRect = RectF(tileLeft, tileTop, tileLeft + tileSize, tileTop + tileSize)
            val tileRadius = 5f

            val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(60, Color.red(node.colorInt), Color.green(node.colorInt), Color.blue(node.colorInt))
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(tileRect, tileRadius, tileRadius, tileBgPaint)

            val tileBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(120, Color.red(node.colorInt), Color.green(node.colorInt), Color.blue(node.colorInt))
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }
            canvas.drawRoundRect(tileRect, tileRadius, tileRadius, tileBorderPaint)

            val tileTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = node.colorInt
                textSize = (tileSize * 0.46f).coerceIn(11f, 15f)
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                meta.textIcon,
                tileLeft + (tileSize / 2f),
                tileTop + (tileSize / 2f) + (tileTextPaint.textSize / 3.2f),
                tileTextPaint
            )

            // Nama provider (Font besar & tebal)
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isActive) node.colorInt else textColor
                textSize = (nodeH * 0.35f).coerceIn(12f, 15f)
                isFakeBoldText = true
                textAlign = Paint.Align.LEFT
            }
            val displayName = meta.name
            val cleanDisplay = if (isActive) displayName.take(8) else displayName.take(10)
            val nameY = node.rect.top + (nodeH / 2f) + (namePaint.textSize / 3.2f)
            canvas.drawText(cleanDisplay, tileLeft + tileSize + 6f, nameY, namePaint)

            if (isActive) {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = node.colorInt
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(node.rect.right - 8f, node.rect.top + nodeH / 2f, 3.8f, dotPaint)
            }
        }

        // 4. Jika diminta drawSummary
        if (drawSummary) {
            val textY = safeH - (summaryBarHeight * 0.30f)
            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BRAND_COLOR
                textSize = 11.5f
                isFakeBoldText = true
                textAlign = Paint.Align.LEFT
            }
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textMutedColor
                textSize = 11.5f
                textAlign = Paint.Align.LEFT
            }
            val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = 11.5f
                isFakeBoldText = false
                textAlign = Paint.Align.LEFT
            }

            val part1 = "9Router"
            val dot = "  •  "
            val part2 = "${DecimalFormat("#,###").format(stats.totalRequests)} reqs"
            val part3 = "$${DecimalFormat("#0.000").format(stats.totalCost)}"

            val w1 = brandPaint.measureText(part1)
            val wDot = dotPaint.measureText(dot)
            val w2 = lightPaint.measureText(part2)
            val w3 = lightPaint.measureText(part3)
            val totalW = w1 + wDot + w2 + wDot + w3

            var curX = (safeW - totalW) / 2f
            canvas.drawText(part1, curX, textY, brandPaint)
            curX += w1
            canvas.drawText(dot, curX, textY, dotPaint)
            curX += wDot
            canvas.drawText(part2, curX, textY, lightPaint)
            curX += w2
            canvas.drawText(dot, curX, textY, dotPaint)
            curX += wDot
            canvas.drawText(part3, curX, textY, lightPaint)
        }

        bitmap.prepareToDraw()
        return bitmap
    }
}
