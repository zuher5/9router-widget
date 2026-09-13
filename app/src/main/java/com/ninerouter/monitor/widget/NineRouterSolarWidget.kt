package com.ninerouter.monitor.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
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
import kotlin.math.roundToInt

class NineRouterSolarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sessionManager = SessionManager(context)
        val apiClient = NineRouterApiClient()
        val repository = UsageRepository(context, apiClient, sessionManager)

        val stats = repository.getCachedStats()
        val providers = repository.getCachedProviders()

        provideContent {
            val bgDark = ColorProvider(R.color.widget_bg_dark)
            val textMuted = ColorProvider(R.color.widget_text_muted)
            val textLight = ColorProvider(R.color.widget_text_light)
            val brandColor = ColorProvider(R.color.widget_brand)
            val cyanColor = ColorProvider(R.color.widget_cyan)
            val emeraldColor = ColorProvider(R.color.widget_emerald)
            val amberColor = ColorProvider(R.color.widget_amber)

            val glanceContext = LocalContext.current
            val glanceSize = LocalSize.current
            val metrics = glanceContext.resources.displayMetrics
            val density = metrics.density

            // Hitung ukuran dp actual widget di home screen
            val widthDp = if (glanceSize.width.value > 0f) glanceSize.width.value else 340f
            val heightDp = if (glanceSize.height.value > 0f) glanceSize.height.value else 170f

            // Area canvas menyisakan baris header (~26dp) dan footer (~22dp)
            val canvasHeightDp = (heightDp - 54f).coerceAtLeast(80f)

            val targetWPx = (widthDp * density).roundToInt()
            val targetHPx = (canvasHeightDp * density).roundToInt()

            // Supersample resolusi tinggi HD tapi aman dari batas 1.5MB RemoteViews
            val safeWPx = targetWPx.coerceIn(280, 720)
            val safeHPx = targetHPx.coerceIn(120, 380)

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(16.dp)
                    .background(bgDark)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                if (stats != null) {
                    val bitmap = SolarSystemRenderer.render(
                        stats = stats,
                        providers = providers,
                        widthPx = safeWPx,
                        heightPx = safeHPx,
                        isDark = true,
                        drawSummary = false,
                        densityDpi = metrics.densityDpi
                    )

                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. TOP BAR NATIVE GLANCE
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "9Router",
                                    style = TextStyle(
                                        color = brandColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                Text(
                                    text = "● LIVE",
                                    style = TextStyle(
                                        color = cyanColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            val reqsFormatted = DecimalFormat("#,###").format(stats.totalRequests)
                            val costFormatted = DecimalFormat("#0.00").format(stats.totalCost)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$reqsFormatted reqs",
                                    style = TextStyle(
                                        color = textLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = "  •  ",
                                    style = TextStyle(
                                        color = textMuted,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = "$$costFormatted",
                                    style = TextStyle(
                                        color = amberColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(3.dp))

                        // 2. CENTER AREA: HD Solar Topology Radar Scope
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .defaultWeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(bitmap),
                                contentDescription = "9Router Topology",
                                contentScale = ContentScale.Fit,
                                modifier = GlanceModifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(3.dp))

                        // 3. BOTTOM BAR NATIVE GLANCE
                        val activeReq = stats.activeRequests.firstOrNull()
                        val lastReq = stats.recentRequests.firstOrNull()
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeReq != null) {
                                val modelName = activeReq.model.ifEmpty { activeReq.provider }
                                Text(
                                    text = "⚡ Active: ${activeReq.provider} / $modelName",
                                    style = TextStyle(
                                        color = emeraldColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                            } else if (lastReq != null) {
                                val latencyStr = if (lastReq.latencyMs > 0) " • ${lastReq.latencyMs}ms" else ""
                                Text(
                                    text = "Last: ${lastReq.model}$latencyStr",
                                    style = TextStyle(
                                        color = textMuted,
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1
                                )
                            } else {
                                Text(
                                    text = "System Ready",
                                    style = TextStyle(
                                        color = textMuted,
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            val providerCount = stats.byProvider.size
                            Text(
                                text = "$providerCount providers",
                                style = TextStyle(
                                    color = textMuted,
                                    fontSize = 10.sp
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
