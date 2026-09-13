package com.ninerouter.monitor.ui.solar

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint as AndroidPaint
import com.ninerouter.monitor.data.model.TopologyProvider
import com.ninerouter.monitor.data.model.UsageStatsResponse
import com.ninerouter.monitor.ui.theme.NineRouterBrand
import kotlinx.coroutines.delay

private const val FE_ACTIVE_TIMEOUT_MS = 60000L

@Composable
fun SolarSystemView(
    stats: UsageStatsResponse,
    providers: List<TopologyProvider> = emptyList(),
    onProviderTap: ((TopologyProvider) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Saring provider aktif yang tidak kedaluwarsa (>60 detik)
    val firstSeenMap = remember { mutableStateMapOf<String, Long>() }
    var currentTick by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(stats.activeRequests) {
        val activeSet = stats.activeRequests.map { it.provider.lowercase() }.toSet()
        val now = System.currentTimeMillis()
        activeSet.forEach { p ->
            if (!firstSeenMap.containsKey(p)) firstSeenMap[p] = now
        }
        firstSeenMap.keys.retainAll(activeSet)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTick = System.currentTimeMillis()
        }
    }

    val activeProviderSet = remember(stats.activeRequests, currentTick) {
        val now = currentTick
        stats.activeRequests.mapNotNull { req ->
            val p = req.provider.lowercase().trim()
            val seen = firstSeenMap[p]
            if (seen == null || now - seen < FE_ACTIVE_TIMEOUT_MS) p else null
        }.toSet()
    }

    val lastProvider = remember(stats.recentRequests) {
        stats.recentRequests.firstOrNull()?.provider.orEmpty()
    }
    val errorProvider = stats.errorProvider.orEmpty()

    // Fallback jika list provider belum dimuat: ekstrak dari stats.byProvider
    val effectiveProviders = remember(providers, stats.byProvider) {
        if (providers.isNotEmpty()) {
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
    }

    // Bangun layout geometri
    val layout = remember(effectiveProviders, activeProviderSet, lastProvider, errorProvider) {
        TopologyGeometry.build(
            providers = effectiveProviders,
            activeProviders = activeProviderSet,
            lastProvider = lastProvider,
            errorProvider = errorProvider
        )
    }

    // State transformasi pan & zoom
    var userScale by remember { mutableStateOf(1f) }
    var userPan by remember { mutableStateOf(Offset.Zero) }

    // Animasi partikel kame beam (0f .. 1f berulang secara halus)
    val infiniteTransition = rememberInfiniteTransition(label = "kame_particle_anim")
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beam_phase"
    )
    val pingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ping_scale"
    )
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ping_alpha"
    )

    val borderColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface
    val textMutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val nodeBgColor = MaterialTheme.colorScheme.surface
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF141414) else Color(0xFFF9F7F4))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        if (effectiveProviders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No providers connected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textMutedColor
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            userScale = (userScale * zoom).coerceIn(0.6f, 3.0f)
                            userPan += pan
                        }
                    }
                    .pointerInput(layout, userScale, userPan) {
                        detectTapGestures(
                            onDoubleTap = {
                                userScale = 1f
                                userPan = Offset.Zero
                            },
                            onTap = { tapOffset ->
                                if (onProviderTap == null) return@detectTapGestures
                                val (fitScale, fitOffset) = layout.computeFitTransform(size.width.toFloat(), size.height.toFloat(), paddingFraction = 0.08f)
                                val totalScale = fitScale * userScale
                                val totalOffset = fitOffset * userScale + userPan

                                val worldX = (tapOffset.x - totalOffset.x) / totalScale
                                val worldY = (tapOffset.y - totalOffset.y) / totalScale

                                val tappedNode = layout.providerNodes.firstOrNull { node ->
                                    node.bounds.contains(Offset(worldX, worldY))
                                }
                                if (tappedNode?.provider != null) {
                                    onProviderTap(tappedNode.provider)
                                }
                            }
                        )
                    }
            ) {
                val (fitScale, fitOffset) = layout.computeFitTransform(size.width, size.height, paddingFraction = 0.08f)
                val totalScale = fitScale * userScale
                val totalOffset = fitOffset * userScale + userPan

                // Gunakan withTransform untuk mentransformasi seluruh world coordinate secara presisi (termasuk font & shapes)
                withTransform({
                    translate(totalOffset.x, totalOffset.y)
                    scale(totalScale, totalScale, pivot = Offset.Zero)
                }) {
                    // 1. Gambar EDGES di world space
                    layout.edges.forEach { edge ->
                        drawTopologyEdgeWorld(
                            edge = edge,
                            particlePhase = particlePhase,
                            borderColor = borderColor
                        )
                    }

                    // 2. Gambar ROUTER CORE NODE (Pusat) di world space
                    val activeCount = activeProviderSet.size
                    drawRouterNodeWorld(
                        node = layout.routerNode,
                        activeCount = activeCount,
                        isDark = isDark
                    )

                    // 3. Gambar PROVIDER NODES di world space
                    layout.providerNodes.forEach { node ->
                        val isActive = activeProviderSet.contains(node.meta.id.lowercase())
                        drawProviderNodeWorld(
                            node = node,
                            isActive = isActive,
                            borderColor = borderColor,
                            nodeBg = nodeBgColor,
                            textColor = textColor,
                            pingScale = pingScale,
                            pingAlpha = pingAlpha
                        )
                    }
                }
            }

            // Controls overlay pojok kiri bawah (persis ReactFlow Controls)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clickable { userScale = (userScale * 1.25f).coerceAtMost(3.0f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clickable { userScale = (userScale * 0.8f).coerceAtLeast(0.6f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clickable {
                                userScale = 1f
                                userPan = Offset.Zero
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⛶", fontSize = 12.sp, color = textMutedColor)
                    }
                }
            }
        }
    }
}

