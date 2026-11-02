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
import com.cloudscope.moneyprinter.data.model.OptionChain
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors

@Composable
fun OptionChainSummaryCard(
    optionChain: OptionChain?,
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OPTION CHAIN INTELLIGENCE",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = optionChain?.let { "ATM: ${it.atmStrike.toInt()}" } ?: "ATM: --",
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics: PCR | Max Pain | Net Call OI | Net Put OI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PCR", color = colors.textMuted, fontSize = 9.sp)
                    Text(
                        text = optionChain?.let { String.format("%.2f", it.pcr) } ?: "--",
                        color = optionChain?.let { if (it.pcr >= 1.0) colors.bullish else colors.bearish } ?: colors.textPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text("MAX PAIN", color = colors.textMuted, fontSize = 9.sp)
                    Text(
                        text = optionChain?.let { "₹${it.maxPainStrike.toInt()}" } ?: "--",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text("CALL OI CHG", color = colors.textMuted, fontSize = 9.sp)
                    Text(
                        text = optionChain?.let { formatOiChange(it.totalCallOiChange) } ?: "--",
                        color = colors.bearish, // Call writing is resistance
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text("PUT OI CHG", color = colors.textMuted, fontSize = 9.sp)
                    Text(
                        text = optionChain?.let { formatOiChange(it.totalPutOiChange) } ?: "--",
                        color = colors.bullish, // Put writing is support
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = colors.border, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Table Header: CE LTP | STRIKE | PE LTP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CALL LTP", color = colors.textMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("STRIKE", color = colors.textMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text("PUT LTP", color = colors.textMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Display 3 rows: ATM - 50, ATM, ATM + 50
            val atm = optionChain?.atmStrike ?: 0.0
            val displayStrikes = listOf(atm - 50.0, atm, atm + 50.0)

            for (strike in displayStrikes) {
                val row = optionChain?.rows?.find { it.strikePrice == strike }
                val isAtm = strike == atm
                val rowBg = if (isAtm) colors.border.copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(rowBg)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row?.callContract?.let { "₹${String.format("%.1f", it.lastPrice)}" } ?: "--",
                        color = colors.bullish,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isAtm) FontWeight.Bold else FontWeight.Normal
                    )

                    Text(
                        text = "${strike.toInt()}${if (isAtm) " (ATM)" else ""}",
                        color = if (isAtm) colors.textPrimary else colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = row?.putContract?.let { "₹${String.format("%.1f", it.lastPrice)}" } ?: "--",
                        color = colors.bearish,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isAtm) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun formatOiChange(oi: Long): String {
    val sign = if (oi >= 0) "+" else ""
    val lakhs = oi / 100000.0
    return "$sign${String.format("%.1f", lakhs)}L"
}
