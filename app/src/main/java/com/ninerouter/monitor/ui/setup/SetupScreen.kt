package com.ninerouter.monitor.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ninerouter.monitor.R
import com.ninerouter.monitor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    initialUrl: String = "",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onLoginClick: (url: String, password: String) -> Unit
) {
    var urlText by remember { mutableStateOf(if (initialUrl.isNotBlank()) initialUrl else "http://") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val presets = listOf(
        "http://localhost:20128",
        "http://192.168.1.",
        "http://10.0.0."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Branding Emblem & Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Outer glow halo
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(NineRouterBrandGlow, CircleShape)
                    )
                    // Inner emblem
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.5.dp, NineRouterBrand)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "9R",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = NineRouterBrand,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Text(
                    text = "9Router Monitor",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorTextPrimary
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianCardElevated,
                    border = BorderStroke(1.dp, ObsidianGlassBorder)
                ) {
                    Text(
                        text = "AI GATEWAY OBSERVABILITY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NineRouterBrand,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.setup_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 2. Glassmorphic Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianGlassBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Input URL
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.setup_url_label).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            color = ColorTextSecondary
                        )

                        OutlinedTextField(
                            value = urlText,
                            onValueChange = { urlText = it },
                            placeholder = { Text("http://192.168.1.50:20128", color = ColorTextMuted) },
                            singleLine = true,
                            leadingIcon = {
                                Text("🌐", fontSize = 16.sp)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NineRouterBrand,
                                unfocusedBorderColor = ObsidianGlassBorder,
                                focusedContainerColor = ObsidianSurfaceVariant,
                                unfocusedContainerColor = ObsidianSurfaceVariant,
                                focusedTextColor = ColorTextPrimary,
                                unfocusedTextColor = ColorTextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Quick Presets Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.forEach { preset ->
                                Surface(
                                    onClick = { urlText = preset },
                                    shape = RoundedCornerShape(8.dp),
                                    color = ObsidianCardElevated,
                                    border = BorderStroke(0.8.dp, ObsidianGlassBorderSubtle)
                                ) {
                                    Text(
                                        text = preset.removePrefix("http://"),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ColorTextSecondary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Input Password
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.setup_password_label).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            color = ColorTextSecondary
                        )

                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = { passwordText = it },
                            placeholder = { Text("••••••••", color = ColorTextMuted) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            leadingIcon = {
                                Text("🔒", fontSize = 16.sp)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(if (passwordVisible) "👁" else "👁‍🗨", fontSize = 15.sp)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NineRouterBrand,
                                unfocusedBorderColor = ObsidianGlassBorder,
                                focusedContainerColor = ObsidianSurfaceVariant,
                                unfocusedContainerColor = ObsidianSurfaceVariant,
                                focusedTextColor = ColorTextPrimary,
                                unfocusedTextColor = ColorTextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (!isLoading && urlText.isNotBlank()) {
                                        onLoginClick(urlText, passwordText)
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Error Message Banner
                    AnimatedVisibility(
                        visible = !errorMessage.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = ColorDangerGlow,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ColorDanger.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("⚠️", fontSize = 14.sp)
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = Color(0xFFFCA5A5),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Connect Button with Gradient
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onLoginClick(urlText, passwordText)
                        },
                        enabled = !isLoading && urlText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NineRouterBrand,
                            disabledContainerColor = NineRouterBrand.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.setup_button_login),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text("→", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
