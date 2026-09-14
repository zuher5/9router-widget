package com.ninerouter.monitor.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninerouter.monitor.R
import com.ninerouter.monitor.data.model.RecentRequestItem
import com.ninerouter.monitor.data.model.TopologyProvider
import com.ninerouter.monitor.data.model.UsageStatsResponse
import com.ninerouter.monitor.ui.solar.ProviderCatalog
import com.ninerouter.monitor.ui.solar.SolarSystemView
import com.ninerouter.monitor.ui.theme.*
import java.text.DecimalFormat
import kotlin.math.roundToInt

/**
 * Status indicator badge with solid dot and smooth state transition (Anti-slop R-19 / R-11 compliant).
 */
@Composable
fun PulsingStatusBadge(
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue = if (isStreaming) NeonCyan else TextTertiary,
        animationSpec = tween(350),
        label = "dotColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isStreaming) NeonCyan.copy(alpha = 0.35f) else ObsidianBorder,
        animationSpec = tween(350),
        label = "borderColor"
    )

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )
            Text(
                text = if (isStreaming) "LIVE" else "OFFLINE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = if (isStreaming) TextPrimary else TextTertiary
            )
        }
    }
}

/**
 * Mini Sparkline Chart using Bezier curves and a vertical gradient fill.
 */
@Composable
fun MiniSparkline(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val maxVal = points.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val minVal = points.minOrNull() ?: 0f
        val range = (maxVal - minVal).coerceAtLeast(0.1f)

        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1)

        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, p ->
            val normalizedY = height - ((p - minVal) / range) * (height * 0.82f) - (height * 0.08f)
            val currentX = i * stepX

            if (i == 0) {
                path.moveTo(currentX, normalizedY)
                fillPath.moveTo(currentX, height)
                fillPath.lineTo(currentX, normalizedY)
            } else {
                val prevX = (i - 1) * stepX
                val prevY = height - ((points[i - 1] - minVal) / range) * (height * 0.82f) - (height * 0.08f)
                val cX = (prevX + currentX) / 2f
                path.cubicTo(cX, prevY, cX, normalizedY, currentX, normalizedY)
                fillPath.cubicTo(cX, prevY, cX, normalizedY, currentX, normalizedY)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.35f), Color.Transparent)
            )
        )

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Mini Circular Gauge with 270 degrees sweep.
 */
