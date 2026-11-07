package com.cloudscope.moneyprinter.ui.components.alerts

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cloudscope.moneyprinter.data.model.MarketDirection
import com.cloudscope.moneyprinter.domain.alert.AlertCategory
import com.cloudscope.moneyprinter.domain.alert.AlertSettings
import com.cloudscope.moneyprinter.domain.alert.MarketAlert
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import com.cloudscope.moneyprinter.ui.viewmodel.MainUiState
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class AlertPanelTab(val title: String) {
    HISTORY("Alert History"),
    SETTINGS("Smart Settings")
}

@Composable
fun AlertsPanelDialog(
    uiState: MainUiState,
    onDismiss: () -> Unit,
    onUpdateSettings: (AlertSettings) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearHistory: () -> Unit,
    onToggleBackgroundMonitoring: (Context) -> Unit,
    onTriggerTestAlert: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AlertPanelTab.HISTORY) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.card,
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.bullish.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = colors.bullish,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Market Alerts & Notifications",
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Autonomous NIFTY 50 State Monitor",
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary
                        )
                    }
                }

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = colors.surface,
                    contentColor = colors.bullish
                ) {
                    Tab(
                        selected = selectedTab == AlertPanelTab.HISTORY,
                        onClick = { selectedTab = AlertPanelTab.HISTORY },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "History",
                                    fontWeight = if (selectedTab == AlertPanelTab.HISTORY) FontWeight.Bold else FontWeight.Normal
                                )
                                if (uiState.unreadAlertCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = colors.bearish) {
                                        Text(text = "${uiState.unreadAlertCount}")
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == AlertPanelTab.SETTINGS,
                        onClick = { selectedTab = AlertPanelTab.SETTINGS },
                        text = {
                            Text(
                                text = "Settings",
                                fontWeight = if (selectedTab == AlertPanelTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                // Content View
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(14.dp)
                ) {
                    when (selectedTab) {
                        AlertPanelTab.HISTORY -> AlertHistoryView(
                            alerts = uiState.alerts,
                            onMarkAllRead = onMarkAllRead,
                            onClearHistory = onClearHistory,
                            onTriggerTestAlert = onTriggerTestAlert
                        )
                        AlertPanelTab.SETTINGS -> AlertSettingsView(
                            settings = uiState.alertSettings,
                            isBackgroundMonitoringActive = uiState.isBackgroundMonitoringActive,
                            onUpdateSettings = onUpdateSettings,
                            onToggleBackgroundMonitoring = { onToggleBackgroundMonitoring(context) },
                            onRequestPermission = onRequestPermission
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertHistoryView(
    alerts: List<MarketAlert>,
    onMarkAllRead: () -> Unit,
    onClearHistory: () -> Unit,
    onTriggerTestAlert: () -> Unit
) {
    val colors = LocalTerminalColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total: ${alerts.size} alerts",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onTriggerTestAlert,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colors.bullish
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Alert", fontSize = 11.5.sp, color = colors.bullish)
                }

                if (alerts.any { !it.isRead }) {
                    OutlinedButton(
                        onClick = onMarkAllRead,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = colors.accentBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Read All", fontSize = 11.5.sp, color = colors.accentBlue)
                    }
                }

                if (alerts.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearHistory,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = colors.textMuted
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", fontSize = 11.5.sp, color = colors.textMuted)
                    }
                }
            }
        }

        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Alerts Triggered Yet",
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The system will notify you automatically when NIFTY 50 conditions or dynamic confidence shift.",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alerts, key = { it.id }) { alert ->
                    AlertItemCard(alert = alert)
                }
            }
        }
    }
}

