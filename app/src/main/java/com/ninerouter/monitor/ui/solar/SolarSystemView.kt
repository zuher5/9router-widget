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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val textMeasurer = rememberTextMeasurer()
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
                                // Transformasi tap koordinat ke world space
                                val (fitScale, fitOffset) = layout.computeFitTransform(size.width.toFloat(), size.height.toFloat())
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
                val (fitScale, fitOffset) = layout.computeFitTransform(size.width, size.height)
                val totalScale = fitScale * userScale
                val totalOffset = fitOffset * userScale + userPan

                // 1. Gambar EDGES
                layout.edges.forEach { edge ->
                    drawTopologyEdge(
                        edge = edge,
                        totalScale = totalScale,
                        totalOffset = totalOffset,
                        particlePhase = particlePhase,
                        borderColor = borderColor
                    )
                }

                // 2. Gambar ROUTER CORE NODE (Pusat)
                val activeCount = activeProviderSet.size
                drawRouterNode(
                    node = layout.routerNode,
                    activeCount = activeCount,
                    totalScale = totalScale,
                    totalOffset = totalOffset,
                    textMeasurer = textMeasurer,
                    isDark = isDark
                )

                // 3. Gambar PROVIDER NODES
                layout.providerNodes.forEach { node ->
                    val isActive = activeProviderSet.contains(node.meta.id.lowercase())
                    drawProviderNode(
                        node = node,
                        isActive = isActive,
                        totalScale = totalScale,
                        totalOffset = totalOffset,
                        textMeasurer = textMeasurer,
                        borderColor = borderColor,
                        nodeBg = nodeBgColor,
                        textColor = textColor,
                        pingScale = pingScale,
                        pingAlpha = pingAlpha
                    )
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
 * Menggambar edge sesuai status (IDLE, LAST, ERROR, ACTIVE kame beam).
 */
private fun DrawScope.drawTopologyEdge(
    edge: LayoutEdge,
    totalScale: Float,
    totalOffset: Offset,
    particlePhase: Float,
    borderColor: Color
) {
    val srcX = edge.sourcePoint.x * totalScale + totalOffset.x
    val srcY = edge.sourcePoint.y * totalScale + totalOffset.y
    val dstX = edge.targetPoint.x * totalScale + totalOffset.x
    val dstY = edge.targetPoint.y * totalScale + totalOffset.y
    val cp1X = edge.controlPoint1.x * totalScale + totalOffset.x
    val cp1Y = edge.controlPoint1.y * totalScale + totalOffset.y
    val cp2X = edge.controlPoint2.x * totalScale + totalOffset.x
    val cp2Y = edge.controlPoint2.y * totalScale + totalOffset.y

    val path = Path().apply {
        moveTo(srcX, srcY)
        cubicTo(cp1X, cp1Y, cp2X, cp2Y, dstX, dstY)
    }

    when (edge.status) {
        EdgeStatus.IDLE -> {
            drawPath(
                path = path,
                color = borderColor.copy(alpha = 0.35f),
                style = Stroke(width = 1.2f)
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
            // 1. Outer halo (cyan)
            drawPath(
                path = path,
                color = Color(0xFF22D3EE).copy(alpha = 0.35f),
                style = Stroke(width = 10f * totalScale.coerceAtLeast(0.6f), cap = StrokeCap.Round)
            )
            // 2. Mid plasma (green)
            drawPath(
                path = path,
                color = Color(0xFF4ADE80).copy(alpha = 0.85f),
                style = Stroke(width = 5f * totalScale.coerceAtLeast(0.6f), cap = StrokeCap.Round)
            )
            // 3. Hot white core
            drawPath(
                path = path,
                color = Color(0xFFF8FAFC),
                style = Stroke(width = 2.2f * totalScale.coerceAtLeast(0.6f), cap = StrokeCap.Round)
            )

            // 4. Energy orbs berjalan di sepanjang kurva
            val pathMeasure = PathMeasure().apply { setPath(path, false) }
            val length = pathMeasure.length
            if (length > 0f) {
                // 6 bola energi dengan variasi kecepatan dan offset
                val orbCount = 6
                for (i in 0 until orbCount) {
                    val speedFactor = 0.7f + (i * 0.12f)
                    val baseT = (particlePhase * speedFactor + (i.toFloat() / orbCount)) % 1f
                    val distance = baseT * length
                    val pos = pathMeasure.getPosition(distance)

                    val orbColor = when (i % 3) {
                        0 -> Color(0xFFFDE047) // Kuning
                        1 -> Color(0xFF67E8F9) // Cyan muda
                        else -> Color.White
                    }
                    val orbRadius = (if (i % 2 == 0) 3.8f else 2.5f) * totalScale.coerceIn(0.7f, 1.4f)

                    // Halo bola
                    drawCircle(
                        color = Color(0xFF22D3EE).copy(alpha = 0.5f),
                        radius = orbRadius + 2.5f,
                        center = pos
                    )
                    // Inti bola
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
 * Menggambar simpul pusat 9Router.
 */
private fun DrawScope.drawRouterNode(
    node: LayoutNode,
    activeCount: Int,
    totalScale: Float,
    totalOffset: Offset,
    textMeasurer: TextMeasurer,
    isDark: Boolean
) {
    val left = node.bounds.left * totalScale + totalOffset.x
    val top = node.bounds.top * totalScale + totalOffset.y
    val width = node.bounds.width * totalScale
    val height = node.bounds.height * totalScale

    val cornerRadius = CornerRadius(12f * totalScale, 12f * totalScale)
    val roundRect = RoundRect(left, top, left + width, top + height, cornerRadius)
    val path = Path().apply { addRoundRect(roundRect) }

    val powering = activeCount > 0

    if (powering) {
        // Gradient powering: primary/30 -> yellow/20 -> cyan/25
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

        // Border kuning menyala
        drawPath(
            path = path,
            color = Color(0xFFFDE047),
            style = Stroke(width = 2.2f * totalScale.coerceAtLeast(0.8f))
        )
    } else {
        // Idle: bg primary/5 + border primary
        drawPath(
            path = path,
            color = NineRouterBrand.copy(alpha = if (isDark) 0.12f else 0.08f)
        )
        drawPath(
            path = path,
            color = NineRouterBrand,
            style = Stroke(width = 1.8f * totalScale.coerceAtLeast(0.8f))
        )
    }

    // Teks "9Router"
    val labelColor = if (powering) Color(0xFFFDE047) else NineRouterBrand
    val labelResult = textMeasurer.measure(
        text = AnnotatedString("9Router"),
        style = TextStyle(
            color = labelColor,
            fontSize = (13f * totalScale.coerceIn(0.75f, 1.3f)).sp,
            fontWeight = FontWeight.Bold
        )
    )

    var textLeft = left + (width - labelResult.size.width) / 2f
    if (activeCount > 0) textLeft -= (10f * totalScale)

    drawText(
        textLayoutResult = labelResult,
        topLeft = Offset(textLeft, top + (height - labelResult.size.height) / 2f)
    )

    // Badge angka activeCount (jika ada)
    if (activeCount > 0) {
        val badgeText = textMeasurer.measure(
            text = AnnotatedString(activeCount.toString()),
            style = TextStyle(
                color = Color.Black,
                fontSize = (10f * totalScale.coerceIn(0.75f, 1.2f)).sp,
                fontWeight = FontWeight.Bold
            )
        )
        val badgeW = (badgeText.size.width + 12f * totalScale).coerceAtLeast(18f * totalScale)
        val badgeH = 16f * totalScale
        val badgeLeft = textLeft + labelResult.size.width + (6f * totalScale)
        val badgeTop = top + (height - badgeH) / 2f

        drawRoundRect(
            color = Color(0xFFFACC15),
            topLeft = Offset(badgeLeft, badgeTop),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(badgeH / 2f, badgeH / 2f)
        )
        drawText(
            textLayoutResult = badgeText,
            topLeft = Offset(
                badgeLeft + (badgeW - badgeText.size.width) / 2f,
                badgeTop + (badgeH - badgeText.size.height) / 2f
            )
        )
    }
}

/**
 * Menggambar node provider (rect, textIcon tile, nama, dan ping dot bila aktif).
 */
private fun DrawScope.drawProviderNode(
    node: LayoutNode,
    isActive: Boolean,
    totalScale: Float,
    totalOffset: Offset,
    textMeasurer: TextMeasurer,
    borderColor: Color,
    nodeBg: Color,
    textColor: Color,
    pingScale: Float,
    pingAlpha: Float
) {
    val left = node.bounds.left * totalScale + totalOffset.x
    val top = node.bounds.top * totalScale + totalOffset.y
    val width = node.bounds.width * totalScale
    val height = node.bounds.height * totalScale

    val cornerRadius = CornerRadius(8f * totalScale, 8f * totalScale)
    val roundRect = RoundRect(left, top, left + width, top + height, cornerRadius)
    val path = Path().apply { addRoundRect(roundRect) }

    val color = node.meta.color

    // Glow jika aktif
    if (isActive) {
        drawPath(
            path = path,
            color = color.copy(alpha = 0.25f),
            style = Stroke(width = 6f * totalScale)
        )
    }

    // Background kartu
    drawPath(path = path, color = nodeBg)

    // Border: aktif warna provider tebal 2dp, idle outline
    drawPath(
        path = path,
        color = if (isActive) color else borderColor,
        style = Stroke(width = (if (isActive) 2f else 1.2f) * totalScale.coerceAtLeast(0.7f))
    )

    // 1. Kotak Icon Tile (26x26) dengan textIcon tebal di dalamnya
    val iconTileSize = 24f * totalScale
    val iconTileLeft = left + (6f * totalScale)
    val iconTileTop = top + (height - iconTileSize) / 2f

    drawRoundRect(
        color = color.copy(alpha = 0.16f),
        topLeft = Offset(iconTileLeft, iconTileTop),
        size = Size(iconTileSize, iconTileSize),
        cornerRadius = CornerRadius(5f * totalScale, 5f * totalScale)
    )

    val iconTextResult = textMeasurer.measure(
        text = AnnotatedString(node.meta.textIcon),
        style = TextStyle(
            color = color,
            fontSize = (10f * totalScale.coerceIn(0.7f, 1.2f)).sp,
            fontWeight = FontWeight.Bold
        )
    )
    drawText(
        textLayoutResult = iconTextResult,
        topLeft = Offset(
            iconTileLeft + (iconTileSize - iconTextResult.size.width) / 2f,
            iconTileTop + (iconTileSize - iconTextResult.size.height) / 2f
        )
    )

    // 2. Nama Provider
    val displayName = node.provider?.displayLabel?.ifBlank { node.meta.name } ?: node.meta.name
    val cleanDisplay = displayName.take(14)
    val nameResult = textMeasurer.measure(
        text = AnnotatedString(cleanDisplay),
        style = TextStyle(
            color = if (isActive) color else textColor,
            fontSize = (11f * totalScale.coerceIn(0.7f, 1.2f)).sp,
            fontWeight = FontWeight.Medium
        )
    )

    val nameLeft = iconTileLeft + iconTileSize + (6f * totalScale)
    drawText(
        textLayoutResult = nameResult,
        topLeft = Offset(nameLeft, top + (height - nameResult.size.height) / 2f)
    )

    // 3. Ping dot saat aktif (persis animate-ping di ProviderTopology.js:84)
    if (isActive) {
        val dotCenterX = left + width - (10f * totalScale)
        val dotCenterY = top + height / 2f
        val baseRadius = 3f * totalScale

        // Lingkaran ping mengembang dan memudar
        drawCircle(
            color = color.copy(alpha = pingAlpha),
            radius = baseRadius * pingScale,
            center = Offset(dotCenterX, dotCenterY)
        )
        // Lingkaran solid
        drawCircle(
            color = color,
            radius = baseRadius,
            center = Offset(dotCenterX, dotCenterY)
        )
    }
}