/**
 * Menggambar edge sesuai status di world space murni.
 */
private fun DrawScope.drawTopologyEdgeWorld(
    edge: LayoutEdge,
    particlePhase: Float,
    borderColor: Color
) {
    val path = Path().apply {
        moveTo(edge.sourcePoint.x, edge.sourcePoint.y)
        cubicTo(
            edge.controlPoint1.x, edge.controlPoint1.y,
            edge.controlPoint2.x, edge.controlPoint2.y,
            edge.targetPoint.x, edge.targetPoint.y
        )
    }

    when (edge.status) {
        EdgeStatus.IDLE -> {
            drawPath(
                path = path,
                color = Color(0xFF6B7280).copy(alpha = 0.50f),
                style = Stroke(width = 1.4f)
            )
        }
        EdgeStatus.LAST -> {
            drawPath(
                path = path,
                color = Color(0xFFF59E0B).copy(alpha = 0.85f),
                style = Stroke(width = 2.2f)
            )
        }
        EdgeStatus.ERROR -> {
            drawPath(
                path = path,
                color = Color(0xFFEF4444).copy(alpha = 0.95f),
                style = Stroke(width = 2.8f)
            )
        }
        EdgeStatus.ACTIVE -> {
            // KAME BEAM: multi-layer stroke + bola energi bergerak
            drawPath(
                path = path,
                color = Color(0xFF22D3EE).copy(alpha = 0.35f),
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )
            drawPath(
                path = path,
                color = Color(0xFF4ADE80).copy(alpha = 0.85f),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
            drawPath(
                path = path,
                color = Color(0xFFF8FAFC),
                style = Stroke(width = 2.2f, cap = StrokeCap.Round)
            )

            // Energy orbs berjalan di sepanjang kurva
            val pathMeasure = PathMeasure().apply { setPath(path, false) }
            val length = pathMeasure.length
            if (length > 0f) {
                val orbCount = 6
                for (i in 0 until orbCount) {
                    val speedFactor = 0.7f + (i * 0.12f)
                    val baseT = (particlePhase * speedFactor + (i.toFloat() / orbCount)) % 1f
                    val distance = baseT * length
                    val pos = pathMeasure.getPosition(distance)

                    val orbColor = when (i % 3) {
                        0 -> Color(0xFFFDE047)
                        1 -> Color(0xFF67E8F9)
                        else -> Color.White
                    }
                    val orbRadius = if (i % 2 == 0) 4f else 2.5f

                    drawCircle(
                        color = Color(0xFF22D3EE).copy(alpha = 0.5f),
                        radius = orbRadius + 3f,
                        center = pos
                    )
                    drawCircle(
                        color = orbColor,
                        radius = orbRadius,
                        center = pos
                    )
                }
            }
        }
    }
}

/**
 * Menggambar simpul pusat 9Router di world space.
 */
private fun DrawScope.drawRouterNodeWorld(
    node: LayoutNode,
    activeCount: Int,
    isDark: Boolean
) {
    val left = node.bounds.left
    val top = node.bounds.top
    val width = node.bounds.width
    val height = node.bounds.height

    val cornerRadius = CornerRadius(10f, 10f)
    val roundRect = RoundRect(left, top, left + width, top + height, cornerRadius)
    val path = Path().apply { addRoundRect(roundRect) }

    val powering = activeCount > 0

    if (powering) {
        val brush = Brush.linearGradient(
            colors = listOf(
                NineRouterBrand.copy(alpha = 0.35f),
                Color(0xFFFACC15).copy(alpha = 0.25f),
                Color(0xFF22D3EE).copy(alpha = 0.30f)
            ),
            start = Offset(left, top),
            end = Offset(left + width, top + height)
        )
        drawPath(path = path, brush = brush)
        drawPath(
            path = path,
            color = Color(0xFFFDE047),
            style = Stroke(width = 2.2f)
        )
    } else {
        drawPath(
            path = path,
            color = NineRouterBrand.copy(alpha = if (isDark) 0.12f else 0.08f)
        )
        drawPath(
            path = path,
            color = NineRouterBrand,
            style = Stroke(width = 1.8f)
        )
    }

    // Teks 9Router & badge menggunakan AndroidPaint di world space
    val labelColorInt = if (powering) android.graphics.Color.rgb(0xFD, 0xE0, 0x47) else android.graphics.Color.rgb(0xE5, 0x6A, 0x4A)

    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val routerPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = labelColorInt
            textSize = 12.5f
            isFakeBoldText = true
            textAlign = AndroidPaint.Align.LEFT
        }

        if (activeCount > 0) {
            val labelW = routerPaint.measureText("9Router")
            val badgePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.BLACK
                textSize = 9.5f
                isFakeBoldText = true
                textAlign = AndroidPaint.Align.CENTER
            }
            val badgeText = activeCount.toString()
            val badgeTextW = badgePaint.measureText(badgeText)
            val badgeW = (badgeTextW + 8f).coerceAtLeast(16f)
            val badgeH = 15f
            val gap = 5f

            val totalContentW = labelW + gap + badgeW
            val startX = left + (width - totalContentW) / 2f
            val textY = top + (height / 2f) + (routerPaint.textSize / 3f)

            native.drawText("9Router", startX, textY, routerPaint)

            val badgeLeft = startX + labelW + gap
            val badgeTop = top + (height - badgeH) / 2f
            val badgeRect = android.graphics.RectF(badgeLeft, badgeTop, badgeLeft + badgeW, badgeTop + badgeH)
            val badgeBgPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(0xFA, 0xCC, 0x15)
                style = AndroidPaint.Style.FILL
            }
            native.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, badgeBgPaint)
            val badgeTextY = badgeTop + (badgeH / 2f) + (badgePaint.textSize / 3f)
            native.drawText(badgeText, badgeLeft + (badgeW / 2f), badgeTextY, badgePaint)
        } else {
            val centerPaint = AndroidPaint(routerPaint).apply { textAlign = AndroidPaint.Align.CENTER }
            val textY = top + (height / 2f) + (centerPaint.textSize / 3f)
            native.drawText("9Router", left + (width / 2f), textY, centerPaint)
        }
    }
}

