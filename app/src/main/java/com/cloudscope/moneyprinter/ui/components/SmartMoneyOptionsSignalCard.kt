package com.cloudscope.moneyprinter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneyConfidence
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignal
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalAction
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors

@Composable
fun SmartMoneyOptionsSignalCard(
    signal: SmartMoneySignal?,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current

    val action = signal?.action ?: SmartMoneySignalAction.NO_TRADE
    val confidence = signal?.confidence ?: SmartMoneyConfidence.LOW

    val (actionColor, actionBgColor, actionBorder) = when (action) {
        SmartMoneySignalAction.BUY_CE -> Triple(colors.bullish, colors.bullishBg, colors.bullish)
        SmartMoneySignalAction.BUY_PE -> Triple(colors.bearish, colors.bearishBg, colors.bearish)
        SmartMoneySignalAction.NO_TRADE -> Triple(colors.textSecondary, colors.cardElevated, colors.border)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, actionBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Section Title
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
                            .background(actionColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SMART MONEY OPTIONS SIGNAL",
                        color = colors.textMuted,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surface)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "NIFTY OPTIONS RADAR",
                        color = colors.textMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Signal Banner & Confidence Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Signal Badge (BUY CE / BUY PE / NO TRADE)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(actionBgColor)
                        .border(1.dp, actionColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = action.displayName,
                        color = actionColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                // Confidence Badge
                val (confColor, confBg, confLabel) = when {
                    action == SmartMoneySignalAction.NO_TRADE -> Triple(colors.textMuted, colors.surface, "Confidence: —")
                    confidence == SmartMoneyConfidence.STRONG -> Triple(colors.bullish, colors.bullishBg, "Confidence: Strong")
                    confidence == SmartMoneyConfidence.MODERATE -> Triple(colors.sideways, colors.sidewaysBg, "Confidence: Moderate")
                    else -> Triple(colors.textMuted, colors.surface, "Confidence: Low")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(confBg)
                        .border(1.dp, confColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = confLabel,
                        color = confColor,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trade Parameters Section
            if (action != SmartMoneySignalAction.NO_TRADE && signal != null) {
                // Row 1: Suggested Strike & Entry Zone
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TradeParamBox(
                        title = "SUGGESTED STRIKE",
                        value = signal.suggestedStrike,
                        valueColor = actionColor,
                        modifier = Modifier.weight(1f)
                    )
                    TradeParamBox(
                        title = "ENTRY ZONE",
                        value = signal.entryZone,
                        valueColor = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Stop Loss & Targets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TradeParamBox(
                        title = "STOP LOSS",
                        value = signal.stopLoss,
                        valueColor = colors.bearish,
                        modifier = Modifier.weight(1f)
                    )
                    TradeParamBox(
                        title = "TARGET 1",
                        value = signal.target1,
                        valueColor = colors.bullish,
                        modifier = Modifier.weight(1f)
                    )
                    TradeParamBox(
                        title = "TARGET 2",
                        value = signal.target2,
                        valueColor = colors.bullish,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (signal.netRiskRewardRatio > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net R:R: 1 : ${signal.netRiskRewardRatio} (Est. charges: ₹${signal.estimatedCharges.toInt()}/lot)",
                            color = colors.textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Standby / No Trade Placeholder Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TradeParamBox(
                        title = "SUGGESTED STRIKE",
                        value = "—",
                        valueColor = colors.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                    TradeParamBox(
                        title = "ENTRY ZONE",
                        value = "—",
                        valueColor = colors.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                    TradeParamBox(
                        title = "STOP LOSS",
                        value = "—",
                        valueColor = colors.textMuted,
                        modifier = Modifier.weight(0.9f)
                    )
                    TradeParamBox(
                        title = "TARGET 1 & 2",
                        value = "—",
                        valueColor = colors.textMuted,
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Short Reason for Signal
            val reasonText = signal?.shortReason ?: "Awaiting live market data & institutional confirmation."
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = if (action == SmartMoneySignalAction.NO_TRADE) Icons.Default.Shield else Icons.Default.Info,
                        contentDescription = "Signal Rationale",
                        tint = actionColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SIGNAL REASON",
                            color = colors.textMuted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = reasonText,
                            color = colors.textPrimary,
                            fontSize = 13.5.sp,
                            lineHeight = 18.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeParamBox(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = title,
                color = colors.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
