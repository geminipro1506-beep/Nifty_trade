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
import com.cloudscope.moneyprinter.data.model.MarketDirection
import com.cloudscope.moneyprinter.data.model.MarketQuote
import com.cloudscope.moneyprinter.data.model.TradingSignal
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MarketSignalTile(
    quote: MarketQuote?,
    signal: TradingSignal?,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val direction = signal?.marketDirection ?: MarketDirection.SIDEWAYS

    val (badgeColor, badgeBgColor, arrowText) = when (direction) {
        MarketDirection.BULLISH -> Triple(colors.bullish, colors.bullishBg, "BULLISH ↑")
        MarketDirection.BEARISH -> Triple(colors.bearish, colors.bearishBg, "BEARISH ↓")
        MarketDirection.SIDEWAYS -> Triple(colors.sideways, colors.sidewaysBg, "SIDEWAYS ↔")
    }

    val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    val priceDisplay = quote?.let {
        "₹${indianFormat.format(it.lastPrice)}"
    } ?: "₹--"

    val pChangeDisplay = quote?.let {
        val sign = if (it.change >= 0) "+" else ""
        "$sign${String.format("%.2f", it.pChange)}%"
    } ?: "--%"

    val confidenceText = if (signal != null && signal.confidencePercent > 0) {
        "AI CONFIDENCE: ${signal.confidencePercent}%"
    } else {
        "AI CONFIDENCE: --"
    }

    val secondaryText = signal?.summaryFactors ?: "VWAP Neutral | EMA Neutral | RSI 50"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Row 1: Symbol & Price | Direction Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NIFTY 50",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "  |  ",
                        color = colors.border,
                        fontSize = 13.sp
                    )
                    Text(
                        text = priceDisplay,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = pChangeDisplay,
                        color = if ((quote?.change ?: 0.0) >= 0) colors.bullish else colors.bearish,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Compact Direction Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBgColor)
                        .border(0.5.dp, badgeColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = arrowText,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: AI Confidence line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = confidenceText,
                    color = badgeColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 3: Small secondary technical factors
            Text(
                text = secondaryText,
                color = colors.textMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
