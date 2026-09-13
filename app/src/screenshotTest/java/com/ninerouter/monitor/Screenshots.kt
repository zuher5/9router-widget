package com.ninerouter.monitor

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.tools.screenshot.PreviewTest
import com.ninerouter.monitor.ui.dashboard.DashboardContent
import com.ninerouter.monitor.ui.setup.SetupScreen
import com.ninerouter.monitor.ui.solar.SolarSystemView
import com.ninerouter.monitor.ui.theme.NineRouterBrand
import com.ninerouter.monitor.ui.theme.NineRouterTheme
import com.ninerouter.monitor.widget.SolarSystemRenderer
import java.text.DecimalFormat

class Screenshots {

    @PreviewTest
    @Preview(name = "Dashboard Light", showBackground = true)
    @Composable
    fun PreviewDashboardLight() {
        NineRouterTheme(darkTheme = false) {
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
    @Preview(name = "Setup Light", showBackground = true)
    @Composable
    fun PreviewSetupLight() {
        NineRouterTheme(darkTheme = false) {
            Surface {
                SetupScreen(
                    initialUrl = "http://192.168.1.50:20128",
                    isLoading = false,
                    errorMessage = null,
                    onLoginClick = { _, _ -> }
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
        // Reproduksi visual widget Glance 4x2 memakai render bitmap topology baru
        val bitmap = SolarSystemRenderer.render(
            stats = PreviewData.sampleStats,
            providers = PreviewData.sampleProviders,
            widthPx = 600,
            heightPx = 280,
            isDark = true
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Solar System Widget",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("9Router", color = NineRouterBrand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("•", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${DecimalFormat("#,###").format(PreviewData.sampleStats.totalRequests)} reqs",
                        color = Color(0xFFEDEDED),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("•", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "$${DecimalFormat("#0.000").format(PreviewData.sampleStats.totalCost)}",
                        color = Color(0xFFEDEDED),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
