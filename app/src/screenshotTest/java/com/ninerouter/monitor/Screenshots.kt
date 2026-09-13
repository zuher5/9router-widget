package com.ninerouter.monitor

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.ninerouter.monitor.ui.dashboard.DashboardContent
import com.ninerouter.monitor.ui.setup.SetupScreen
import com.ninerouter.monitor.ui.solar.SolarSystemView
import com.ninerouter.monitor.ui.theme.NineRouterTheme
import com.ninerouter.monitor.widget.SolarSystemRenderer

class Screenshots {

    @PreviewTest
    @Preview(
        name = "Dashboard Dark",
        showBackground = true,
        uiMode = Configuration.UI_MODE_NIGHT_YES
    )
    @Composable
    fun PreviewDashboardDark() {
        NineRouterTheme(darkTheme = true) {
            Surface {
                DashboardContent(
                    stats = PreviewData.sampleStats,
                    providers = PreviewData.sampleProviders,
                    selectedPeriod = "today",
                    isLoading = false,
                    isOffline = false,
                    isStreaming = true,
                    errorMessage = null
                )
            }
        }
    }

    @PreviewTest
    @Preview(
        name = "Setup Dark with Error",
        showBackground = true,
        uiMode = Configuration.UI_MODE_NIGHT_YES
    )
    @Composable
    fun PreviewSetupDarkError() {
        NineRouterTheme(darkTheme = true) {
            Surface {
                SetupScreen(
                    initialUrl = "http://192.168.1.50:20128",
                    isLoading = false,
                    errorMessage = "Password salah. Sisa kesempatan: 3",
                    onLoginClick = { _, _ -> }
                )
            }
        }
    }

    @PreviewTest
    @Preview(name = "Solar System Only", showBackground = true, widthDp = 360, heightDp = 320)
    @Composable
    fun PreviewSolarSystem() {
        NineRouterTheme(darkTheme = true) {
            Surface(modifier = Modifier.fillMaxSize()) {
                SolarSystemView(
                    stats = PreviewData.sampleStats,
                    providers = PreviewData.sampleProviders,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    @PreviewTest
    @Preview(name = "Solar Widget Mockup", showBackground = true, widthDp = 320, heightDp = 150)
    @Composable
    fun PreviewSolarWidget() {
        // Reproduksi visual widget Glance 4x2 memakai render bitmap topology baru (single image with summary)
        val bitmap = SolarSystemRenderer.render(
            stats = PreviewData.sampleStats,
            providers = PreviewData.sampleProviders,
            widthPx = 420,
            heightPx = 200,
            isDark = true,
            drawSummary = true
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0D14), RoundedCornerShape(16.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Solar System Widget",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
