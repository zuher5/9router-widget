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
import kotlin.math.roundToInt

class NineRouterSolarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sessionManager = SessionManager(context)
        val apiClient = NineRouterApiClient()
        val repository = UsageRepository(context, apiClient, sessionManager)

        val stats = repository.getCachedStats()
        val providers = repository.getCachedProviders()

        provideContent {
            val bgDark = ColorProvider(android.graphics.Color.rgb(0x0B, 0x0D, 0x14))
            val textMuted = ColorProvider(android.graphics.Color.rgb(0x94, 0xA3, 0xB8))
            val brandColor = ColorProvider(android.graphics.Color.rgb(0xFF, 0x6F, 0x59))

            val glanceContext = LocalContext.current
            val glanceSize = LocalSize.current
            val metrics = glanceContext.resources.displayMetrics
            val density = metrics.density

            // Konversi dp ke px yang dibatasi aman agar tidak melampaui batas memori RemoteViews
            val targetWPx = if (glanceSize.width.value > 0f) (glanceSize.width.value * density).roundToInt() else 420
            val targetHPx = if (glanceSize.height.value > 0f) (glanceSize.height.value * density).roundToInt() else 200

            val safeWPx = targetWPx.coerceIn(240, 480)
            val safeHPx = targetHPx.coerceIn(120, 240)

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgDark)
                    .padding(4.dp)
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
                        drawSummary = true,
                        densityDpi = metrics.densityDpi
                    )

                    // Image tunggal full-size menggantikan nested Column + defaultWeight
                    // untuk mencegah bug "can't load image" di launcher RemoteViews
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = "9Router Topology",
                        contentScale = ContentScale.FillBounds,
                        modifier = GlanceModifier.fillMaxSize()
                    )
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