/**
 * Menggambar node provider di world space murni.
 */
private fun DrawScope.drawProviderNodeWorld(
    node: LayoutNode,
    isActive: Boolean,
    borderColor: Color,
    nodeBg: Color,
    textColor: Color,
    pingScale: Float,
    pingAlpha: Float
) {
    val left = node.bounds.left
    val top = node.bounds.top
    val width = node.bounds.width
    val height = node.bounds.height

    val cornerRadius = CornerRadius(7f, 7f)
    val roundRect = RoundRect(left, top, left + width, top + height, cornerRadius)
    val path = Path().apply { addRoundRect(roundRect) }

    val color = node.meta.color

    // Glow jika aktif
    if (isActive) {
        drawPath(
            path = path,
            color = color.copy(alpha = 0.25f),
            style = Stroke(width = 6f)
        )
    }

    drawPath(path = path, color = nodeBg)

    drawPath(
        path = path,
        color = if (isActive) color else borderColor,
        style = Stroke(width = if (isActive) 2f else 1.2f)
    )

    // Kotak Icon Tile
    val iconTileSize = height - 8f
    val iconTileLeft = left + 4f
    val iconTileTop = top + 4f

    drawRoundRect(
        color = color.copy(alpha = 0.22f),
        topLeft = Offset(iconTileLeft, iconTileTop),
        size = Size(iconTileSize, iconTileSize),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = color.copy(alpha = 0.45f),
        topLeft = Offset(iconTileLeft, iconTileTop),
        size = Size(iconTileSize, iconTileSize),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 1f)
    )

    val colorInt = android.graphics.Color.rgb(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt()
    )
    val textColorInt = android.graphics.Color.rgb(
        (textColor.red * 255).toInt(),
        (textColor.green * 255).toInt(),
        (textColor.blue * 255).toInt()
    )

    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas

        // 1. Teks icon tile
        val iconPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            this.color = colorInt
            textSize = 9.5f
            isFakeBoldText = true
            textAlign = AndroidPaint.Align.CENTER
        }
        val iconTextY = iconTileTop + (iconTileSize / 2f) + (iconPaint.textSize / 3f)
        native.drawText(node.meta.textIcon, iconTileLeft + (iconTileSize / 2f), iconTextY, iconPaint)

        // 2. Nama Provider
        val namePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            this.color = if (isActive) colorInt else textColorInt
            textSize = 10f
            isFakeBoldText = isActive
            textAlign = AndroidPaint.Align.LEFT
        }
        val displayName = node.meta.name
        val cleanDisplay = if (isActive) displayName.take(9) else displayName.take(11)
        val nameY = top + (height / 2f) + (namePaint.textSize / 3f)
        native.drawText(cleanDisplay, iconTileLeft + iconTileSize + 6f, nameY, namePaint)
    }

    // Ping dot jika aktif
    if (isActive) {
        val dotCenterX = left + width - 9f
        val dotCenterY = top + height / 2f
        val baseRadius = 3f

        drawCircle(
            color = color.copy(alpha = pingAlpha),
            radius = baseRadius * pingScale,
            center = Offset(dotCenterX, dotCenterY)
        )
        drawCircle(
            color = color,
            radius = baseRadius,
            center = Offset(dotCenterX, dotCenterY)
        )
    }
}
