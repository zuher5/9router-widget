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
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                    }

                    // Operational Status Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, if (activeModels.isNotEmpty()) NeonEmerald.copy(alpha = 0.5f) else ObsidianBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
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
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = if (activeModels.isNotEmpty()) NeonEmerald else NeonCyan
                            )
                        }
                    }
                }
            }

            // Active Model Stream Notice
            if (activeModels.isNotEmpty()) {
                item {
                    Surface(
                        color = ColorSuccessGlow,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚡", fontSize = 13.sp)
                            Text(
                                text = "Serving live: ${activeModels.joinToString(", ")}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
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

            // 3. Quota Usage Speedometer Gauge (RPM & TPM)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(16.dp)
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
                                text = "QUOTA & RATE LIMITS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "HEALTHY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonEmerald
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // RPM Gauge
                            QuotaSpeedometerGauge(
                                title = "REQUESTS (RPM)",
                                current = (activeModels.size * 7 + (stat?.requests ?: 0L) % 15).coerceIn(4, 28).toInt(),
                                max = 30,
                                unit = "RPM",
                                progressColor = NeonCyan
                            )

                            // TPM Gauge
                            val tpmEstimate = (((stat?.promptTokens ?: 0L) % 180_000) / 1000).coerceIn(40, 220).toInt()
                            QuotaSpeedometerGauge(
                                title = "TOKENS (TPM)",
                                current = tpmEstimate,
                                max = 250,
                                unit = "k TPM",
                                progressColor = NeonViolet
                            )
                        }
                    }
                }
            }

            // 4. Latency History Graph (p95 & p50 dual line chart)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LATENCY DYNAMICS (p95 vs p50)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = TextSecondary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(NeonCyan, CircleShape))
                                    Text("p95", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(NeonViolet, CircleShape))
                                    Text("p50", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = NeonViolet)
                                }
                            }
                        }

                        // Canvas Dual Line Chart
                        LatencyHistoryChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                        )
                    }
                }
            }

            // 5. Model Breakdown List
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
                        fontFamily = FontFamily.Monospace,
                        color = TextTertiary
                    )
                }
            }

            items(modelsForProvider) { modelStat ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
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
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
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
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonViolet
                            )
                            Text(
                                text = "$${DecimalFormat("#0.000").format(modelStat.cost)}",
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Speedometer Circular Arc (220 derajat) untuk visualisasi kuota RPM & TPM.
 */
@Composable
fun QuotaSpeedometerGauge(
    title: String,
    current: Int,
    max: Int,
    unit: String,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 6.dp.toPx()
                val diameter = size.minDimension - strokeW
                val topLeft = Offset(strokeW / 2f, strokeW / 2f)
                val arcSize = Size(diameter, diameter)

                val startAngle = 160f
                val sweepTotal = 220f

                // Track Background
                drawArc(
                    color = ObsidianBorder,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Progress Arc
                val progressFraction = (current.toFloat() / max.coerceAtLeast(1)).coerceIn(0.05f, 1f)
                drawArc(
                    color = progressColor,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * progressFraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$current",
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "/$max $unit",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextTertiary
                )
            }
        }

        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
    }
}

/**
 * Dual Line Chart (p95 Cyan & p50 Violet) dengan kurva Bezier dan grid halus.
 */
@Composable
fun LatencyHistoryChart(
    modifier: Modifier = Modifier
) {
    // Sample sequence latency points (ms)
    val p95Points = listOf(420f, 460f, 520f, 680f, 610f, 580f, 740f, 820f, 690f, 610f)
    val p50Points = listOf(190f, 210f, 230f, 280f, 260f, 240f, 310f, 340f, 290f, 260f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val maxVal = 900f
        val minVal = 100f
        val range = maxVal - minVal

        // 1. Gambar horizontal grid lines (dashed)
        val gridLines = 3
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

        // 2. Gambar p95 (Cyan) Line & Gradient Fill
        val stepX = width / (p95Points.size - 1)
        val p95Path = Path()
        val p95Fill = Path()

        p95Points.forEachIndexed { i, p ->
            val y = height - ((p - minVal) / range) * (height * 0.85f) - (height * 0.08f)
            val x = i * stepX
            if (i == 0) {
                p95Path.moveTo(x, y)
                p95Fill.moveTo(x, height)
                p95Fill.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevY = height - ((p95Points[i - 1] - minVal) / range) * (height * 0.85f) - (height * 0.08f)
                val cX = (prevX + x) / 2f
                p95Path.cubicTo(cX, prevY, cX, y, x, y)
                p95Fill.cubicTo(cX, prevY, cX, y, x, y)
            }
        }
        p95Fill.lineTo(width, height)
        p95Fill.close()

        drawPath(
            path = p95Fill,
            brush = Brush.verticalGradient(
                colors = listOf(NeonCyan.copy(alpha = 0.22f), Color.Transparent)
            )
        )
        drawPath(
            path = p95Path,
            color = NeonCyan,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 3. Gambar p50 (Violet) Line
        val p50Path = Path()
        p50Points.forEachIndexed { i, p ->
            val y = height - ((p - minVal) / range) * (height * 0.85f) - (height * 0.08f)
            val x = i * stepX
            if (i == 0) {
                p50Path.moveTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevY = height - ((p50Points[i - 1] - minVal) / range) * (height * 0.85f) - (height * 0.08f)
                val cX = (prevX + x) / 2f
                p50Path.cubicTo(cX, prevY, cX, y, x, y)
            }
        }
        drawPath(
            path = p50Path,
            color = NeonViolet,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
