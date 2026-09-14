package com.ninerouter.monitor.ui.dashboard

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninerouter.monitor.data.model.ModelStat
import com.ninerouter.monitor.data.model.TopologyProvider
import com.ninerouter.monitor.data.model.UsageStatsResponse
import com.ninerouter.monitor.ui.solar.ProviderCatalog
import com.ninerouter.monitor.ui.theme.*
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailBottomSheet(
    provider: TopologyProvider,
    stats: UsageStatsResponse?,
    onDismiss: () -> Unit
) {
    val meta = ProviderCatalog.get(provider.provider)
    val stat = stats?.byProvider?.get(provider.provider.lowercase())

    // Ambil list model yang digunakan oleh provider ini
    val modelsForProvider = remember(stats, provider.provider) {
        val byModelList = stats?.byModel?.values
            ?.filter { it.provider.equals(provider.provider, ignoreCase = true) }
            .orEmpty()

        if (byModelList.isNotEmpty()) {
            byModelList.sortedByDescending { it.requests }
        } else {
            // Fallback dari recent requests jika byModel kosong
            val fromRecent = stats?.recentRequests
                ?.filter { it.provider.equals(provider.provider, ignoreCase = true) }
                ?.groupBy { it.model }
                ?.map { (modelName, items) ->
                    ModelStat(
                        requests = items.size.toLong(),
                        promptTokens = items.sumOf { it.promptTokens },
                        completionTokens = items.sumOf { it.completionTokens },
                        cachedTokens = items.sumOf { it.cachedTokens },
                        cost = 0.0,
                        rawModel = modelName,
                        provider = provider.provider
                    )
                }
                .orEmpty()

            if (fromRecent.isNotEmpty()) {
                fromRecent.sortedByDescending { it.requests }
            } else {
                listOf(
                    ModelStat(
                        requests = stat?.requests ?: 0L,
                        promptTokens = stat?.promptTokens ?: 0L,
                        completionTokens = stat?.completionTokens ?: 0L,
                        cachedTokens = stat?.cachedTokens ?: 0L,
                        cost = stat?.cost ?: 0.0,
                        rawModel = "${provider.provider.lowercase()}-default",
                        provider = provider.provider
                    )
                )
            }
        }
    }

    val activeModels = stats?.activeRequests
        ?.filter { it.provider.equals(provider.provider, ignoreCase = true) }
        ?.map { it.model }
        ?.distinct()
        .orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ObsidianSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextTertiary) }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header: Avatar 48x48dp + Nama + Status Pill
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = meta.color.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, meta.color.copy(alpha = 0.55f)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = meta.textIcon,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = meta.color
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = meta.name.ifBlank { provider.provider },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = provider.provider.lowercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    }

                    // Operational Status Badge (F-08: disciplined 6dp radius)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, if (activeModels.isNotEmpty()) NeonEmerald.copy(alpha = 0.5f) else ObsidianBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (activeModels.isNotEmpty()) NeonEmerald else NeonCyan, CircleShape)
                            )
                            Text(
                                text = if (activeModels.isNotEmpty()) "ACTIVE" else "ONLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = if (activeModels.isNotEmpty()) NeonEmerald else NeonCyan
                            )
                        }
                    }
                }
            }

            // Active Model Stream Notice (F-05: remove emoji decoration)
            if (activeModels.isNotEmpty()) {
                item {
                    Surface(
                        color = ColorSuccessGlow,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(NeonEmerald, CircleShape)
                            )
                            Text(
                                text = "Serving live: ${activeModels.joinToString(", ")}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonEmerald
                            )
                        }
                    }
                }
            }

            // 2. Metrics Triad Card for Provider
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WebStyleCard(
                        label = "REQUESTS",
                        value = DecimalFormat("#,###").format(stat?.requests ?: 0L),
                        valueColor = TextPrimary,
                        accentColor = NeonCyan,
                        isCompact = true,
                        modifier = Modifier.weight(1f)
                    )
                    WebStyleCard(
                        label = "COST",
                        value = "$${DecimalFormat("#0.000").format(stat?.cost ?: 0.0)}",
                        valueColor = NeonAmber,
                        accentColor = NeonAmber,
                        isCompact = true,
                        modifier = Modifier.weight(1f)
                    )
                    WebStyleCard(
                        label = "TOKENS",
                        value = formatTokens((stat?.promptTokens ?: 0L) + (stat?.completionTokens ?: 0L)),
                        valueColor = NeonViolet,
                        accentColor = NeonViolet,
                        isCompact = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Traffic Share & Utilization Card (Honest Router Telemetry - F-02 Fixed)
            item {
                val totalRouterReqs = (stats?.totalRequests ?: 0L).coerceAtLeast(1L)
                val providerReqs = stat?.requests ?: 0L
                val trafficSharePct = (providerReqs.toFloat() / totalRouterReqs * 100f).coerceIn(0f, 100f)

                val totalProvTokens = (stat?.promptTokens ?: 0L) + (stat?.completionTokens ?: 0L)
                val avgTokensPerReq = if (providerReqs > 0) totalProvTokens / providerReqs else 0L

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRAFFIC SHARE & UTILIZATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = TextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NeonCyan.copy(alpha = 0.15f),
                                border = BorderStroke(0.6.dp, NeonCyan.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${DecimalFormat("#0.0").format(trafficSharePct)}% SHARE",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Traffic Share Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(ObsidianBorder, RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (trafficSharePct / 100f).coerceIn(0.02f, 1f))
                                    .fillMaxHeight()
                                    .background(NeonCyan, RoundedCornerShape(3.dp))
                            )
                        }

                        // Real Operational Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("AVG TOKENS / REQ", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextTertiary)
                                Text(
                                    text = "${formatTokens(avgTokensPerReq)} tok",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("INPUT / OUTPUT BREAKDOWN", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextTertiary)
                                Text(
                                    text = "${formatTokens(stat?.promptTokens ?: 0L)} in • ${formatTokens(stat?.completionTokens ?: 0L)} out",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Latency Dynamics (Real Telemetry Extraction - F-03 Fixed)
            item {
                val providerRecent = remember(stats, provider.provider) {
                    stats?.recentRequests?.filter {
                        it.provider.equals(provider.provider, ignoreCase = true)
                    }.orEmpty()
                }
                val latencySamples = remember(providerRecent) {
                    providerRecent.mapNotNull { it.latencyMs }.filter { it > 0 }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECENT LATENCY DYNAMICS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = TextSecondary
                            )
                            if (latencySamples.isNotEmpty()) {
                                Text(
                                    text = "${latencySamples.size} SAMPLES",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }

                        if (latencySamples.isNotEmpty()) {
                            val minLatency = latencySamples.minOrNull() ?: 0L
                            val avgLatency = latencySamples.average().toLong()
                            val maxLatency = latencySamples.maxOrNull() ?: 0L

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LatencyMetricChip(
                                    label = "MIN",
                                    value = "${minLatency}ms",
                                    color = NeonEmerald,
                                    modifier = Modifier.weight(1f)
                                )
                                LatencyMetricChip(
                                    label = "AVG",
                                    value = "${avgLatency}ms",
                                    color = NeonCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                LatencyMetricChip(
                                    label = "MAX",
                                    value = "${maxLatency}ms",
                                    color = if (maxLatency > 1500) NeonCoral else NeonAmber,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (latencySamples.size >= 2) {
                                RealLatencySequenceChart(
                                    samples = latencySamples.take(16).reversed(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recent latency telemetry recorded for this provider",
                                    fontSize = 11.5.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }

            // 5. Model Breakdown List (Clean Sans Typography per F-06)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MODEL BREAKDOWN (${modelsForProvider.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "REQS • TOKENS • COST",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = TextTertiary
                    )
                }
            }

            items(modelsForProvider) { modelStat ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianSurfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(0.8.dp, ObsidianBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = modelStat.rawModel.ifBlank { "default-model" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${DecimalFormat("#,###").format(modelStat.requests)} requests",
                                fontSize = 10.5.sp,
                                color = TextTertiary
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = formatTokens(modelStat.totalTokens),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonViolet
                            )
                            Text(
                                text = "$${DecimalFormat("#0.000").format(modelStat.cost)}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LatencyMetricChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = ObsidianSurface,
        border = BorderStroke(0.8.dp, ObsidianBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                color = TextTertiary
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Real Latency Sequence Line Chart connecting actual historical sample points.
 */
@Composable
fun RealLatencySequenceChart(
    samples: List<Long>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (samples.size < 2) return@Canvas

        val width = size.width
        val height = size.height
        val maxVal = samples.maxOrNull()?.toFloat()?.coerceAtLeast(100f) ?: 100f
        val minVal = (samples.minOrNull()?.toFloat() ?: 0f).coerceAtLeast(0f)
        val range = (maxVal - minVal).coerceAtLeast(10f)

        // Subtle horizontal guide lines
        val gridLines = 2
        for (i in 1..gridLines) {
            val y = height * (i.toFloat() / (gridLines + 1))
            drawLine(
                color = ObsidianBorder,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
            )
        }

        val stepX = width / (samples.size - 1)
        val path = Path()
        val fillPath = Path()

        samples.forEachIndexed { i, sample ->
            val y = height - ((sample.toFloat() - minVal) / range) * (height * 0.78f) - (height * 0.11f)
            val x = i * stepX
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevY = height - ((samples[i - 1].toFloat() - minVal) / range) * (height * 0.78f) - (height * 0.11f)
                val cX = (prevX + x) / 2f
                path.cubicTo(cX, prevY, cX, y, x, y)
                fillPath.cubicTo(cX, prevY, cX, y, x, y)
            }
        }
        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(NeonCyan.copy(alpha = 0.22f), Color.Transparent)
            )
        )
        drawPath(
            path = path,
            color = NeonCyan,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
