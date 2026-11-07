package com.cloudscope.moneyprinter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.data.model.OptionType
import com.cloudscope.moneyprinter.domain.normaloptions.NormalOptionsSignal
import com.cloudscope.moneyprinter.domain.normaloptions.NormalSignalAction
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NormalOptionsSignalCard(
    signal: NormalOptionsSignal?,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current

    val action = signal?.action ?: NormalSignalAction.WAITING_FOR_DATA
    val confidence = signal?.confidencePercent ?: 0

    val (actionColor, actionBgColor, actionBorder) = when (action) {
        NormalSignalAction.BUY_CALL -> Triple(colors.bullish, colors.bullishBg, colors.bullish)
        NormalSignalAction.BUY_PUT -> Triple(colors.bearish, colors.bearishBg, colors.bearish)
        NormalSignalAction.NO_TRADE -> Triple(colors.textSecondary, colors.cardElevated, colors.border)
        NormalSignalAction.WAITING_FOR_CONFIRMATION -> Triple(colors.sideways, colors.sidewaysBg, colors.sideways)
        NormalSignalAction.WAITING_FOR_DATA -> Triple(colors.textMuted, colors.surface, colors.border)
    }

    val indianFormat = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    val timeFormat = remember {
        SimpleDateFormat("HH:mm:ss 'IST'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
    }
    val updateTime = timeFormat.format(Date(signal?.lastUpdateTime ?: System.currentTimeMillis()))

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
            // Header: Section Title & Subtitle Badge
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
                        text = "NORMAL OPTIONS SIGNAL",
                        color = colors.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surface)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "TECHNICAL & PRICE ACTION",
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
                // Signal Status Badge
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
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                // Confidence / Edge Badge
                val (confColor, confBg, confLabel) = when (action) {
                    NormalSignalAction.BUY_CALL, NormalSignalAction.BUY_PUT ->
                        Triple(actionColor, actionBgColor, "$confidence% Confidence")
                    NormalSignalAction.WAITING_FOR_CONFIRMATION ->
                        Triple(colors.sideways, colors.sidewaysBg, "$confidence% Confluence")
                    NormalSignalAction.NO_TRADE ->
                        Triple(colors.textMuted, colors.surface, "No Edge")
                    NormalSignalAction.WAITING_FOR_DATA ->
                        Triple(colors.textMuted, colors.surface, "Standby")
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

            // Confidence Progress Bar if Active or Developing
            if (confidence > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (confidence / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = actionColor,
                    trackColor = colors.surface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Trade Parameters Section (When BUY CALL or BUY PUT is active)
            if (action == NormalSignalAction.BUY_CALL || action == NormalSignalAction.BUY_PUT) {
                // Row 1: Suggested Strike, Option Type, Expiry Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NormalParamBox(
                        title = "SUGGESTED STRIKE",
                        value = "${signal?.suggestedStrike?.toInt() ?: "—"}",
                        valueColor = actionColor,
                        modifier = Modifier.weight(1f)
                    )
                    NormalParamBox(
                        title = "OPTION TYPE",
                        value = if (signal?.optionType == OptionType.CE) "CALL (CE)" else "PUT (PE)",
                        valueColor = actionColor,
                        modifier = Modifier.weight(1f)
                    )
                    NormalParamBox(
                        title = "EXPIRY DATE",
                        value = signal?.expiryDate ?: "—",
                        valueColor = colors.textPrimary,
                        modifier = Modifier.weight(1.1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Entry Price (Live Premium), Stop Loss, Target 1, Target 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NormalParamBox(
                        title = "ENTRY (PREMIUM)",
                        value = "₹${indianFormat.format(signal?.entryPrice ?: 0.0)}",
                        valueColor = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    NormalParamBox(
                        title = "STOP LOSS",
                        value = "₹${indianFormat.format(signal?.stopLoss ?: 0.0)}",
                        valueColor = colors.bearish,
                        modifier = Modifier.weight(1f)
                    )
                    NormalParamBox(
                        title = "TARGET 1",
                        value = "₹${indianFormat.format(signal?.target1 ?: 0.0)}",
                        valueColor = colors.bullish,
                        modifier = Modifier.weight(1f)
                    )
                    NormalParamBox(
                        title = "TARGET 2",
                        value = "₹${indianFormat.format(signal?.target2 ?: 0.0)}",
                        valueColor = colors.bullish,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 3: Risk-Reward & Last Update Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Risk-to-Reward: ${signal?.riskRewardRatio ?: "1 : 2.0"}",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Updated: $updateTime",
                        color = colors.textMuted,
                        fontSize = 11.5.sp
                    )
                }
            } else {
                // Standby / No Trade / Waiting Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NormalParamBox(
                        title = "SUGGESTED STRIKE",
                        value = if ((signal?.suggestedStrike ?: 0.0) > 0.0) "${signal?.suggestedStrike?.toInt()} ${signal?.optionType?.name ?: ""}" else "—",
                        valueColor = colors.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                    NormalParamBox(
                        title = "EXPIRY",
                        value = signal?.expiryDate ?: "—",
                        valueColor = colors.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                    NormalParamBox(
                        title = "ENTRY / SL / TARGET",
                        value = "—",
                        valueColor = colors.textMuted,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Updated: $updateTime",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Signal Reason Box
            val reasonText = signal?.signalReason ?: "Awaiting live market data & technical confirmation."
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
                        imageVector = when (action) {
                            NormalSignalAction.BUY_CALL, NormalSignalAction.BUY_PUT -> Icons.Default.Timeline
                            NormalSignalAction.WAITING_FOR_CONFIRMATION -> Icons.Default.Warning
                            NormalSignalAction.NO_TRADE -> Icons.Default.Shield
                            NormalSignalAction.WAITING_FOR_DATA -> Icons.Default.Info
                        },
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
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NormalParamBox(
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
        }
    }
}
