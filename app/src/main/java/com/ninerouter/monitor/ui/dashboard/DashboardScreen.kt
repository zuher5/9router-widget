package com.ninerouter.monitor.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninerouter.monitor.R
import com.ninerouter.monitor.data.model.RecentRequestItem
import com.ninerouter.monitor.ui.solar.SolarSystemView
import com.ninerouter.monitor.ui.theme.ColorDanger
import com.ninerouter.monitor.ui.theme.ColorSuccess
import com.ninerouter.monitor.ui.theme.NineRouterBrand
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onLogoutClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val periods = listOf(
        "today" to R.string.period_today,
        "24h" to R.string.period_24h,
        "7d" to R.string.period_7d,
        "30d" to R.string.period_30d,
        "60d" to R.string.period_60d
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "9Router",
                            fontWeight = FontWeight.Bold,
                            color = NineRouterBrand
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (state.isStreaming) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ColorSuccess, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text(stringResource(R.string.refresh), color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { viewModel.logout(onLogoutClick) }) {
                        Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.error)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Filter Periode (mengikuti web: today, 24h, 7d, 30d, 60d)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    periods.forEach { (key, labelRes) ->
                        FilterChip(
                            selected = state.selectedPeriod == key,
                            onClick = { viewModel.onPeriodSelected(key) },
                            label = { Text(stringResource(labelRes), fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NineRouterBrand,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (state.isOffline) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
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

            val stats = state.stats
            if (stats != null) {
                // Baris Metrik 1: Total Requests & Total Token
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = stringResource(R.string.total_requests),
                            value = DecimalFormat("#,###").format(stats.totalRequests),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = stringResource(R.string.total_tokens),
                            value = formatTokens(stats.totalTokens),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Baris Metrik 2: Biaya & Success Rate
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = stringResource(R.string.total_cost),
                            value = "$${DecimalFormat("#0.0000").format(stats.totalCost)}",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = stringResource(R.string.success_rate),
                            value = "${DecimalFormat("#0.0").format(stats.derivedSuccessRate)}%",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Solar System Card (Matahari = total, Planet = model)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.solar_system),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${stats.byModel.size} models",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            SolarSystemView(
                                stats = stats,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                            )
                        }
                    }
                }

                // Recent Requests
                if (stats.recentRequests.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.recent_requests),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(stats.recentRequests.take(15)) { item ->
                        RecentRequestRow(item)
                    }
                }
            } else if (state.isLoading) {
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
                            text = state.errorMessage ?: stringResource(R.string.no_data),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RecentRequestRow(item: RecentRequestItem) {
    val isOk = item.status.equals("ok", ignoreCase = true) || item.status.equals("success", ignoreCase = true)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.model.ifEmpty { "unknown" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.provider.uppercase()} • In: ${formatTokens(item.promptTokens)} Out: ${formatTokens(item.completionTokens)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        if (isOk) ColorSuccess else ColorDanger,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.status.uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
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
