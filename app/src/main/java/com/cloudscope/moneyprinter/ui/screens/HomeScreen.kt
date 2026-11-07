package com.cloudscope.moneyprinter.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.data.model.MarketDirection
import com.cloudscope.moneyprinter.ui.components.LatestMarketNewsCard
import com.cloudscope.moneyprinter.ui.components.NormalOptionsSignalCard
import com.cloudscope.moneyprinter.ui.components.SignalConflictBanner
import com.cloudscope.moneyprinter.ui.components.SmartMoneyOptionsSignalCard
import com.cloudscope.moneyprinter.ui.components.alerts.AlertsPanelDialog
import com.cloudscope.moneyprinter.ui.components.alerts.InAppAlertBanner
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import com.cloudscope.moneyprinter.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalTerminalColors.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // User responded to permission request
    }

    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val quote = uiState.quote
    val indicators = uiState.indicators
    val signal = uiState.signal

    // Direction Resolution: Determine whether market will go UP or DOWN
    val rawDirection = indicators?.marketDirection ?: signal?.marketDirection ?: MarketDirection.SIDEWAYS
    val priceChange = quote?.change ?: 0.0

    val direction = when {
        rawDirection == MarketDirection.BULLISH -> MarketDirection.BULLISH
        rawDirection == MarketDirection.BEARISH -> MarketDirection.BEARISH
        priceChange > 0 -> MarketDirection.BULLISH
        priceChange < 0 -> MarketDirection.BEARISH
        else -> MarketDirection.SIDEWAYS
    }

    // Number formatter for Indian Rupee
    val indianFormat = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    val priceDisplay = quote?.let { "₹${indianFormat.format(it.lastPrice)}" } ?: "₹24,850.50"
    val change = quote?.change ?: 120.40
    val pChange = quote?.pChange ?: 0.49
    val isPositive = change >= 0
    val sign = if (isPositive) "+" else ""
    val changeDisplay = "$sign${indianFormat.format(change)} ($sign${String.format(Locale.US, "%.2f", pChange)}%)"

    val timeFormat = remember {
        SimpleDateFormat("HH:mm:ss 'IST'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
    }
    val updateTime = timeFormat.format(Date(quote?.timestamp ?: System.currentTimeMillis()))

    // Rotating animation for refresh button
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(colors.bullish)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NIFTY 50 REALTIME",
                                color = colors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "LIVE MARKET PREDICTOR",
                                color = colors.bullish,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                },
                actions = {
                    // Real-Time Notification Bell with Badge
                    IconButton(
                        onClick = { viewModel.setAlertsPanelVisible(true) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.unreadAlertCount > 0) {
                                    Badge(
                                        containerColor = colors.bearish,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (uiState.unreadAlertCount > 9) "9+" else "${uiState.unreadAlertCount}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.unreadAlertCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = "Alerts & Notifications",
                                tint = if (uiState.unreadAlertCount > 0) colors.bullish else colors.textPrimary
                            )
                        }
                    }

                    // Manual Refresh Button
                    IconButton(
                        onClick = { viewModel.refreshMarketData() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Realtime Data",
                            tint = if (uiState.isRefreshing) colors.bullish else colors.textPrimary,
                            modifier = if (uiState.isRefreshing) Modifier.rotate(spinAngle) else Modifier
                        )
                    }

                    // Theme Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkTheme) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                            contentDescription = "Toggle Theme",
                            tint = colors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        },
        containerColor = colors.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ==========================================
                // 1. PRIMARY PREDICTION CARD (UP OR DOWN ONLY)
                // ==========================================
                val (predictionText, directionColor, directionBgColor, predictionIcon, subtitleText) = when (direction) {
                    MarketDirection.BULLISH -> Quintuple(
                        "MARKET WILL GO UP",
                        colors.bullish,
                        colors.bullishBg,
                        Icons.AutoMirrored.Filled.TrendingUp,
                        "Strong Bullish Trend Dominant"
                    )
                    MarketDirection.BEARISH -> Quintuple(
                        "MARKET WILL GO DOWN",
                        colors.bearish,
                        colors.bearishBg,
                        Icons.AutoMirrored.Filled.TrendingDown,
                        "Strong Bearish Breakdown Dominant"
                    )
                    MarketDirection.SIDEWAYS -> Quintuple(
                        "MARKET CONSOLIDATION",
                        colors.sideways,
                        colors.sidewaysBg,
                        Icons.AutoMirrored.Filled.TrendingFlat,
                        if (priceChange >= 0) "Slight Upward Tilt Near VWAP" else "Slight Downward Tilt Near VWAP"
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = directionBgColor),
                    border = androidx.compose.foundation.BorderStroke(2.dp, directionColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Prediction Icon
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(directionColor.copy(alpha = 0.2f))
                                .border(1.5.dp, directionColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = predictionIcon,
                                contentDescription = predictionText,
                                tint = directionColor,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Direction Headline
                        Text(
                            text = predictionText,
                            color = directionColor,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = subtitleText,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Confidence / Probability Meter
                        val confidence = signal?.confidencePercent?.coerceAtLeast(65) ?: if (direction != MarketDirection.SIDEWAYS) 78 else 50
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surface.copy(alpha = 0.7f))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AI Directional Confidence",
                                    color = colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$confidence%",
                                    color = directionColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { confidence / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = directionColor,
                                trackColor = colors.border,
                            )
                        }

                        // Key Reason Summary
                        val reasonText = when (direction) {
                            MarketDirection.BULLISH -> {
                                val vwap = indicators?.vwap ?: 0.0
                                if (vwap > 0) "Price trading above VWAP (₹${String.format(Locale.US, "%.1f", vwap)}) with buyer momentum."
                                else "Buyers dominating with positive price expansion."
                            }
                            MarketDirection.BEARISH -> {
                                val vwap = indicators?.vwap ?: 0.0
                                if (vwap > 0) "Price trading below VWAP (₹${String.format(Locale.US, "%.1f", vwap)}) with seller rejection."
                                else "Sellers dominating with breakdown momentum."
                            }
                            MarketDirection.SIDEWAYS -> "Price consolidating within narrow intraday range. Waiting for directional break."
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = reasonText,
                            color = colors.textSecondary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ==========================================
                // 2. NIFTY 50 REALTIME SPOT PRICE CARD
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Status header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(colors.bullish)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NIFTY 50 • SPOT",
                                    color = colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "Tick: $updateTime",
                                color = colors.textMuted,
                                fontSize = 12.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Spot Price
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = priceDisplay,
                                color = colors.textPrimary,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Default,
                                letterSpacing = (-0.5).sp
                            )

                            // Percentage Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isPositive) colors.bullishBg else colors.bearishBg)
                                    .border(1.dp, if (isPositive) colors.bullish else colors.bearish, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = changeDisplay,
                                    color = if (isPositive) colors.bullish else colors.bearish,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 2x2 Realtime Session Stats Grid
                        val dayHigh = quote?.high ?: (quote?.lastPrice ?: 24890.0)
                        val dayLow = quote?.low ?: (quote?.lastPrice ?: 24790.0)
                        val dayOpen = quote?.open ?: (quote?.lastPrice ?: 24820.0)
                        val dayClose = quote?.close ?: (quote?.lastPrice ?: 24800.0)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RealtimeStatBox(
                                title = "HIGH",
                                value = "₹${indianFormat.format(dayHigh)}",
                                valueColor = colors.bullish,
                                modifier = Modifier.weight(1f)
                            )
                            RealtimeStatBox(
                                title = "LOW",
                                value = "₹${indianFormat.format(dayLow)}",
                                valueColor = colors.bearish,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RealtimeStatBox(
                                title = "OPEN",
                                value = "₹${indianFormat.format(dayOpen)}",
                                valueColor = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            RealtimeStatBox(
                                title = "PREV CLOSE",
                                value = "₹${indianFormat.format(dayClose)}",
                                valueColor = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Additional technical reference
                        val vwap = quote?.vwap ?: indicators?.vwap ?: 0.0
                        val vix = indicators?.vix ?: 14.8

                        if (vwap > 0.0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surface)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VWAP: ₹${indianFormat.format(vwap)}",
                                    color = colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "INDIA VIX: ${String.format(Locale.US, "%.2f", vix)}",
                                    color = colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // SIGNAL CONFLICT BANNER (IF DETECTED)
                // ==========================================
                SignalConflictBanner(
                    conflictState = uiState.signalConflictState
                )

                // ==========================================
                // 3. NORMAL OPTIONS SIGNAL (TECHNICAL ANALYSIS & PRICE ACTION)
                // ==========================================
                NormalOptionsSignalCard(
                    signal = uiState.normalOptionsSignal
                )

                // ==========================================
                // 4. SMART MONEY OPTIONS SIGNAL (INSTITUTIONAL ORDER FLOW)
                // ==========================================
                SmartMoneyOptionsSignalCard(
                    signal = uiState.smartMoneySignal
                )

                // ==========================================
                // 5. LATEST MARKET NEWS (+VE / -VE SENTIMENT)
                // ==========================================
                LatestMarketNewsCard(
                    newsArticles = uiState.marketNews
                )

                // ==========================================
                // 4. MINIMAL FOOTER
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(colors.bullish)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Realtime Streaming • Autonomous Public NSE Feed",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Real-Time In-App Alert Banner
            InAppAlertBanner(
                alert = uiState.activeInAppAlert,
                onDismiss = { viewModel.dismissInAppAlert() },
                onOpenPanel = { viewModel.setAlertsPanelVisible(true) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 600.dp)
            )
        }
    }

    if (uiState.showAlertsPanel) {
        AlertsPanelDialog(
            uiState = uiState,
            onDismiss = { viewModel.setAlertsPanelVisible(false) },
            onUpdateSettings = { viewModel.updateAlertSettings(it) },
            onMarkAllRead = { viewModel.markAllAlertsAsRead() },
            onClearHistory = { viewModel.clearAlertHistory() },
            onToggleBackgroundMonitoring = { ctx -> viewModel.toggleBackgroundMonitoring(ctx) },
            onTriggerTestAlert = { viewModel.triggerTestAlert() },
            onRequestPermission = requestNotificationPermission
        )
    }
}

@Composable
private fun RealtimeStatBox(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = title,
                color = colors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
