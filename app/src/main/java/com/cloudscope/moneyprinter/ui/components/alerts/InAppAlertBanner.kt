package com.cloudscope.moneyprinter.ui.components.alerts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.domain.alert.AlertCategory
import com.cloudscope.moneyprinter.domain.alert.MarketAlert
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InAppAlertBanner(
    alert: MarketAlert?,
    onDismiss: () -> Unit,
    onOpenPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current

    AnimatedVisibility(
        visible = alert != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (alert == null) return@AnimatedVisibility

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

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .border(1.5.dp, accentColor, RoundedCornerShape(12.dp))
                .clickable { onOpenPanel() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji Icon Box
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = alert.type.defaultEmoji,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = alert.title,
                            color = colors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = timeString,
                            color = colors.textSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = alert.message,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Alert",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
