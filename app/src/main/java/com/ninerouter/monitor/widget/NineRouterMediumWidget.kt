package com.ninerouter.monitor.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ninerouter.monitor.NineRouterApp
import com.ninerouter.monitor.ui.dashboard.formatTokens
import java.text.DecimalFormat

class NineRouterMediumWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? NineRouterApp
        val stats = app?.repository?.getCachedStats()

        provideContent {
            GlanceTheme {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "9Router Monitor",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        if (stats != null) {
                            Text(
                                text = "${formatTokens(stats.totalTokens)} Tokens",
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary)
                            )
                            Text(
                                text = "${stats.totalRequests} Requests",
                                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                            )
                            Text(
                                text = "Sukses: ${DecimalFormat("#0.0").format(stats.derivedSuccessRate)}%",
                                style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant)
                            )
                        } else {
                            Text(
                                text = "Belum terhubung",
                                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                            )
                        }
                    }

                    if (stats != null && stats.byModel.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Top Model:",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface)
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            val topModels = stats.byModel.values
                                .sortedByDescending { it.totalTokens }
                                .take(2)
                            topModels.forEach { m ->
                                val name = m.rawModel.ifBlank { m.provider }.take(12)
                                Text(
                                    text = "• $name (${formatTokens(m.totalTokens)})",
                                    style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class NineRouterMediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NineRouterMediumWidget()
}
