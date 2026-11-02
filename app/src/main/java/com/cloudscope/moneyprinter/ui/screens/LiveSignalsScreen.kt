package com.cloudscope.moneyprinter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import com.cloudscope.moneyprinter.ui.viewmodel.MainUiState
import com.cloudscope.moneyprinter.ui.viewmodel.MainViewModel

@Composable
fun LiveSignalsScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val signal = uiState.signal
    val indicators = uiState.indicators
    val quote = uiState.quote

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. SIGNAL HERO BANNER
        // ==========================================
        val isWait = signal?.action == SignalAction.WAIT
        val isNoTrade = signal == null || signal.action == SignalAction.NO_TRADE || signal.status == SignalStatus.INACTIVE
        val isCe = signal?.action == SignalAction.BUY_CE

        val (themeColor, themeBgColor, statusTitle) = when {
            isWait -> Triple(colors.sideways, colors.sidewaysBg, "WAIT FOR CONFIRMATION")
            isNoTrade -> Triple(colors.textSecondary, colors.cardElevated, "NO ACTIVE SIGNAL")
            isCe -> Triple(colors.bullish, colors.bullishBg, "BUY CALL (CE) SIGNAL")
            else -> Triple(colors.bearish, colors.bearishBg, "BUY PUT (PE) SIGNAL")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, themeColor)
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeBgColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusTitle,
                            color = themeColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "Confidence: ${signal?.confidencePercent ?: 0}%",
                        color = themeColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isWait) {
                    Text(
                        text = "Setup forming — Waiting for final technical confirmation",
                        color = colors.textPrimary,
                        fontSize = 22.sp, // Section headings: 20-24px
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = signal?.waitReason ?: "Directional bias detected, but momentum or EMA alignment is incomplete. Wait for 9 EMA expansion and volume breakout.",
                        color = colors.textSecondary,
                        fontSize = 16.sp, // Minimum 16px body text
                        lineHeight = 24.sp
                    )
                } else if (isNoTrade) {
                    Text(
                        text = "Capital Preservation Active",
                        color = colors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = signal?.confirmationReasons?.firstOrNull() ?: "Market conditions are rangebound or volatility regime is unfavourable for buying options.",
                        color = colors.textSecondary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                } else {
                    // Active Trade Setup
                    Text(
                        text = signal?.contractSymbol ?: "",
                        color = colors.textPrimary,
                        fontSize = 24.sp, // Section headings: 20-24px
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Expiry: ${signal?.expiryDate} • Strategy: ${signal?.strategyType?.displayName}",
                        color = colors.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Pricing Grid: Entry, SL, T1, T2 (22-28px requirement)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SignalPriceItem(label = "ENTRY", price = "₹${String.format("%.2f", signal?.entryPrice ?: 0.0)}", color = colors.textPrimary)
                        SignalPriceItem(label = "STOP LOSS", price = "₹${String.format("%.2f", signal?.stopLoss ?: 0.0)}", color = colors.bearish)
                        SignalPriceItem(label = "TARGET 1", price = "₹${String.format("%.2f", signal?.target1 ?: 0.0)}", color = colors.bullish)
                        SignalPriceItem(label = "TARGET 2", price = "₹${String.format("%.2f", signal?.target2 ?: 0.0)}", color = colors.bullish)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Risk/Reward: 1:${String.format("%.1f", signal?.riskRewardRatio ?: 2.0)}",
                            color = colors.bullish,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Capital: ₹${String.format("%.0f", signal?.capitalRequired ?: 0.0)} (1 Lot)",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.requestTradeExecution(signal!!, BrokerType.GROWW) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.bullish)
                        ) {
                            Text("⚡ GROWW", color = colors.background, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.requestTradeExecution(signal!!, BrokerType.ANGEL_ONE) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.cardElevated),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.bullish)
                        ) {
                            Text("⚡ ANGEL ONE", color = colors.bullish, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. STRATEGY CONFLUENCE CHECKLIST
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
                    text = "TECHNICAL CONFLUENCE CHECKLIST",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                val spot = quote?.lastPrice ?: 24850.0
                val vwap = indicators?.vwap ?: 24830.0
                val ema9 = indicators?.ema9 ?: 24860.0
                val ema20 = indicators?.ema20 ?: 24840.0
                val ema50 = indicators?.ema50 ?: 24810.0
                val rsi = indicators?.rsi14 ?: 58.0
                val pcr = indicators?.pcr ?: 1.15
                val macdHist = indicators?.macdHistogram ?: 4.2
                val orbState = indicators?.orbState ?: OrbState.INSIDE_RANGE

                ConfluenceRow(
                    label = "VWAP Price Action",
                    detail = if (spot > vwap) "Price (₹${spot.toInt()}) above VWAP (₹${vwap.toInt()})" else "Price (₹${spot.toInt()}) below VWAP (₹${vwap.toInt()})",
                    isConfirmed = if (isCe) spot > vwap else spot < vwap
                )

                ConfluenceRow(
                    label = "EMA Sequence (9 / 20 / 50)",
                    detail = if (ema9 > ema20) "9 EMA (₹${ema9.toInt()}) > 20 EMA (₹${ema20.toInt()}) Bullish" else "9 EMA < 20 EMA Bearish",
                    isConfirmed = if (isCe) ema9 > ema20 && ema20 > ema50 else ema9 < ema20
                )

                ConfluenceRow(
                    label = "RSI 14 Momentum",
                    detail = "RSI at ${rsi.toInt()} ${if (rsi in 54.0..70.0) "(Bullish Expansion)" else if (rsi in 30.0..46.0) "(Bearish Expansion)" else "(Neutral Range)"}",
                    isConfirmed = if (isCe) rsi in 52.0..72.0 else rsi in 28.0..48.0
                )

                ConfluenceRow(
                    label = "MACD Histogram Momentum",
                    detail = "Histogram at ${String.format("%.2f", macdHist)} ${if (macdHist > 0) "Positive Bullish" else "Negative Bearish"}",
                    isConfirmed = if (isCe) macdHist > 0 else macdHist < 0
                )

                ConfluenceRow(
                    label = "Put-Call Ratio (PCR) OI Flow",
                    detail = "PCR at ${String.format("%.2f", pcr)} ${if (pcr >= 1.05) "Confirms Put Writing Support" else if (pcr <= 0.95) "Confirms Call Writing Resistance" else "Neutral Sentiment"}",
                    isConfirmed = if (isCe) pcr >= 1.05 else pcr <= 0.95
                )

                ConfluenceRow(
                    label = "15-Minute ORB Range",
                    detail = when (orbState) {
                        OrbState.BREAKOUT_UP -> "Bullish Breakout above ORB High"
                        OrbState.BREAKOUT_DOWN -> "Bearish Breakdown below ORB Low"
                        OrbState.INSIDE_RANGE -> "Inside 15-Minute Opening Range"
                    },
                    isConfirmed = orbState != OrbState.INSIDE_RANGE
                )
            }
        }

        // ==========================================
        // 3. EXECUTION & RISK MANAGEMENT RULES
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
                    text = "INTRADAY TRADE MANAGEMENT RULES",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                RuleItem(title = "Strict Stop Loss", description = "Never enter an option trade without entering the stop-loss order simultaneously. Max loss capped per lot.")
                RuleItem(title = "Target 1 Trailing Rule", description = "Once Target 1 is reached (+50% risk), immediately trail Stop Loss to cost (Break-even).")
                RuleItem(title = "15:15 IST Mandatory Square-Off", description = "All intraday positions must be closed by 15:15 IST to prevent overnight gap risk and gamma decay.")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SignalPriceItem(label: String, price: String, color: Color) {
    Column {
        Text(text = label, color = LocalTerminalColors.current.textMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = price,
            color = color,
            fontSize = 24.sp, // Requirement: 22-28px
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ConfluenceRow(label: String, detail: String, isConfirmed: Boolean) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isConfirmed) colors.bullishBg else colors.cardElevated)
                .border(1.dp, if (isConfirmed) colors.bullish else colors.textMuted, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isConfirmed) Icons.Default.Check else Icons.Default.HorizontalRule,
                contentDescription = if (isConfirmed) "Confirmed" else "Pending",
                tint = if (isConfirmed) colors.bullish else colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                color = colors.textPrimary,
                fontSize = 16.sp, // Requirement: Minimum 16px body text
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detail,
                color = colors.textSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun RuleItem(title: String, description: String) {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = colors.accentBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = description,
            color = colors.textSecondary,
            fontSize = 14.5.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 26.dp)
        )
    }
}
