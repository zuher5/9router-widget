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
        val safeW = widthPx.coerceIn(200, 600)
        val safeH = heightPx.coerceIn(120, 360)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        if (densityDpi > 0) {
            bitmap.density = densityDpi
        }
        val canvas = Canvas(bitmap)

        val effectiveProviders = if (providers.isNotEmpty()) {
            providers
        } else {
            val list = stats.byProvider.keys.map {
                TopologyProvider(id = it, provider = it, name = it)
            }.toMutableList()
            if (list.none { it.provider.equals("opencode", ignoreCase = true) }) {
                list.add(TopologyProvider(id = "opencode", provider = "opencode", name = "OpenCode Free"))
            }
            list
        }

        val activeSet = stats.activeRequests.map { it.provider.lowercase() }.toSet()
        val lastProvider = stats.recentRequests.firstOrNull()?.provider.orEmpty()
        val errorProvider = stats.errorProvider.orEmpty()

        val layout = TopologyGeometry.build(
            providers = effectiveProviders,
            activeProviders = activeSet,
            lastProvider = lastProvider,
            errorProvider = errorProvider
        )

        // Jika drawSummary aktif, sisakan 26px di bawah untuk baris ringkasan
        val summaryBarHeight = if (drawSummary) (safeH * 0.14f).coerceIn(22f, 32f) else 0f
        val topologyHeight = safeH - summaryBarHeight

        val (fitScale, fitOffset) = layout.computeFitTransform(
            safeW.toFloat(),
            topologyHeight,
            paddingFraction = 0.08f
        )

        val borderColor = if (isDark) Color.argb(32, 255, 255, 255) else Color.rgb(0xDC, 0xDC, 0xDC)
        val textColor = if (isDark) Color.rgb(0xF8, 0xFA, 0xFC) else Color.rgb(0x0A, 0x0A, 0x0A)
        val textMutedColor = if (isDark) Color.rgb(0x94, 0xA3, 0xB8) else Color.rgb(0x6B, 0x72, 0x80)
        val nodeBgColor = if (isDark) Color.rgb(0x13, 0x17, 0x22) else Color.rgb(0xFF, 0xFF, 0xFF)

        canvas.save()
        canvas.translate(fitOffset.x, fitOffset.y)
        canvas.scale(fitScale, fitScale)

        // 0. Radar Scope Background
        if (layout.rx > 0f && layout.ry > 0f) {
            val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.argb(22, 255, 255, 255)
                strokeWidth = 1.2f
            }
            val orbitRect = RectF(-layout.rx, -layout.ry, layout.rx, layout.ry)
            canvas.drawOval(orbitRect, orbitPaint)

            val orbitDashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.argb(14, 255, 255, 255)
                strokeWidth = 1f
                pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
            }
            val orbitMidRect = RectF(-layout.rx * 0.65f, -layout.ry * 0.65f, layout.rx * 0.65f, layout.ry * 0.65f)
            canvas.drawOval(orbitMidRect, orbitDashedPaint)
        }

        // 1. Gambar EDGES di world space
        layout.edges.forEach { edge ->
            val androidPath = Path().apply {
                moveTo(edge.sourcePoint.x, edge.sourcePoint.y)
                cubicTo(
                    edge.controlPoint1.x, edge.controlPoint1.y,
                    edge.controlPoint2.x, edge.controlPoint2.y,
                    edge.targetPoint.x, edge.targetPoint.y
                )
            }

            when (edge.status) {
                EdgeStatus.IDLE -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(130, 0x6B, 0x72, 0x80)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                    }
                    canvas.drawPath(androidPath, paint)
                }
                EdgeStatus.LAST -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(200, 0xF5, 0x9E, 0x0B)
                        style = Paint.Style.STROKE
                        strokeWidth = 2.4f
                    }
                    canvas.drawPath(androidPath, paint)
                }
                EdgeStatus.ERROR -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(230, 0xEF, 0x44, 0x44)
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                    }
                    canvas.drawPath(androidPath, paint)
                }
                EdgeStatus.ACTIVE -> {
                    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(85, 0x22, 0xD3, 0xEE)
                        style = Paint.Style.STROKE
                        strokeWidth = 9f
                        strokeCap = Paint.Cap.ROUND
                    }
                    val plasmaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(210, 0x4A, 0xDE, 0x80)
                        style = Paint.Style.STROKE
                        strokeWidth = 4.5f
                        strokeCap = Paint.Cap.ROUND
                    }
                    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                        strokeCap = Paint.Cap.ROUND
                    }
                    canvas.drawPath(androidPath, haloPaint)
                    canvas.drawPath(androidPath, plasmaPaint)
                    canvas.drawPath(androidPath, corePaint)
                }
            }
        }

        // 2. Gambar ROUTER NODE di world space
        val rLeft = layout.routerNode.bounds.left
        val rTop = layout.routerNode.bounds.top
        val rWidth = layout.routerNode.bounds.width
        val rHeight = layout.routerNode.bounds.height
        val rRadius = 10f

        val powering = activeSet.isNotEmpty()

        val routerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            if (powering) {
                shader = LinearGradient(
                    rLeft, rTop, rLeft + rWidth, rTop + rHeight,
                    Color.argb(80, 0xE5, 0x6A, 0x4A),
                    Color.argb(60, 0x22, 0xD3, 0xEE),
                    Shader.TileMode.CLAMP
                )
            } else {
                color = Color.argb(if (isDark) 30 else 18, 0xE5, 0x6A, 0x4A)
            }
        }
        val routerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (powering) Color.rgb(0xFD, 0xE0, 0x47) else BRAND_COLOR
            strokeWidth = 2f
        }
        val routerRect = RectF(rLeft, rTop, rLeft + rWidth, rTop + rHeight)
        canvas.drawRoundRect(routerRect, rRadius, rRadius, routerBgPaint)
        canvas.drawRoundRect(routerRect, rRadius, rRadius, routerBorderPaint)

        val routerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (powering) Color.rgb(0xFD, 0xE0, 0x47) else BRAND_COLOR
            textSize = 12f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }

        val activeCount = activeSet.size
        if (activeCount > 0) {
            val labelW = routerTextPaint.measureText("9Router")
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 9.5f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val badgeText = activeCount.toString()
            val badgeTextW = badgePaint.measureText(badgeText)
            val badgeW = (badgeTextW + 8f).coerceAtLeast(16f)
            val badgeH = 15f
            val gap = 5f

            val totalW = labelW + gap + badgeW
            val startX = rLeft + (rWidth - totalW) / 2f
            val textY = rTop + (rHeight / 2f) + (routerTextPaint.textSize / 3f)

            canvas.drawText("9Router", startX, textY, routerTextPaint)

            val badgeLeft = startX + labelW + gap
            val badgeTop = rTop + (rHeight - badgeH) / 2f
            val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeW, badgeTop + badgeH)
            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(0xFA, 0xCC, 0x15)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, badgeBgPaint)
            val badgeTextY = badgeTop + (badgeH / 2f) + (badgePaint.textSize / 3f)
            canvas.drawText(badgeText, badgeLeft + (badgeW / 2f), badgeTextY, badgePaint)
        } else {
            val textPaintCenter = Paint(routerTextPaint).apply { textAlign = Paint.Align.CENTER }
            val textY = rTop + (rHeight / 2f) + (textPaintCenter.textSize / 3f)
            canvas.drawText("9Router", rLeft + (rWidth / 2f), textY, textPaintCenter)
        }

        // 3. Gambar PROVIDER NODES di world space
        layout.providerNodes.forEach { node ->
            val pLeft = node.bounds.left
            val pTop = node.bounds.top
            val pWidth = node.bounds.width
            val pHeight = node.bounds.height
            val pRadius = 7f

            val meta = ProviderCatalog.get(node.meta.id)
            val pColorInt = Color.rgb(
                (meta.color.red * 255).toInt(),
                (meta.color.green * 255).toInt(),
                (meta.color.blue * 255).toInt()
            )
            val isActive = activeSet.contains(node.meta.id.lowercase())

            val nodeRect = RectF(pLeft, pTop, pLeft + pWidth, pTop + pHeight)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = nodeBgColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(nodeRect, pRadius, pRadius, bgPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = if (isActive) pColorInt else borderColor
                strokeWidth = if (isActive) 2f else 1.2f
            }
            canvas.drawRoundRect(nodeRect, pRadius, pRadius, borderPaint)

            // Icon tile
            val tileSize = pHeight - 8f
            val tileLeft = pLeft + 4f
            val tileTop = pTop + 4f
            val tileRect = RectF(tileLeft, tileTop, tileLeft + tileSize, tileTop + tileSize)
            val tileRadius = 4f

            val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(55, Color.red(pColorInt), Color.green(pColorInt), Color.blue(pColorInt))
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(tileRect, tileRadius, tileRadius, tileBgPaint)

            val tileBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(100, Color.red(pColorInt), Color.green(pColorInt), Color.blue(pColorInt))
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(tileRect, tileRadius, tileRadius, tileBorderPaint)

            val tileTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = pColorInt
                textSize = 9.5f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                meta.textIcon,
                tileLeft + (tileSize / 2f),
                tileTop + (tileSize / 2f) + (tileTextPaint.textSize / 3f),
                tileTextPaint
            )

            // Nama provider
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isActive) pColorInt else textColor
                textSize = 10f
                isFakeBoldText = isActive
                textAlign = Paint.Align.LEFT
            }
            val displayName = meta.name
            val cleanDisplay = if (isActive) displayName.take(9) else displayName.take(11)
            val nameY = pTop + (pHeight / 2f) + (namePaint.textSize / 3f)
            canvas.drawText(cleanDisplay, tileLeft + tileSize + 6f, nameY, namePaint)

            if (isActive) {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pColorInt
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(pLeft + pWidth - 8f, pTop + pHeight / 2f, 3f, dotPaint)
            }
        }

        canvas.restore()

        // 4. Jika diminta, gambar 1 baris ringkasan elegan di bagian bawah canvas
        if (drawSummary) {
            val textY = safeH - (summaryBarHeight * 0.32f)
            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BRAND_COLOR
                textSize = 10.5f
                isFakeBoldText = true
                textAlign = Paint.Align.LEFT
            }
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textMutedColor
                textSize = 10.5f
                textAlign = Paint.Align.LEFT
            }
            val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = 10.5f
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
