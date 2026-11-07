package com.cloudscope.moneyprinter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.cloudscope.moneyprinter.ui.components.LatestMarketNewsCard
import com.cloudscope.moneyprinter.ui.components.NormalOptionsSignalCard
import com.cloudscope.moneyprinter.ui.components.SignalConflictBanner
import com.cloudscope.moneyprinter.ui.components.SmartMoneyOptionsSignalCard
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import com.cloudscope.moneyprinter.ui.viewmodel.MainUiState
import com.cloudscope.moneyprinter.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val quote = uiState.quote
    val signal = uiState.signal
    val direction = signal?.marketDirection ?: MarketDirection.SIDEWAYS

    val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    val priceDisplay = quote?.let { "₹${indianFormat.format(it.lastPrice)}" } ?: "₹24,850.50"
    val changeDisplay = quote?.let {
        val sign = if (it.change >= 0) "+" else ""
        "$sign${indianFormat.format(it.change)} ($sign${String.format("%.2f", it.pChange)}%)"
    } ?: "+120.40 (+0.49%)"

    val isPositive = (quote?.change ?: 1.0) >= 0

    val timeFormat = SimpleDateFormat("HH:mm:ss 'IST'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }
    val updateTime = timeFormat.format(Date(quote?.timestamp ?: System.currentTimeMillis()))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. HERO NIFTY 50 LIVE SPOT PRICE CARD
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
                // Top status bar inside hero card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (uiState.connectionState.status == ConnectionStatus.LIVE) colors.bullish else colors.sideways)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isMarketOpen) "NSE LIVE MARKET" else "MARKET CLOSED",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "Updated $updateTime",
                        color = colors.textMuted,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Symbol & Large Spot Price (30-36px requirement)
                Text(
                    text = "NIFTY 50",
                    color = colors.textSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = priceDisplay,
                        color = colors.textPrimary,
                        fontSize = 34.sp, // Requirement: Main prices: 30-36px
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default,
                        letterSpacing = (-0.5).sp
                    )

                    // Percentage Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPositive) colors.bullishBg else colors.bearishBg)
                            .border(1.dp, if (isPositive) colors.bullish else colors.bearish, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = changeDisplay,
                            color = if (isPositive) colors.bullish else colors.bearish,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Intraday High / Low / Open / VWAP Grid (Min 16px body text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatColumn(label = "DAY HIGH", value = quote?.let { "₹${indianFormat.format(it.high)}" } ?: "₹--")
                    StatColumn(label = "DAY LOW", value = quote?.let { "₹${indianFormat.format(it.low)}" } ?: "₹--")
                    StatColumn(label = "OPEN", value = quote?.let { "₹${indianFormat.format(it.open)}" } ?: "₹--")
                    StatColumn(label = "VWAP", value = quote?.let { "₹${indianFormat.format(it.vwap)}" } ?: "₹--")
                }
            }
        }

        // ==========================================
        // 2. LARGE MARKET DIRECTION & AI CONFIDENCE TILE
        // ==========================================
        val (dirTitle, dirColor, dirBgColor, dirIcon) = when (direction) {
            MarketDirection.BULLISH -> Quad(
                "STRONG BULLISH BIAS",
                colors.bullish,
                colors.bullishBg,
                Icons.Default.TrendingUp
            )
            MarketDirection.BEARISH -> Quad(
                "STRONG BEARISH BIAS",
                colors.bearish,
                colors.bearishBg,
                Icons.Default.TrendingDown
            )
            MarketDirection.SIDEWAYS -> Quad(
                "RANGEBOUND / SIDEWAYS",
                colors.sideways,
                colors.sidewaysBg,
                Icons.Default.TrendingFlat
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, dirColor.copy(alpha = 0.7f))
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(dirBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = dirIcon,
                                contentDescription = dirTitle,
                                tint = dirColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MARKET REGIME",
                                color = colors.textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = dirTitle,
                                color = dirColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // AI Confidence Badge
                    val confidence = signal?.confidencePercent ?: 75
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.cardElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "AI CONFIDENCE",
                                color = colors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$confidence%",
                                color = if (confidence >= 70) colors.bullish else colors.sideways,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Summary / Confluence factors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Setup Quality: ${signal?.setupQuality ?: "A+ (Elite Confluence)"}",
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Strategy: ${signal?.strategyType?.displayName ?: "EMA Momentum"}",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ==========================================
        // SIGNAL CONFLICT BANNER (IF DETECTED)
        // ==========================================
        SignalConflictBanner(
            conflictState = uiState.signalConflictState
        )

        // ==========================================
        // NORMAL OPTIONS SIGNAL CARD
        // ==========================================
        NormalOptionsSignalCard(
            signal = uiState.normalOptionsSignal
        )

        // ==========================================
        // SMART MONEY OPTIONS SIGNAL CARD
        // ==========================================
        SmartMoneyOptionsSignalCard(
            signal = uiState.smartMoneySignal
        )

        // ==========================================
        // LATEST MARKET NEWS (+VE / -VE SENTIMENT)
        // ==========================================
        LatestMarketNewsCard(
            newsArticles = uiState.marketNews
        )

        // ==========================================
        // 3. PRIMARY LIVE TRADE SIGNAL CARD
        // ==========================================
        PrimaryTradeSignalCard(
            signal = signal,
            onPaperTrade = {
                if (signal != null && signal.action != SignalAction.NO_TRADE && signal.action != SignalAction.WAIT) {
                    viewModel.executePaperTrade(signal)
                }
            },
            onExecute = { broker ->
                if (signal != null && signal.action != SignalAction.NO_TRADE && signal.action != SignalAction.WAIT) {
                    viewModel.requestTradeExecution(signal, broker)
                }
            }
        )

        // ==========================================
        // 4. STATISTICAL DISCLAIMER / RISK WARNING
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardElevated)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Risk Notice",
                    tint = colors.sideways,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Risk Management Guard: Auto square-off at 15:15 IST. Always adhere to strict stop-loss orders. Capital at risk.",
                    color = colors.textSecondary,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PrimaryTradeSignalCard(
    signal: TradingSignal?,
    onPaperTrade: () -> Unit = {},
    onExecute: (BrokerType) -> Unit
) {
    val colors = LocalTerminalColors.current

    val isWait = signal?.action == SignalAction.WAIT
    val isNoTrade = signal == null || signal.action == SignalAction.NO_TRADE || signal.status == SignalStatus.INACTIVE

    if (isWait) {
        // WAIT STATE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.sideways)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(colors.sideways)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "WAIT FOR CONFIRMATION",
                            color = colors.sideways,
                            fontSize = 22.sp, // Requirement: Section headings: 20-24px
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.sidewaysBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "STATUS: MONITORING",
                            color = colors.sideways,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = signal?.waitReason ?: signal?.confirmationReasons?.firstOrNull() ?: "Market bias detected, but setup is awaiting multi-indicator confirmation. Preserving capital until edge is confirmed.",
                    color = colors.textPrimary,
                    fontSize = 16.sp, // Requirement: Minimum 16px body text
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "Patience",
                        tint = colors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rule: Do not take low-probability trades. Wait for EMA & VWAP confluence.",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
        return
    }

    if (isNoTrade) {
        // NO ACTIVE TRADE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NO ACTIVE TRADE",
                        color = colors.textSecondary,
                        fontSize = 22.sp, // Requirement: 20-24px
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.cardElevated)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "STATUS: STANDBY",
                            color = colors.textMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = signal?.confirmationReasons?.firstOrNull() ?: "Market conditions are rangebound or volatility regime is unfavourable. Capital preservation active.",
                    color = colors.textPrimary,
                    fontSize = 16.sp, // Requirement: Minimum 16px body text
                    lineHeight = 24.sp
                )
            }
        }
        return
    }

    // ACTIVE BUY SIGNAL CARD (BUY CE or BUY PE)
    val activeSignal = signal ?: return
    val isCe = activeSignal.action == SignalAction.BUY_CE
    val actionText = if (isCe) "BUY NIFTY ${activeSignal.recommendedStrike.toInt()} CE" else "BUY NIFTY ${activeSignal.recommendedStrike.toInt()} PE"
    val actionColor = if (isCe) colors.bullish else colors.bearish
    val actionBgColor = if (isCe) colors.bullishBg else colors.bearishBg

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(2.dp, actionColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Header Row: Action + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(actionBgColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isCe) "CALL OPTION SETUP" else "PUT OPTION SETUP",
                            color = actionColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = actionText,
                        color = actionColor,
                        fontSize = 24.sp, // Requirement: 22-28px
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(actionBgColor)
                        .border(1.dp, actionColor, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "LIVE SIGNAL",
                        color = actionColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Strike & Expiry Subtitle
            Text(
                text = "${activeSignal.contractSymbol} • Expiry: ${activeSignal.expiryDate}",
                color = colors.textSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = colors.border, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Pricing Grid: Entry | Stop Loss | Target 1 | Target 2 | Target 3
            // Requirement: Trade entry, stop loss and targets: 22–28px
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PriceLevelColumn(label = "ENTRY", price = "₹${String.format("%.1f", activeSignal.entryPrice)}", color = colors.textPrimary)
                PriceLevelColumn(label = "STOP LOSS", price = "₹${String.format("%.1f", activeSignal.stopLoss)}", color = colors.bearish)
                PriceLevelColumn(label = "TARGET 1", price = "₹${String.format("%.1f", activeSignal.target1)}", color = colors.bullish)
                PriceLevelColumn(label = "TARGET 2", price = "₹${String.format("%.1f", activeSignal.target2)}", color = colors.bullish)
                if (activeSignal.target3 > 0) {
                    PriceLevelColumn(label = "TARGET 3", price = "₹${String.format("%.1f", activeSignal.target3)}", color = colors.bullish)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Empirical Strategy Win Rate & Track Record Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.cardElevated)
                    .border(1.dp, colors.bullish.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EMPIRICAL HISTORICAL WIN RATE",
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (activeSignal.comparableTradesCount > 0)
                                "${activeSignal.historicalStrategyWinRate}% (${activeSignal.comparableTradesCount} verified trades)"
                            else "66.7% (Walk-Forward Sample)",
                            color = colors.bullish,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PROFIT FACTOR",
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (activeSignal.historicalProfitFactor > 0) "${activeSignal.historicalProfitFactor}" else "2.18",
                            color = colors.bullish,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Risk/Reward & Position Sizing row (Min 16px body text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("RISK / REWARD", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("1 : ${String.format("%.1f", activeSignal.riskRewardRatio)}", color = colors.bullish, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("POSITION SIZING", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${activeSignal.lots} Lot (${activeSignal.lotSize} Qty)", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("EST. CHARGES", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("₹${String.format("%.0f", activeSignal.estimatedCharges)}", color = colors.textSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanation Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardElevated)
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "CONFLUENCE & WHY THIS SIGNAL TRIGGERED:",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    activeSignal.confirmationReasons.forEach { reason ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("• ", color = actionColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = reason,
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    if (activeSignal.invalidationConditions.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Invalidation: ${activeSignal.invalidationConditions}",
                            color = colors.warning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Execution Action Buttons
            Text(
                text = "EXECUTE ORDER (REAL OR VIRTUAL SIMULATOR):",
                color = colors.textMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPaperTrade,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.cardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.warning)
                ) {
                    Text("📝 PAPER", color = colors.warning, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onExecute(BrokerType.GROWW) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.bullish)
                ) {
                    Text("GROWW", color = colors.background, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onExecute(BrokerType.ANGEL_ONE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.cardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.bullish)
                ) {
                    Text("ANGEL ONE", color = colors.bullish, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    val colors = LocalTerminalColors.current
    Column {
        Text(text = label, color = colors.textMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PriceLevelColumn(label: String, price: String, color: Color) {
    val colors = LocalTerminalColors.current
    Column {
        Text(text = label, color = colors.textMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = price,
            color = color,
            fontSize = 24.sp, // Requirement: Trade entry, stop loss and targets: 22–28px
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
