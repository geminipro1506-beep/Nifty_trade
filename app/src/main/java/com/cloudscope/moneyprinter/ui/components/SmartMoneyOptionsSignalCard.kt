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
import com.cloudscope.moneyprinter.domain.smartmoney.*
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
                        text = "POTENTIAL INSTITUTIONAL FLOW",
                        color = colors.textMuted,
                        fontSize = 10.sp,
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
                // Signal Badge (SMART MONEY BUY CE / SMART MONEY BUY PE / NO ACTIVITY)
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                // Confidence / Strength Badge
                val (confColor, confBg, confLabel) = when {
                    action == SmartMoneySignalAction.NO_TRADE -> Triple(colors.textMuted, colors.surface, "Strength: Low")
                    confidence == SmartMoneyConfidence.STRONG -> Triple(colors.bullish, colors.bullishBg, "Strength: Strong")
                    confidence == SmartMoneyConfidence.MODERATE -> Triple(colors.sideways, colors.sidewaysBg, "Strength: Moderate")
                    else -> Triple(colors.textMuted, colors.surface, "Strength: Low")
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Trade Parameters Section
            if (action != SmartMoneySignalAction.NO_TRADE && signal != null) {
                // Row 1: Suggested Strike & Expiry Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TradeParamBox(
                        title = "SUGGESTED STRIKE",
                        value = signal.suggestedStrike,
                        valueColor = actionColor,
                        modifier = Modifier.weight(1.1f)
                    )
                    TradeParamBox(
                        title = "EXPIRY",
                        value = signal.expiryDate,
                        valueColor = colors.textPrimary,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Entry Premium & Stop Loss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TradeParamBox(
                        title = "ENTRY PREMIUM",
                        value = signal.entryZone,
                        valueColor = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TradeParamBox(
                        title = "STOP LOSS",
                        value = signal.stopLoss,
                        valueColor = colors.bearish,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 3: Target 1, Target 2 & Detection Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    TradeParamBox(
                        title = "DETECTION TIME",
                        value = signal.detectionTime,
                        valueColor = colors.textMuted,
                        modifier = Modifier.weight(1.1f)
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
                // Standby / No Activity Grid
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
                        title = "EXPIRY",
                        value = signal?.expiryDate ?: "—",
                        valueColor = colors.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                    TradeParamBox(
                        title = "DETECTION TIME",
                        value = signal?.detectionTime ?: "—",
                        valueColor = colors.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Institutional Structure Tags (CHoCH, BOS, Order Block, Liquidity Sweep)
            signal?.smartMoneyActivity?.let { act ->
                val chips = mutableListOf<String>()
                if (act.chochState != ChochState.NONE) chips.add(act.chochState.displayName)
                if (act.bosState != BosState.NONE) chips.add(act.bosState.displayName)
                if (act.orderBlock != OrderBlockState.NONE) chips.add(act.orderBlock.displayName)
                if (act.liquiditySweep != LiquiditySweepState.NONE) chips.add(act.liquiditySweep.displayName)
                if (act.isUnusualVolume) chips.add("Unusual Volume (${act.volumeRatio}x)")

                if (chips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chips.take(3).forEach { chip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.surface)
                                    .border(1.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = chip,
                                    color = colors.textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Short Reason / Explanation
            val reasonText = signal?.shortReason ?: "Potential Smart Money Activity: Awaiting live market data & institutional confirmation."
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
                            text = "POTENTIAL SMART MONEY EXPLANATION",
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = reasonText,
                            color = colors.textPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 17.5.sp,
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
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
