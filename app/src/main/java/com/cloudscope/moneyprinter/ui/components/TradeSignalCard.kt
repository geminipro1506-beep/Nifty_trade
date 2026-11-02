package com.cloudscope.moneyprinter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.data.model.SignalAction
import com.cloudscope.moneyprinter.data.model.TradingSignal
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors

@Composable
fun TradeSignalCard(
    signal: TradingSignal?,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current

    if (signal == null || signal.action == SignalAction.NO_TRADE) {
        // No Trade state
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.card)
                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NO TRADE",
                        color = colors.textSecondary,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.border)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "STAND ASIDE",
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val reason = signal?.confirmationReasons?.firstOrNull() ?: "Market conditions are not favourable."
                Text(
                    text = reason,
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
        return
    }

    // Active Trading Setup Card
    val actionText = when (signal.action) {
        SignalAction.BUY_CE -> "ACTION: BUY ${signal.contractSymbol}"
        SignalAction.BUY_PE -> "ACTION: BUY ${signal.contractSymbol}"
        SignalAction.NO_TRADE -> "NO TRADE"
    }

    val actionColor = when (signal.action) {
        SignalAction.BUY_CE -> colors.bullish
        SignalAction.BUY_PE -> colors.bearish
        SignalAction.NO_TRADE -> colors.textMuted
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.card)
            .border(1.dp, actionColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Row: Action & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    color = actionColor,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(actionColor.copy(alpha = 0.15f))
                        .border(0.5.dp, actionColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "STATUS: ${signal.status.name}",
                        color = actionColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing Grid: Entry | Stop Loss | Target 1 | Target 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LevelItem(label = "Entry", value = "₹${String.format("%.2f", signal.entryPrice)}", color = colors.textPrimary)
                LevelItem(label = "Stop Loss", value = "₹${String.format("%.2f", signal.stopLoss)}", color = colors.bearish)
                LevelItem(label = "Target 1", value = "₹${String.format("%.2f", signal.target1)}", color = colors.bullish)
                LevelItem(label = "Target 2", value = "₹${String.format("%.2f", signal.target2)}", color = colors.bullish)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = colors.border, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Risk / Reward & Strike Meta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Risk/Reward: 1:${String.format("%.1f", signal.riskRewardRatio)}",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Spot: ₹${String.format("%.2f", signal.underlyingSpot)}",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun LevelItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    val colors = LocalTerminalColors.current
    Column {
        Text(
            text = label,
            color = colors.textMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = color,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
