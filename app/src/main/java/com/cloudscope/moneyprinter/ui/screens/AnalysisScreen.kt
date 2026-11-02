package com.cloudscope.moneyprinter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
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
import com.cloudscope.moneyprinter.data.model.OrbState
import com.cloudscope.moneyprinter.data.model.SupportResistanceLevels
import com.cloudscope.moneyprinter.data.model.TechnicalIndicators
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import com.cloudscope.moneyprinter.ui.viewmodel.MainUiState
import com.cloudscope.moneyprinter.ui.viewmodel.MainViewModel

@Composable
fun AnalysisScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val indicators = uiState.indicators
    val srLevels = uiState.supportResistance
    val spot = uiState.quote?.lastPrice ?: 24850.0
    val backtest = uiState.backtestResult

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. TECHNICAL INDICATORS GRID (Large & Readable)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "TECHNICAL INDICATORS CONFLUENCE",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // VWAP & RSI Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IndicatorBox(
                        title = "VWAP",
                        value = indicators?.let { "₹${String.format("%.1f", it.vwap)}" } ?: "--",
                        subtitle = if (spot > (indicators?.vwap ?: 0.0)) "Bullish (+₹${String.format("%.1f", spot - (indicators?.vwap ?: 0.0))})" else "Bearish (-₹${String.format("%.1f", (indicators?.vwap ?: 0.0) - spot)})",
                        subtitleColor = if (spot > (indicators?.vwap ?: 0.0)) colors.bullish else colors.bearish,
                        modifier = Modifier.weight(1f)
                    )

                    IndicatorBox(
                        title = "RSI (14)",
                        value = indicators?.let { String.format("%.1f", it.rsi14) } ?: "--",
                        subtitle = when {
                            (indicators?.rsi14 ?: 50.0) > 65.0 -> "Strong Momentum"
                            (indicators?.rsi14 ?: 50.0) < 35.0 -> "Oversold Zone"
                            else -> "Healthy Expansion"
                        },
                        subtitleColor = if ((indicators?.rsi14 ?: 50.0) in 52.0..70.0) colors.bullish else colors.sideways,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // EMA Ribbon: 9, 20, 50, 200
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.cardElevated)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "EXPONENTIAL MOVING AVERAGE (EMA) RIBBON",
                            color = colors.textMuted,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            EmaPill(label = "9 EMA", value = indicators?.let { "₹${it.ema9.toInt()}" } ?: "--", color = colors.bullish)
                            EmaPill(label = "20 EMA", value = indicators?.let { "₹${it.ema20.toInt()}" } ?: "--", color = colors.accentBlue)
                            EmaPill(label = "50 EMA", value = indicators?.let { "₹${it.ema50.toInt()}" } ?: "--", color = colors.sideways)
                            EmaPill(label = "200 EMA", value = indicators?.let { "₹${it.ema200.toInt()}" } ?: "--", color = colors.textSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // MACD & ATR Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IndicatorBox(
                        title = "MACD (12, 26, 9)",
                        value = indicators?.let { String.format("%.2f", it.macdHistogram) } ?: "--",
                        subtitle = if ((indicators?.macdHistogram ?: 0.0) > 0) "Histogram Expansion (+)" else "Histogram Contraction (-)",
                        subtitleColor = if ((indicators?.macdHistogram ?: 0.0) > 0) colors.bullish else colors.bearish,
                        modifier = Modifier.weight(1f)
                    )

                    IndicatorBox(
                        title = "ATR (14 Volatility)",
                        value = indicators?.let { "₹${String.format("%.1f", it.atr14)}" } ?: "--",
                        subtitle = "Intraday Range: ±₹${indicators?.let { String.format("%.0f", it.atr14 * 2.0) } ?: "--"}",
                        subtitleColor = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 15-Minute ORB Status
                val orbState = indicators?.orbState ?: OrbState.INSIDE_RANGE
                val orbText = when (orbState) {
                    OrbState.BREAKOUT_UP -> "BULLISH BREAKOUT ABOVE ORB HIGH"
                    OrbState.BREAKOUT_DOWN -> "BEARISH BREAKDOWN BELOW ORB LOW"
                    OrbState.INSIDE_RANGE -> "CONSOLIDATING INSIDE 15M ORB RANGE"
                }
                val orbColor = when (orbState) {
                    OrbState.BREAKOUT_UP -> colors.bullish
                    OrbState.BREAKOUT_DOWN -> colors.bearish
                    OrbState.INSIDE_RANGE -> colors.sideways
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.cardElevated)
                        .border(1.dp, orbColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "15-MIN OPENING RANGE (ORB)",
                                color = colors.textMuted,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = orbText,
                                color = orbColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ORB High: ₹${indicators?.let { String.format("%.1f", it.orbHigh) } ?: "--"}",
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "ORB Low: ₹${indicators?.let { String.format("%.1f", it.orbLow) } ?: "--"}",
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. SUPPORT & RESISTANCE (PIVOT LEVELS)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "SUPPORT & RESISTANCE LEVELS",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                srLevels?.let { sr ->
                    PivotRow(label = "Resistance 2 (R2)", price = "₹${String.format("%.1f", sr.r2)}", distance = spot - sr.r2, color = colors.bearish)
                    PivotRow(label = "Resistance 1 (R1)", price = "₹${String.format("%.1f", sr.r1)}", distance = spot - sr.r1, color = colors.bearish)
                    PivotRow(label = "Previous Day High (PDH)", price = "₹${String.format("%.1f", sr.pdh)}", distance = spot - sr.pdh, color = colors.bearish)
                    PivotRow(label = "Central Pivot Point (PP)", price = "₹${String.format("%.1f", sr.pivot)}", distance = spot - sr.pivot, color = colors.textPrimary, isPivot = true)
                    PivotRow(label = "Previous Day Low (PDL)", price = "₹${String.format("%.1f", sr.pdl)}", distance = spot - sr.pdl, color = colors.bullish)
                    PivotRow(label = "Support 1 (S1)", price = "₹${String.format("%.1f", sr.s1)}", distance = spot - sr.s1, color = colors.bullish)
                    PivotRow(label = "Support 2 (S2)", price = "₹${String.format("%.1f", sr.s2)}", distance = spot - sr.s2, color = colors.bullish)
                } ?: run {
                    Text("Support and resistance calculations pending market quote.", color = colors.textSecondary, fontSize = 15.sp)
                }
            }
        }

        // ==========================================
        // 3. HISTORICAL BACKTEST & STRATEGY AUDIT
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HISTORICAL STRATEGY AUDIT",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    IconButton(
                        onClick = { viewModel.setBacktestDialogVisible(true) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Analytics, contentDescription = "Full Audit", tint = colors.bullish)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                backtest?.let { bt ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("WIN RATE", color = colors.textMuted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${String.format("%.1f", bt.winRate)}%", color = colors.bullish, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }

                        Column {
                            Text("PROFIT FACTOR", color = colors.textMuted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(String.format("%.2f", bt.profitFactor), color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }

                        Column {
                            Text("MAX DRAWDOWN", color = colors.textMuted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${String.format("%.1f", bt.maxDrawdownPercent)}%", color = colors.bearish, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }

                        Column {
                            Text("TRADES", color = colors.textMuted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${bt.totalTrades}", color = colors.textSecondary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.runBacktestSimulation() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp), // Touch target >= 48dp
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.cardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.bullish)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run", tint = colors.bullish)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RE-RUN HISTORICAL BACKTEST", color = colors.bullish, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun IndicatorBox(
    title: String,
    value: String,
    subtitle: String,
    subtitleColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardElevated)
            .padding(14.dp)
    ) {
        Column {
            Text(text = title, color = colors.textMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, color = subtitleColor, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmaPill(label: String, value: String, color: Color) {
    val colors = LocalTerminalColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PivotRow(
    label: String,
    price: String,
    distance: Double,
    color: Color,
    isPivot: Boolean = false
) {
    val colors = LocalTerminalColors.current
    val distSign = if (distance >= 0) "+" else ""
    val distDisplay = "$distSign${String.format("%.1f", distance)} pts"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isPivot) colors.accentBlue else colors.textPrimary,
            fontSize = 15.5.sp,
            fontWeight = if (isPivot) FontWeight.Bold else FontWeight.Medium
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = price,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "($distDisplay)",
                color = colors.textMuted,
                fontSize = 13.sp
            )
        }
    }
}
