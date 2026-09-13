package com.ninerouter.monitor.ui.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
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
                            color = NineRouterBrand,
                            letterSpacing = (-0.5).sp
                        )

                        // LIVE Status Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, if (isStreaming) ColorSuccess.copy(alpha = 0.4f) else ObsidianGlassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(if (isStreaming) ColorSuccess else ColorTextMuted, CircleShape)
                                )
                                Text(
                                    text = if (isStreaming) "LIVE" else "OFFLINE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = if (isStreaming) ColorSuccess else ColorTextMuted
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Glass Icon Buttons
                    Surface(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, ObsidianGlassBorder),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⟳", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        onClick = onLogout,
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, ObsidianGlassBorder),
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
            // 1. Floating Segmented Period Capsule
            item {
                Surface(
                    color = ObsidianSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        periods.forEach { (key, label) ->
                            val selected = selectedPeriod == key
                            Surface(
                                onClick = { onPeriodSelected(key) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) NineRouterBrand else Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (selected) Color.White else ColorTextSecondary
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
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ColorWarning.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📡", fontSize = 14.sp)
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
                // 2. Hero Overview Cards: Total Requests & Est. Cost
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WebStyleCard(
                            label = "TOTAL REQUESTS",
                            value = DecimalFormat("#,###").format(stats.totalRequests),
                            valueColor = ColorTextPrimary,
                            accentColor = NineRouterBrand,
                            subtitlePill = "${stats.derivedSuccessRate.toInt()}% OK",
                            subtitlePillColor = ColorSuccess,
                            modifier = Modifier.weight(1.05f)
                        )
                        WebStyleCard(
                            label = "EST. COST",
                            value = "~$${DecimalFormat("#0.00").format(stats.totalCost)}",
                            valueColor = ColorWarning,
                            accentColor = ColorWarning,
                            subtitle = "Token Billing Est.",
                            modifier = Modifier.weight(0.95f)
                        )
                    }
                }

                // 3. Token Flow Triad: Input, Cached, Output
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WebStyleCard(
                            label = "INPUT ↑",
                            value = formatTokens(stats.totalPromptTokens),
                            valueColor = ColorViolet,
                            accentColor = ColorViolet,
                            isCompact = true,
                            modifier = Modifier.weight(1f)
                        )
                        WebStyleCard(
                            label = "CACHED ⚡",
                            value = formatTokens(stats.totalCachedTokens),
                            valueColor = ColorInfo,
                            accentColor = ColorInfo,
                            isCompact = true,
                            modifier = Modifier.weight(1f)
                        )
                        WebStyleCard(
                            label = "OUTPUT ↓",
                            value = formatTokens(stats.totalCompletionTokens),
                            valueColor = ColorSuccess,
                            accentColor = ColorSuccess,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailBottomSheet(
    provider: TopologyProvider,
    stats: UsageStatsResponse?,
    onDismiss: () -> Unit
) {
    val meta = ProviderCatalog.get(provider.provider)
    val stat = stats?.byProvider?.get(provider.provider.lowercase())
    val activeModels = stats?.activeRequests
        ?.filter { it.provider.equals(provider.provider, ignoreCase = true) }
        ?.map { it.model }
        ?.distinct()
        ?: emptyList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = ObsidianSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ColorTextMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Icon tile + Nama Provider + ID
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = meta.color.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, meta.color.copy(alpha = 0.5f)),
                    modifier = Modifier.size(46.dp)
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
                Column {
                    Text(
                        text = meta.name.ifBlank { provider.provider },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    Text(
                        text = provider.provider,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ColorTextSecondary
                    )
                }
            }

            Divider(color = ObsidianGlassBorder)

            // Status koneksi & active models
            if (activeModels.isNotEmpty()) {
                Surface(
                    color = ColorSuccessGlow,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ColorSuccess.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(ColorSuccess, CircleShape)
                        )
                        Text(
                            text = "Active Now: ${activeModels.joinToString(", ")}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorSuccess
                        )
                    }
                }
            }

            // Metrik kartu ringkasan provider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WebStyleCard(
                    label = "REQUESTS",
                    value = DecimalFormat("#,###").format(stat?.requests ?: 0L),
                    valueColor = ColorTextPrimary,
                    accentColor = NineRouterBrand,
                    modifier = Modifier.weight(1f)
                )
                WebStyleCard(
                    label = "COST",
                    value = "$${DecimalFormat("#0.000").format(stat?.cost ?: 0.0)}",
                    valueColor = ColorWarning,
                    accentColor = ColorWarning,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WebStyleCard(
                    label = "PROMPT",
                    value = formatTokens(stat?.promptTokens ?: 0L),
                    valueColor = ColorViolet,
                    accentColor = ColorViolet,
                    isCompact = true,
                    modifier = Modifier.weight(1f)
                )
                WebStyleCard(
                    label = "CACHED",
                    value = formatTokens(stat?.cachedTokens ?: 0L),
                    valueColor = ColorInfo,
                    accentColor = ColorInfo,
                    isCompact = true,
                    modifier = Modifier.weight(1f)
                )
                WebStyleCard(
                    label = "OUTPUT",
                    value = formatTokens(stat?.completionTokens ?: 0L),
                    valueColor = ColorSuccess,
                    accentColor = ColorSuccess,
                    isCompact = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Kartu metrik bergaya Glassmorphism dengan garis aksen bercahaya di atas kartu.
 */
@Composable
fun WebStyleCard(
    label: String,
    value: String,
    valueColor: Color,
    accentColor: Color? = null,
    subtitle: String? = null,
    subtitlePill: String? = null,
    subtitlePillColor: Color = ColorSuccess,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianGlassBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Glowing Top Accent Line
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(accentColor)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isCompact) 10.dp else 14.dp, vertical = if (isCompact) 10.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = label,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = ColorTextSecondary
                )
                Text(
                    text = value,
                    fontSize = if (isCompact) 16.sp else 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitlePill != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
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
                } else if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 9.5.sp,
                        color = ColorTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Strip kartu permintaan terbaru:
 * Provider squircle avatar, status pill (200 OK / ERR), monospace model name, dan token In/Out.
 */
@Composable
fun WebStyleRecentRow(item: RecentRequestItem) {
    val isOk = item.status.equals("ok", ignoreCase = true) || item.status.equals("success", ignoreCase = true)
    val meta = ProviderCatalog.get(item.provider)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(0.8.dp, ObsidianGlassBorderSubtle)
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
                // Provider Avatar Tile
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = meta.color.copy(alpha = 0.18f),
                    border = BorderStroke(0.8.dp, meta.color.copy(alpha = 0.4f)),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = meta.textIcon,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = meta.color
                        )
                    }
                }

                // Model name & Provider ID
                Column {
                    Text(
                        text = item.model.ifEmpty { "unknown" },
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTextPrimary,
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
                            color = ColorTextMuted
                        )

                        // Status dot badge
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(if (isOk) ColorSuccess else ColorDanger, CircleShape)
                        )
                        Text(
                            text = if (isOk) "OK" else "ERR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOk) ColorSuccess else ColorDanger
                        )
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
                    fontFamily = FontFamily.Monospace,
                    color = ColorViolet
                )
                Text(
                    text = "${formatTokens(item.completionTokens)}↓",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = ColorSuccess
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