@Composable
fun MiniCircularGauge(
    progress: Float,
    color: Color,
    trackColor: Color = ObsidianBorder,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier = modifier) {
        val strokeW = 2.8.dp.toPx()
        val diameter = size.minDimension - strokeW
        val topLeft = Offset(strokeW / 2f, strokeW / 2f)
        val arcSize = Size(diameter, diameter)

        // Track (270 degrees sweep starting at 135 deg)
        drawArc(
            color = trackColor,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Progress
        val sweep = (270f * progress.coerceIn(0.06f, 1f))
        drawArc(
            color = color,
            startAngle = 135f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}

/**
 * Mini Latency Spark Bar with 4 vertical rating bars.
 */
@Composable
fun MiniLatencyBars(
    latencyMs: Long,
    modifier: Modifier = Modifier
) {
    val (filledCount, barColor) = when {
        latencyMs <= 0 -> 1 to TextTertiary
        latencyMs < 400 -> 1 to NeonEmerald
        latencyMs < 800 -> 2 to NeonCyan
        latencyMs < 1600 -> 3 to NeonAmber
        else -> 4 to NeonCoral
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        (1..4).forEach { index ->
            val isFilled = index <= filledCount
            val barHeight = (4 + index * 2.2f).dp
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(barHeight)
                    .background(
                        if (isFilled) barColor else ObsidianBorder,
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}


@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onLogoutClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    DashboardContent(
        stats = state.stats,
        providers = state.providers,
        selectedProvider = state.selectedProvider,
        selectedPeriod = state.selectedPeriod,
        isLoading = state.isLoading,
        isOffline = state.isOffline,
        isStreaming = state.isStreaming,
        errorMessage = state.errorMessage,
        onPeriodSelected = { viewModel.onPeriodSelected(it) },
        onProviderTap = { viewModel.selectProvider(it) },
        onDismissProviderDetail = { viewModel.selectProvider(null) },
        onRefresh = { viewModel.refresh() },
        onLogout = { viewModel.logout(onLogoutClick) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    stats: UsageStatsResponse?,
    providers: List<TopologyProvider> = emptyList(),
    selectedProvider: TopologyProvider? = null,
    selectedPeriod: String = "today",
    isLoading: Boolean = false,
    isOffline: Boolean = false,
    isStreaming: Boolean = false,
    errorMessage: String? = null,
    onPeriodSelected: (String) -> Unit = {},
    onProviderTap: (TopologyProvider) -> Unit = {},
    onDismissProviderDetail: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val periods = listOf(
        "today" to "Today",
        "24h" to "24h",
        "7d" to "7D",
        "30d" to "30D",
        "60d" to "60D"
    )

    Scaffold(
        modifier = modifier.background(ObsidianBg),
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg,
                    titleContentColor = ColorTextPrimary
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "9Router",
                            fontWeight = FontWeight.Black,
                            fontSize = 21.sp,
                            color = NeonCoral,
                            letterSpacing = (-0.5).sp
                        )

                        // Pulsing LIVE Status Pill
                        PulsingStatusBadge(isStreaming = isStreaming)
                    }
                },
                actions = {
                    // Glass Icon Buttons
                    Surface(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⟳", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        onClick = onLogout,
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⎋", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorDanger)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Segmented Period Selector (Modern crisp radius per F-08)
            item {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        periods.forEach { (key, label) ->
                            val selected = selectedPeriod == key
                            Surface(
                                onClick = { onPeriodSelected(key) },
                                shape = RoundedCornerShape(7.dp),
                                color = if (selected) NeonCoral else Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isOffline) {
                item {
                    Surface(
                        color = ColorWarningGlow,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ColorWarning.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(ColorWarning, CircleShape)
                            )
                            Text(
                                text = stringResource(R.string.offline_mode_cached),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
                }
            }

            if (stats != null) {
                // 2. Hero Overview Cards: Total Requests & Est. Cost (Clean, Truthful Metrics - F-01 Fixed)
                item {
                    val avgCostPerReq = remember(stats.totalCost, stats.totalRequests) {
                        if (stats.totalRequests > 0) stats.totalCost / stats.totalRequests else 0.0
                    }
                    val avgCostFormatted = remember(avgCostPerReq) {
                        if (avgCostPerReq in 0.000001..0.001) {
                            "$${DecimalFormat("#0.0000").format(avgCostPerReq)}/req"
                        } else {
                            "$${DecimalFormat("#0.000").format(avgCostPerReq)}/req"
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WebStyleCard(
                            label = "TOTAL REQUESTS",
                            value = DecimalFormat("#,###").format(stats.totalRequests),
                            valueColor = TextPrimary,
                            accentColor = NeonCyan,
                            subtitlePill = "${stats.derivedSuccessRate.roundToInt()}% OK",
                            subtitlePillColor = NeonEmerald,
                            subtitle = "${stats.byProvider.size} providers active",
                            modifier = Modifier.weight(1.05f)
                        )
                        WebStyleCard(
                            label = "EST. COST",
                            value = "$${DecimalFormat("#0.00").format(stats.totalCost)}",
                            valueColor = NeonAmber,
                            accentColor = NeonAmber,
                            subtitlePill = avgCostFormatted,
                            subtitlePillColor = NeonAmber,
                            subtitle = "Token billing estimate",
                            modifier = Modifier.weight(0.95f)
                        )
                    }
                }

                // 3. Token Flow Triad: Input, Cached, Output with Restrained Hierarchy (F-05, F-07 Fixed)
                item {
                    val totalSum = (stats.totalPromptTokens + stats.totalCachedTokens + stats.totalCompletionTokens).coerceAtLeast(1L)
                    val inputProgress = (stats.totalPromptTokens.toFloat() / totalSum).coerceIn(0.12f, 1f)
                    val cachedProgress = (stats.totalCachedTokens.toFloat() / totalSum).coerceIn(0.08f, 1f)
                    val outputProgress = (stats.totalCompletionTokens.toFloat() / totalSum).coerceIn(0.08f, 1f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WebStyleCard(
                            label = "INPUT ↑",
                            value = formatTokens(stats.totalPromptTokens),
                            valueColor = TextPrimary,
                            accentColor = NeonViolet,
                            gaugeProgress = inputProgress,
                            gaugeColor = NeonViolet,
                            isCompact = true,
                            modifier = Modifier.weight(1f)
                        )
                        WebStyleCard(
                            label = "CACHED",
                            value = formatTokens(stats.totalCachedTokens),
                            valueColor = TextPrimary,
                            accentColor = NeonCyan,
                            gaugeProgress = cachedProgress,
                            gaugeColor = NeonCyan,
                            isCompact = true,
                            modifier = Modifier.weight(1f)
                        )
                        WebStyleCard(
                            label = "OUTPUT ↓",
                            value = formatTokens(stats.totalCompletionTokens),
                            valueColor = TextPrimary,
                            accentColor = NeonEmerald,
                            gaugeProgress = outputProgress,
                            gaugeColor = NeonEmerald,
                            isCompact = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Solar System Radar Scope
                item {
                    SolarSystemView(
                        stats = stats,
                        providers = providers,
                        onProviderTap = onProviderTap,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    )
                }

                // 5. Recent Requests Stream
                if (stats.recentRequests.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                            border = BorderStroke(1.dp, ObsidianGlassBorder),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RECENT REQUESTS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = ColorTextSecondary
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ObsidianCardElevated,
                                        border = BorderStroke(0.8.dp, ObsidianGlassBorderSubtle)
                                    ) {
                                        Text(
                                            text = "LATEST ${stats.recentRequests.take(12).size}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            color = NineRouterBrand,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Divider(color = ObsidianGlassBorder, thickness = 0.8.dp)

                                // Request Rows
                                stats.recentRequests.take(12).forEach { req ->
                                    WebStyleRecentRow(req)
                                }
                            }
                        }
                    }
                }
            } else if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NineRouterBrand)
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: stringResource(R.string.no_data),
                            color = ColorTextSecondary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Modal Bottom Sheet detail provider saat ditap
        if (selectedProvider != null) {
            ProviderDetailBottomSheet(
                provider = selectedProvider,
                stats = stats,
                onDismiss = onDismissProviderDetail
            )
        }
    }
}


/**
 * Kartu metrik bergaya Glassmorphism dengan garis aksen bercahaya di atas kartu,
 * opsional sparkline chart di latar bawah, dan opsional mini circular gauge.
 */
@Composable
fun WebStyleCard(
    label: String,
    value: String,
    valueColor: Color,
    accentColor: Color? = null,
    subtitle: String? = null,
    subtitlePill: String? = null,
    subtitlePillColor: Color = NeonEmerald,
    sparklinePoints: List<Float>? = null,
    sparklineColor: Color? = null,
    gaugeProgress: Float? = null,
    gaugeColor: Color? = null,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle Top Accent Line
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(accentColor)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isCompact) 10.dp else 14.dp,
                        vertical = if (isCompact) 10.dp else 12.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header row (Label + Gauge if present)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = if (isCompact) 9.5.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = TextSecondary
                    )
                    if (gaugeProgress != null && gaugeColor != null) {
                        MiniCircularGauge(
                            progress = gaugeProgress,
                            color = gaugeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = value,
                    fontSize = if (isCompact) 16.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitlePill != null || subtitle != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (subtitlePill != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = subtitlePillColor.copy(alpha = 0.15f),
                                border = BorderStroke(0.6.dp, subtitlePillColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = subtitlePill,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subtitlePillColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                fontSize = 9.5.sp,
                                color = TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Optional sparkline slot
                if (sparklinePoints != null && sparklineColor != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    MiniSparkline(
                        points = sparklinePoints,
                        color = sparklineColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    )
                }
            }
        }
    }
}

/**
 * Strip kartu permintaan terbaru:
 * Provider squircle avatar, status dot (OK / ERR), clean sans model name,
 * latency spark bars, dan token In/Out.
 */
@Composable
fun WebStyleRecentRow(item: RecentRequestItem) {
    val isOk = item.status.equals("ok", ignoreCase = true) || item.status.equals("success", ignoreCase = true)
    val meta = ProviderCatalog.get(item.provider)
    val latency = item.latencyMs ?: 0L

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = ObsidianSurfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(0.8.dp, ObsidianBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Provider tile + Model
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Provider Avatar Tile with dark accent
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = meta.color.copy(alpha = 0.18f),
                    border = BorderStroke(0.8.dp, meta.color.copy(alpha = 0.4f)),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = meta.textIcon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = meta.color
                        )
                    }
                }

                // Model name & Provider ID + Status Dot + Latency Bars
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.model.ifEmpty { "unknown" },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.provider.uppercase(),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary
                        )

                        // Status dot badge
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(if (isOk) NeonEmerald else ColorDanger, CircleShape)
                        )
                        Text(
                            text = if (isOk) "OK" else "ERR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOk) NeonEmerald else ColorDanger
                        )

                        if (latency > 0) {
                            Text(
                                text = "•",
                                fontSize = 9.sp,
                                color = TextTertiary
                            )
                            MiniLatencyBars(latencyMs = latency)
                            Text(
                                text = "${latency}ms",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Tokens In & Out
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${formatTokens(item.promptTokens)}↑",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonViolet
                )
                Text(
                    text = "${formatTokens(item.completionTokens)}↓",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonEmerald
                )
            }
        }
    }
}

fun formatTokens(count: Long): String {
    return when {
        count >= 1_000_000 -> "${DecimalFormat("#0.0").format(count / 1_000_000.0)}M"
        count >= 1_000 -> "${DecimalFormat("#0.0").format(count / 1_000.0)}K"
        else -> count.toString()
    }
}
