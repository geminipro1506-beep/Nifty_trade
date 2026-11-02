package com.cloudscope.moneyprinter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cloudscope.moneyprinter.data.model.BacktestResult
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BacktestDialog(
    result: BacktestResult?,
    onDismiss: () -> Unit,
    onRunBacktest: () -> Unit
) {
    val colors = LocalTerminalColors.current
    val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(14.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HISTORICAL STRATEGY AUDIT",
                        color = colors.textPrimary,
                        fontSize = 18.sp, // Requirement: 20-24px
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = result?.validationNotes
                        ?: "Deterministic walk-forward backtest accounting for slippage, STT, exchange turnover fees, and brokerage.",
                    color = colors.textSecondary,
                    fontSize = 15.sp, // Minimum 15-16px
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Key Performance Metrics Grid
                if (result != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBox("WIN RATE", "${String.format("%.1f", result.winRate)}%", colors.bullish)
                        MetricBox("PROFIT FACTOR", String.format("%.2f", result.profitFactor), colors.textPrimary)
                        MetricBox("MAX DRAWDOWN", "${String.format("%.1f", result.maxDrawdownPercent)}%", colors.bearish)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBox("TOTAL TRADES", "${result.totalTrades}", colors.textPrimary)
                        MetricBox("WINNING TRADES", "${result.winningTrades}", colors.bullish)
                        MetricBox("LOSING TRADES", "${result.losingTrades}", colors.bearish)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Financial Summary Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.cardElevated)
                            .padding(14.dp)
                    ) {
                        Column {
                            FinanceRow(label = "Net Simulated Profit:", value = "₹${indianFormat.format(result.netProfit)}", color = if (result.netProfit >= 0) colors.bullish else colors.bearish)
                            FinanceRow(label = "Total Brokerage & Taxes:", value = "₹${indianFormat.format(result.totalChargesAndTaxes)}", color = colors.textSecondary)
                            FinanceRow(label = "Average Trade P&L:", value = "₹${indianFormat.format(result.expectancy)}", color = if (result.expectancy >= 0) colors.bullish else colors.bearish)
                            FinanceRow(label = "Return on Capital:", value = "${String.format("%.1f", result.returnOnCapitalPercent)}%", color = colors.bullish)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Button(
                    onClick = onRunBacktest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp), // Touch target >= 48dp
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.bullish)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run", tint = colors.background)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RE-EXECUTE SIMULATION", color = colors.background, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("CLOSE", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, valueColor: Color) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.cardElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = colors.textMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FinanceRow(label: String, value: String, color: Color) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = colors.textSecondary, fontSize = 14.5.sp)
        Text(text = value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
