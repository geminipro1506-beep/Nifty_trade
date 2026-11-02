package com.cloudscope.moneyprinter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.data.model.OrbState
import com.cloudscope.moneyprinter.data.model.TechnicalIndicators
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors

@Composable
fun IndicatorsGrid(
    indicators: TechnicalIndicators?,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "INTRADAY INDICATORS",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 1: VWAP | 20 EMA | 50 EMA | RSI 14
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IndicatorCell(
                    label = "VWAP",
                    value = indicators?.let { "₹${String.format("%.1f", it.vwap)}" } ?: "--",
                    subtext = indicators?.let { if (it.vwap > 0) "Volume Wtd" else "" } ?: ""
                )
                IndicatorCell(
                    label = "20 EMA",
                    value = indicators?.let { "₹${String.format("%.1f", it.ema20)}" } ?: "--",
                    subtext = "Short Trend"
                )
                IndicatorCell(
                    label = "50 EMA",
                    value = indicators?.let { "₹${String.format("%.1f", it.ema50)}" } ?: "--",
                    subtext = "Med Trend"
                )
                IndicatorCell(
                    label = "RSI (14)",
                    value = indicators?.let { String.format("%.1f", it.rsi14) } ?: "--",
                    valueColor = indicators?.let {
                        if (it.rsi14 >= 55) colors.bullish else if (it.rsi14 <= 45) colors.bearish else colors.textPrimary
                    } ?: colors.textPrimary,
                    subtext = indicators?.let {
                        if (it.rsi14 >= 60) "Bullish" else if (it.rsi14 <= 40) "Bearish" else "Neutral"
                    } ?: ""
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: PCR | India VIX | MACD Hist | ORB 15m
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IndicatorCell(
                    label = "PCR",
                    value = indicators?.let { String.format("%.2f", it.pcr) } ?: "--",
                    valueColor = indicators?.let {
                        if (it.pcr >= 1.15) colors.bullish else if (it.pcr <= 0.85) colors.bearish else colors.textPrimary
                    } ?: colors.textPrimary,
                    subtext = indicators?.let {
                        if (it.pcr >= 1.15) "Put Support" else if (it.pcr <= 0.85) "Call Res." else "Range"
                    } ?: ""
                )
                IndicatorCell(
                    label = "INDIA VIX",
                    value = indicators?.let { String.format("%.2f", it.vix) } ?: "--",
                    subtext = indicators?.let {
                        if (it.vix > 20) "High Vol" else if (it.vix < 13) "Low Vol" else "Normal"
                    } ?: ""
                )
                IndicatorCell(
                    label = "MACD HIST",
                    value = indicators?.let {
                        val sign = if (it.macdHistogram >= 0) "+" else ""
                        "$sign${String.format("%.1f", it.macdHistogram)}"
                    } ?: "--",
                    valueColor = indicators?.let {
                        if (it.macdHistogram > 0) colors.bullish else colors.bearish
                    } ?: colors.textPrimary,
                    subtext = indicators?.let { if (it.macdHistogram > 0) "Positive" else "Negative" } ?: ""
                )
                IndicatorCell(
                    label = "ORB 15M",
                    value = indicators?.let {
                        when (it.orbState) {
                            OrbState.BREAKOUT_UP -> "BREAK UP"
                            OrbState.BREAKOUT_DOWN -> "BREAK DN"
                            OrbState.INSIDE_RANGE -> "INSIDE"
                        }
                    } ?: "--",
                    valueColor = indicators?.let {
                        when (it.orbState) {
                            OrbState.BREAKOUT_UP -> colors.bullish
                            OrbState.BREAKOUT_DOWN -> colors.bearish
                            OrbState.INSIDE_RANGE -> colors.textSecondary
                        }
                    } ?: colors.textPrimary,
                    subtext = indicators?.let { "High: ${it.orbHigh.toInt()}" } ?: ""
                )
            }
        }
    }
}

@Composable
private fun IndicatorCell(
    label: String,
    value: String,
    subtext: String = "",
    valueColor: androidx.compose.ui.graphics.Color = LocalTerminalColors.current.textPrimary
) {
    val colors = LocalTerminalColors.current
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.width(76.dp)
    ) {
        Text(
            text = label,
            color = colors.textMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        if (subtext.isNotBlank()) {
            Text(
                text = subtext,
                color = colors.textSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}
