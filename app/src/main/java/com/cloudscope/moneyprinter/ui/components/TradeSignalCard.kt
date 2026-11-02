package com.cloudscope.moneyprinter.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.data.model.OptionType
import com.cloudscope.moneyprinter.data.model.SignalAction
import com.cloudscope.moneyprinter.data.model.SignalStatus
import com.cloudscope.moneyprinter.data.model.TradingSignal
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors

@Composable
fun TradeSignalCard(
    signal: TradingSignal?,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current

    if (signal == null || signal.status == SignalStatus.INACTIVE || signal.action == SignalAction.NO_TRADE) {
        // Deterministic NO TRADE State
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.card)
                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NO TRADE",
                        color = colors.sideways,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.card)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "STATUS: INACTIVE",
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = signal?.confirmationReasons?.firstOrNull() ?: signal?.summaryFactors ?: "Market conditions are not favourable. Preserving capital.",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
        return
    }

    // Active Trading Signal Card
    val actionText = when (signal.action) {
        SignalAction.BUY_CE -> "ACTION: BUY NIFTY ${signal.recommendedStrike?.toInt() ?: 0} CE"
        SignalAction.BUY_PE -> "ACTION: BUY NIFTY ${signal.recommendedStrike?.toInt() ?: 0} PE"
        SignalAction.NO_TRADE -> "NO TRADE"
    }

    val actionColor = when (signal.action) {
        SignalAction.BUY_CE -> colors.bullish
        SignalAction.BUY_PE -> colors.bearish
        SignalAction.NO_TRADE -> colors.sideways
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.card)
            .border(1.dp, actionColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Action + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    color = actionColor,
                    fontSize = 13.5.sp,
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
            HorizontalDivider(color = colors.border, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Risk / Reward & Spot Meta
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

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = colors.border, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Quick Execution Shortcuts for Groww & Angel One
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXECUTE:",
                    color = colors.textMuted,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                val context = LocalContext.current

                // Groww Quick Execution Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.bullish.copy(alpha = 0.15f))
                        .border(1.dp, colors.bullish, RoundedCornerShape(4.dp))
                        .clickable {
                            launchBrokerApp(context, "com.nextbillion.groww", "https://groww.in/options-trading")
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚡ GROWW",
                        color = colors.bullish,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Angel One Quick Execution Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.card)
                        .border(1.dp, colors.border, RoundedCornerShape(4.dp))
                        .clickable {
                            launchBrokerApp(context, "com.msf.angelmobile", "https://trade.angelone.in/")
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚡ ANGEL ONE",
                        color = colors.textPrimary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun launchBrokerApp(context: Context, packageName: String, webUrl: String) {
    try {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
            context.startActivity(browserIntent)
        }
    } catch (e: Exception) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
        context.startActivity(browserIntent)
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
