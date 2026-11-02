package com.cloudscope.moneyprinter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.bullish.copy(alpha = 0.15f))
                            .border(0.5.dp, colors.bullish, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "WALK-FORWARD OOS",
                            color = colors.bullish,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = result?.validationNotes
                        ?: "Deterministic out-of-sample backtest with actual slippage (0.5 pt), STT 0.1%, Brokerage ₹40 roundtrip, and GST 18%.",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Key Performance Metrics Grid
                if (result != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricBox("WIN RATE", "${String.format("%.1f", result.winRate)}%", colors.bullish)
                        MetricBox("PROFIT FACTOR", String.format("%.2f", result.profitFactor), colors.textPrimary)
                        MetricBox("MAX DRAWDOWN", "${String.format("%.1f", result.maxDrawdownPercent)}%", colors.bearish)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val netSign = if (result.netProfit >= 0) "+" else ""
                        MetricBox("NET P&L", "$netSign₹${indianFormat.format(result.netProfit)}", if (result.netProfit >= 0) colors.bullish else colors.bearish)
                        MetricBox("TOTAL TRADES", "${result.totalTrades} (${result.winningTrades}W / ${result.losingTrades}L)", colors.textPrimary)
                        MetricBox("AVG R:R", "1:${String.format("%.1f", result.averageRiskReward)}", colors.textPrimary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricBox("EXPECTANCY", "₹${String.format("%.1f", result.expectancy)}/trade", colors.textPrimary)
                        MetricBox("TAXES & COSTS", "₹${indianFormat.format(result.totalChargesAndTaxes)}", colors.textMuted)
                        MetricBox("RETURN ON CAP", "${String.format("%.1f", result.returnOnCapitalPercent)}%", if (result.returnOnCapitalPercent >= 0) colors.bullish else colors.bearish)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = colors.border, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Recent Executed Trades
                    Text(
                        text = "AUDITED TRADE LOG",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (result.trades.isEmpty()) {
                        Text(
                            text = "No trades generated in the selected backtest sample.",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    } else {
                        for (trade in result.trades.take(6)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "${trade.symbol} (${trade.exitReason.name})",
                                        color = colors.textPrimary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Entry: ₹${String.format("%.1f", trade.entryPrice)} → Exit: ₹${String.format("%.1f", trade.exitPrice)}",
                                        color = colors.textMuted,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Text(
                                    text = "${if (trade.netProfit >= 0) "+" else ""}₹${String.format("%.1f", trade.netProfit)}",
                                    color = if (trade.netProfit >= 0) colors.bullish else colors.bearish,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onRunBacktest,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Re-Run Simulation", color = colors.textPrimary, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.bullish)
                    ) {
                        Text("Close", color = colors.background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.width(90.dp)) {
        Text(text = label, color = colors.textMuted, fontSize = 8.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
