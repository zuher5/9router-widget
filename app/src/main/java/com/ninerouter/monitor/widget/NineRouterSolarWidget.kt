package com.ninerouter.monitor.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ninerouter.monitor.MainActivity
import com.ninerouter.monitor.R
import com.ninerouter.monitor.data.auth.SessionManager
import com.ninerouter.monitor.data.network.NineRouterApiClient
import com.ninerouter.monitor.data.repository.UsageRepository
import java.text.DecimalFormat

class NineRouterSolarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sessionManager = SessionManager(context)
        val apiClient = NineRouterApiClient()
        val repository = UsageRepository(context, apiClient, sessionManager)

        val stats = repository.getCachedStats()

        provideContent {
            val bgDark = ColorProvider(android.graphics.Color.rgb(0x1A, 0x1A, 0x1A))
            val textLight = ColorProvider(android.graphics.Color.rgb(0xED, 0xED, 0xED))
            val textMuted = ColorProvider(android.graphics.Color.rgb(0x9C, 0xA3, 0xAF))
            val brandColor = ColorProvider(android.graphics.Color.rgb(0xE5, 0x6A, 0x4A))

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgDark)
                    .padding(8.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                if (stats != null) {
                    val bitmap = SolarSystemRenderer.render(
                        stats = stats,
                        widthPx = 600,
                        heightPx = 280,
                        isDark = true
                    )

                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Grafik Solar System (Image dari Bitmap hasil render Canvas)
                        Image(
                            provider = ImageProvider(bitmap),
                            contentDescription = "Solar System Usage",
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .defaultWeight()
                        )

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // 1 Baris Ringkasan: Total Requests • Biaya
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "9Router",
                                style = TextStyle(
                                    color = brandColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = "•",
                                style = TextStyle(color = textMuted, fontSize = 11.sp)
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = "${DecimalFormat("#,###").format(stats.totalRequests)} reqs",
                                style = TextStyle(
                                    color = textLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = "•",
                                style = TextStyle(color = textMuted, fontSize = 11.sp)
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = "$${DecimalFormat("#0.000").format(stats.totalCost)}",
                                style = TextStyle(
                                    color = textLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "9Router",
                            style = TextStyle(
                                color = brandColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = context.getString(R.string.widget_no_data),
                            style = TextStyle(
                                color = textMuted,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class SolarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NineRouterSolarWidget()
}
