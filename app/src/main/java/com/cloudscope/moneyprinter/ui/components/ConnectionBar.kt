package com.cloudscope.moneyprinter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.data.model.BrokerType
import com.cloudscope.moneyprinter.data.model.ConnectionState
import com.cloudscope.moneyprinter.data.model.ConnectionStatus
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConnectionBar(
    state: ConnectionState,
    onRefreshClick: () -> Unit,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current

    val (statusLabel, statusColor, statusEmoji) = when (state.status) {
        ConnectionStatus.LIVE -> Triple("LIVE", colors.bullish, "🟢")
        ConnectionStatus.DELAYED -> Triple("DELAYED", colors.sideways, "🟠")
        ConnectionStatus.DATA_UNAVAILABLE -> Triple("DISCONNECTED", colors.bearish, "🔴")
    }

    val brokerShortName = when (state.brokerType) {
        BrokerType.NONE_STANDALONE -> "NSE Direct"
        BrokerType.GROWW -> "Groww Ready"
        BrokerType.ANGEL_ONE -> "Angel One"
        BrokerType.ZERODHA -> "Zerodha Kite"
    }

    val timeFormatter = SimpleDateFormat("hh:mm:ss a 'IST'", Locale.ENGLISH).apply {
        timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
    }
    val timeText = if (state.lastUpdated > 0) timeFormatter.format(Date(state.lastUpdated)) else "--:--:--"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Dot + Status Label + Broker Tag + Last Updated
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onConfigureClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "$statusEmoji $statusLabel",
                color = statusColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Text(
                text = " • $brokerShortName • Tick: $timeText",
                color = colors.textMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Action icons: Refresh and Broker Settings
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onConfigureClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configure Broker",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