@Composable
private fun AlertItemCard(alert: MarketAlert) {
    val colors = LocalTerminalColors.current

    val accentColor = when (alert.type.category) {
        AlertCategory.DIRECTION -> when {
            alert.title.contains("BULLISH", ignoreCase = true) -> colors.bullish
            alert.title.contains("BEARISH", ignoreCase = true) -> colors.bearish
            else -> colors.sideways
        }
        AlertCategory.CONFIDENCE -> colors.accentBlue
        AlertCategory.TECHNICAL -> colors.bullish
        AlertCategory.VOLATILITY -> colors.sideways
        AlertCategory.OPTIONS_SIGNAL -> when {
            alert.title.contains("BUY CE", ignoreCase = true) || alert.title.contains("BUY CALL", ignoreCase = true) -> colors.bullish
            alert.title.contains("BUY PE", ignoreCase = true) || alert.title.contains("BUY PUT", ignoreCase = true) -> colors.bearish
            else -> colors.sideways
        }
        AlertCategory.MARKET_NEWS -> if (alert.title.contains("BULLISH", ignoreCase = true)) colors.bullish else colors.bearish
        AlertCategory.SYSTEM -> colors.bearish
    }

    val timeFormat = remember {
        SimpleDateFormat("HH:mm:ss 'IST'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
    }
    val timeString = timeFormat.format(Date(alert.timestamp))

    val indianFormat = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (!alert.isRead) 1.5.dp else 1.dp,
                color = if (!alert.isRead) accentColor.copy(alpha = 0.8f) else colors.border,
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Emoji, Title, Time, Unread Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = alert.type.defaultEmoji,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alert.title,
                        color = colors.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeString,
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!alert.isRead) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body Message
            Text(
                text = alert.message,
                color = colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // Transition Chips (Direction, Confidence, Spot Price)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                AlertTagChip(
                    text = alert.type.category.displayName,
                    bgColor = colors.cardElevated,
                    textColor = colors.textSecondary
                )

                // Direction Shift Chip
                if (alert.previousDirection != null && alert.newDirection != null) {
                    val dirColor = when (alert.newDirection) {
                        MarketDirection.BULLISH -> colors.bullish
                        MarketDirection.BEARISH -> colors.bearish
                        MarketDirection.SIDEWAYS -> colors.sideways
                    }
                    AlertTagChip(
                        text = "${alert.previousDirection.name} ➔ ${alert.newDirection.name}",
                        bgColor = dirColor.copy(alpha = 0.15f),
                        textColor = dirColor
                    )
                }

                // Confidence Shift Chip
                if (alert.previousConfidence != null && alert.newConfidence != null) {
                    val delta = alert.newConfidence - alert.previousConfidence
                    val sign = if (delta > 0) "+" else ""
                    val confColor = if (delta >= 0) colors.bullish else colors.bearish
                    AlertTagChip(
                        text = "${alert.previousConfidence}% ➔ ${alert.newConfidence}% ($sign$delta%)",
                        bgColor = confColor.copy(alpha = 0.15f),
                        textColor = confColor
                    )
                }

                // Spot Price Chip
                AlertTagChip(
                    text = "₹${indianFormat.format(alert.spotPrice)}",
                    bgColor = colors.cardElevated,
                    textColor = colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun AlertTagChip(
    text: String,
    bgColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun AlertSettingsView(
    settings: AlertSettings,
    isBackgroundMonitoringActive: Boolean,
    onUpdateSettings: (AlertSettings) -> Unit,
    onToggleBackgroundMonitoring: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val colors = LocalTerminalColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Master Switch
        item {
            SettingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Real-Time Notifications Master Switch",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enable or suppress all market alerts and notifications",
                            color = colors.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                onRequestPermission()
                            }
                            onUpdateSettings(settings.copy(notificationsEnabled = enabled))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.surface,
                            checkedTrackColor = colors.bullish
                        )
                    )
                }
            }
        }

        // Alert Types Sub-toggles
        item {
            SettingCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Alert Categories & Triggers",
                        color = colors.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    AlertToggleRow(
                        title = "Bullish Turn Alerts (🟢)",
                        subtitle = "Trigger when market flips to Bullish momentum",
                        checked = settings.bullishAlertsEnabled,
                        enabled = settings.notificationsEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(bullishAlertsEnabled = it)) }
                    )

                    AlertToggleRow(
                        title = "Bearish Turn Alerts (🔴)",
                        subtitle = "Trigger when market flips to Bearish breakdown",
                        checked = settings.bearishAlertsEnabled,
                        enabled = settings.notificationsEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(bearishAlertsEnabled = it)) }
                    )

                    AlertToggleRow(
                        title = "Sideways Regime Alerts (🟡)",
                        subtitle = "Trigger when market enters rangebound consolidation",
                        checked = settings.sidewaysAlertsEnabled,
                        enabled = settings.notificationsEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(sidewaysAlertsEnabled = it)) }
                    )

                    AlertToggleRow(
                        title = "AI Confidence Shifts (📈 / 📉)",
                        subtitle = "Trigger when dynamic AI conviction changes by ≥ threshold",
                        checked = settings.confidenceAlertsEnabled,
                        enabled = settings.notificationsEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(confidenceAlertsEnabled = it)) }
                    )

                    AlertToggleRow(
                        title = "Technical Indicator Alerts (⚡)",
                        subtitle = "VWAP cross, RSI 70/30, MACD/EMA crossover, Volume surge, VIX spike",
                        checked = settings.technicalAlertsEnabled,
                        enabled = settings.notificationsEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(technicalAlertsEnabled = it)) }
                    )
                }
            }
        }

        // Sound & Audio
        item {
            SettingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audio Alert Chime (🔊)",
                            color = colors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Play distinctive audio tone on each triggered alert",
                            color = colors.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(soundEnabled = it)) },
                        enabled = settings.notificationsEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.surface,
                            checkedTrackColor = colors.bullish
                        )
                    )
                }
            }
        }

        // Thresholds & Cooldown
        item {
            SettingCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Sensitivity & Cooldown Thresholds",
                        color = colors.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Min Confidence Change Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Min Confidence Change Threshold",
                                color = colors.textPrimary,
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = "${settings.minConfidenceChangeThreshold}%",
                                color = colors.bullish,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = settings.minConfidenceChangeThreshold.toFloat(),
                            onValueChange = { onUpdateSettings(settings.copy(minConfidenceChangeThreshold = it.toInt())) },
                            valueRange = 5f..30f,
                            steps = 4,
                            enabled = settings.notificationsEnabled && settings.confidenceAlertsEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.bullish,
                                activeTrackColor = colors.bullish
                            )
                        )
                    }

                    // Cooldown Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Alert Repeat Cooldown Period",
                                color = colors.textPrimary,
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = "${settings.cooldownMinutes} min",
                                color = colors.bullish,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = settings.cooldownMinutes.toFloat(),
                            onValueChange = { onUpdateSettings(settings.copy(cooldownMinutes = it.toInt())) },
                            valueRange = 1f..15f,
                            steps = 13,
                            enabled = settings.notificationsEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.bullish,
                                activeTrackColor = colors.bullish
                            )
                        )
                    }
                }
            }
        }

        // Background Monitoring Foreground Service
        item {
            SettingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Background Live Monitoring (⚡)",
                                color = colors.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Independent background service monitors NIFTY 50 and fires alerts even when the app is minimized",
                            color = colors.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    Switch(
                        checked = isBackgroundMonitoringActive,
                        onCheckedChange = {
                            if (!isBackgroundMonitoringActive) {
                                onRequestPermission()
                            }
                            onToggleBackgroundMonitoring()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.surface,
                            checkedTrackColor = colors.bullish
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalTerminalColors.current
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            content = content
        )
    }
}

@Composable
private fun AlertToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalTerminalColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) colors.textPrimary else colors.textMuted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.surface,
                checkedTrackColor = colors.bullish
            )
        )
    }
}
