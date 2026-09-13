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

class NineRouterSmallWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? NineRouterApp
        val stats = app?.repository?.getCachedStats()

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "9Router Usage",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))

                    if (stats != null) {
                        Text(
                            text = "Tokens: ${formatTokens(stats.totalTokens)}",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary)
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "Requests: ${stats.totalRequests}",
                            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "Success: ${DecimalFormat("#0.0").format(stats.derivedSuccessRate)}%",
                            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    } else {
                        Text(
                            text = "Buka app untuk connect",
                            style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    }
                }
            }
        }
    }
}

class NineRouterSmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NineRouterSmallWidget()
}
