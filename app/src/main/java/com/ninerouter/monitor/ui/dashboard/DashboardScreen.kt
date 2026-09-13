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
import com.ninerouter.monitor.data.model.UsageStatsResponse
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
        selectedPeriod = state.selectedPeriod,
        isLoading = state.isLoading,
        isOffline = state.isOffline,
        isStreaming = state.isStreaming,
        errorMessage = state.errorMessage,
        onPeriodSelected = { viewModel.onPeriodSelected(it) },
        onRefresh = { viewModel.refresh() },
        onLogout = { viewModel.logout(onLogoutClick) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    stats: UsageStatsResponse?,
    selectedPeriod: String,
    isLoading: Boolean,
    isOffline: Boolean,
    isStreaming: Boolean,
    errorMessage: String?,
    onPeriodSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Sesuai SegmentedControl di web 9Router: Today, 24h, 7D, 30D, 60D
    val periods = listOf(
        "today" to "Today",
        "24h" to "24h",
        "7d" to "7D",
        "30d" to "30D",
        "60d" to "60D"
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "9Router",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = NineRouterBrand
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        if (isStreaming) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(ColorSuccess, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh) {
                        Text(
                            text = stringResource(R.string.refresh),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = onLogout) {
                        Text(
                            text = stringResource(R.string.logout),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Segmented Period Selector bergaya 9Router web
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        periods.forEach { (key, label) ->
                            val selected = selectedPeriod == key
                            Surface(
                                onClick = { onPeriodSelected(key) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) NineRouterBrand else Color.Transparent,
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
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.offline_mode_cached),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (stats != null) {
                // Overview Cards 1: Total Requests & Est. Cost (mengikuti OverviewCards.js)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WebStyleCard(
                            label = "TOTAL REQUESTS",
                            value = DecimalFormat("#,###").format(stats.totalRequests),
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        WebStyleCard(
                            label = "EST. COST",
                            value = "~$${DecimalFormat("#0.00").format(stats.totalCost)}",
                            valueColor = ColorWarning,
                            subtitle = "Estimated, not actual billing",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Overview Cards 2: Input Tokens, Cached Tokens, Output Tokens (3 kolom meniru web)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WebStyleCard(
                            label = "INPUT",
                            value = formatTokens(stats.totalPromptTokens),
                            valueColor = NineRouterBrand,
                            modifier = Modifier.weight(1f)
                        )
                        WebStyleCard(
                            label = "CACHED",
                            value = formatTokens(stats.totalCachedTokens),
                            valueColor = ColorInfo,
                            modifier = Modifier.weight(1f)
                        )
                        WebStyleCard(
                            label = "OUTPUT",
                            value = formatTokens(stats.totalCompletionTokens),
                            valueColor = ColorSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Solar System Card (Matahari = total, Planet = model)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SOLAR SYSTEM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${stats.byModel.size} models active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            SolarSystemView(
                                stats = stats,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(290.dp)
                            )
                        }
                    }
                }

                // Recent Requests: Tabel ramping meniru RecentRequests di UsageStats.js
                if (stats.recentRequests.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Header tabel
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RECENT REQUESTS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.8.dp)

                                // Baris-baris request
                                stats.recentRequests.take(12).forEachIndexed { index, req ->
                                    WebStyleRecentRow(req)
                                    if (index < stats.recentRequests.take(12).lastIndex) {
                                        Divider(
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            thickness = 0.5.dp
                                        )
                                    }
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Kartu metrik berdesain 1:1 seperti `Card` di `OverviewCards.js` 9Router web:
 * background surface, border outline halus, radius 14dp, label uppercase font-semibold, angka 2xl bold bergradasi warna.
 */
@Composable
fun WebStyleCard(
    label: String,
    value: String,
    valueColor: Color,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Baris recent request yang persis dengan tabel RecentRequests di web:
 * dot status bulat 6dp, font monospace untuk model, prompt token (primary↑) dan completion token (success↓).
 */
@Composable
fun WebStyleRecentRow(item: RecentRequestItem) {
    val isOk = item.status.equals("ok", ignoreCase = true) || item.status.equals("success", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dot status
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(if (isOk) ColorSuccess else ColorDanger, CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Model & Provider
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.model.ifEmpty { "unknown" },
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.provider.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // In / Out tokens persis di web: text-primary prompt↑ text-success completion↓
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${formatTokens(item.promptTokens)}↑",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = NineRouterBrand
            )
            Text(
                text = "${formatTokens(item.completionTokens)}↓",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = ColorSuccess
            )
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
