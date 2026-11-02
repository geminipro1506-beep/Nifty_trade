package com.cloudscope.moneyprinter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import com.cloudscope.moneyprinter.ui.viewmodel.MainUiState
import com.cloudscope.moneyprinter.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PerformanceScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val metrics = uiState.accuracyMetrics
    val signals = uiState.historicalSignals
    val insights = uiState.strategyInsights
    val comparisons = uiState.strategyComparisons
    val paperAccount = uiState.paperAccount

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Notification Banner
        uiState.notificationMessage?.let { msg ->
            item {
                NotificationBanner(
                    message = msg,
                    onDismiss = { viewModel.clearNotification() }
                )
            }
        }

        // Screen Header
        item {
            PerformanceHeader()
        }

        // Filter Bar (Period, Strategy, Option type, Paper toggle)
        item {
            FilterSection(
                selectedPeriod = uiState.selectedAccuracyPeriod,
                selectedStrategy = uiState.selectedStrategyFilter,
                selectedOption = uiState.selectedOptionFilter,
                paperTradingOnly = uiState.paperTradingOnlyFilter,
                onPeriodSelected = { viewModel.setAccuracyPeriod(it) },
                onStrategySelected = { viewModel.setStrategyFilter(it) },
                onOptionSelected = { viewModel.setOptionFilter(it) },
                onTogglePaperOnly = { viewModel.togglePaperTradingOnlyFilter() }
            )
        }

        // Primary KPI Metric Dashboard
        if (metrics != null) {
            item {
                KpiMetricsSection(metrics = metrics)
            }

            item {
                TargetDistributionCard(metrics = metrics)
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Accumulating trade performance data...", color = colors.textSecondary, fontSize = 14.sp)
                    }
                }
            }
        }

        // Strategy Improvement Engine
        item {
            StrategyImprovementSection(
                insights = insights,
                comparisons = comparisons
            )
        }

        // Paper Trading Simulator Portfolio
        item {
            PaperTradingPortfolioCard(
                account = paperAccount,
                onReset = { viewModel.resetPaperAccount() }
            )
        }

        // Signal Audit Ledger Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORICAL SIGNAL AUDIT TRAIL",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${signals.size} Signals Recorded",
                    color = colors.bullish,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Signal Audit Items
        if (signals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No trade signals found for current filter selection.", color = colors.textSecondary, fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(signals, key = { it.id }) { record ->
                HistoricalSignalCard(record = record)
            }
        }

        // SEBI Regulatory Disclaimer
        item {
            DisclaimerFooter()
        }
    }
}

@Composable
private fun PerformanceHeader() {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ACCURACY & PERFORMANCE TRACKER",
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Walk-Forward Statistical Engine • Multi-Dimensional Verification • Net P&L",
            color = colors.textSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun NotificationBanner(
    message: String,
    onDismiss: () -> Unit
) {
    val colors = LocalTerminalColors.current
    Surface(
        color = colors.bullishBg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.bullish.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.bullish, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = message, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun FilterSection(
    selectedPeriod: AccuracyPeriod,
    selectedStrategy: StrategyFilter,
    selectedOption: OptionFilter,
    paperTradingOnly: Boolean,
    onPeriodSelected: (AccuracyPeriod) -> Unit,
    onStrategySelected: (StrategyFilter) -> Unit,
    onOptionSelected: (OptionFilter) -> Unit,
    onTogglePaperOnly: () -> Unit
) {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Period tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AccuracyPeriod.values()) { period ->
                val isSelected = selectedPeriod == period
                FilterChipItem(
                    label = period.label,
                    isSelected = isSelected,
                    onClick = { onPeriodSelected(period) }
                )
            }
        }

        // Secondary filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Strategy Filter
            FilterChipItem(
                label = selectedStrategy.label,
                isSelected = selectedStrategy != StrategyFilter.ALL,
                onClick = {
                    val next = when (selectedStrategy) {
                        StrategyFilter.ALL -> StrategyFilter.BREAKOUT
                        StrategyFilter.BREAKOUT -> StrategyFilter.REVERSAL
                        StrategyFilter.REVERSAL -> StrategyFilter.TREND_FOLLOWING
                        StrategyFilter.TREND_FOLLOWING -> StrategyFilter.ALL
                    }
                    onStrategySelected(next)
                },
                modifier = Modifier.weight(1f)
            )

            // Option Filter
            FilterChipItem(
                label = selectedOption.label,
                isSelected = selectedOption != OptionFilter.ALL,
                onClick = {
                    val next = when (selectedOption) {
                        OptionFilter.ALL -> OptionFilter.CE_ONLY
                        OptionFilter.CE_ONLY -> OptionFilter.PE_ONLY
                        OptionFilter.PE_ONLY -> OptionFilter.ALL
                    }
                    onOptionSelected(next)
                },
                modifier = Modifier.weight(1f)
            )

            // Paper Only Toggle
            FilterChipItem(
                label = if (paperTradingOnly) "Paper Only" else "All Trades",
                isSelected = paperTradingOnly,
                onClick = onTogglePaperOnly,
                modifier = Modifier.weight(0.9f)
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colors.bullish else colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else colors.textPrimary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun KpiMetricsSection(metrics: AccuracyMetrics) {
    val colors = LocalTerminalColors.current

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Main Row: Win Rate & Net PnL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Win Rate Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("EMPIRICAL WIN RATE", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format("%.1f", metrics.winRate)}%",
                        color = if (metrics.winRate >= 60.0) colors.bullish else if (metrics.winRate >= 50.0) colors.warning else colors.bearish,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${metrics.winningTrades}W / ${metrics.losingTrades}L (${metrics.totalExecutedTrades} executed)",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Net P&L Card
            val isNetPositive = metrics.netPnl >= 0
            Card(
                modifier = Modifier.weight(1.2f),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("NET P&L (POST FRICTION)", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (isNetPositive) "+" else ""}₹${String.format("%,.2f", metrics.netPnl)}",
                        color = if (isNetPositive) colors.bullish else colors.bearish,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Gross: ₹${String.format("%,.0f", metrics.grossProfit - metrics.grossLoss)} | Charges: ₹${String.format("%,.0f", metrics.totalChargesAndTaxes)}",
                        color = colors.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Secondary Row: Profit Factor, Expectancy, Max Drawdown, Avg Holding Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallMetricCard(
                title = "PROFIT FACTOR",
                value = String.format("%.2f", metrics.profitFactor),
                color = if (metrics.profitFactor >= 1.5) colors.bullish else colors.warning,
                modifier = Modifier.weight(1f)
            )
            SmallMetricCard(
                title = "EXPECTANCY",
                value = "₹${String.format("%.0f", metrics.expectancyPerTrade)}",
                color = if (metrics.expectancyPerTrade > 0) colors.bullish else colors.bearish,
                modifier = Modifier.weight(1f)
            )
            SmallMetricCard(
                title = "MAX DRAWDOWN",
                value = "${String.format("%.1f", metrics.maxDrawdownPercent)}%",
                color = if (metrics.maxDrawdownPercent < 10.0) colors.bullish else colors.bearish,
                modifier = Modifier.weight(1f)
            )
            SmallMetricCard(
                title = "AVG HOLDING",
                value = "${metrics.averageHoldingTimeMinutes}m",
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SmallMetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = colors.textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun TargetDistributionCard(metrics: AccuracyMetrics) {
    val colors = LocalTerminalColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "TARGET & STOP-LOSS HIT DISTRIBUTION",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TargetRateItem("Target 1", "${String.format("%.1f", metrics.target1HitRate)}%", colors.bullish)
                TargetRateItem("Target 2", "${String.format("%.1f", metrics.target2HitRate)}%", colors.bullish)
                TargetRateItem("Target 3", "${String.format("%.1f", metrics.target3HitRate)}%", colors.bullish)
                TargetRateItem("Stop Loss", "${String.format("%.1f", metrics.stopLossHitRate)}%", colors.bearish)
                TargetRateItem("Ambiguous", "${metrics.ambiguousTrades}", colors.warning)
            }
        }
    }
}

@Composable
private fun TargetRateItem(label: String, value: String, color: Color) {
    val colors = LocalTerminalColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = colors.textSecondary, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun StrategyImprovementSection(
    insights: List<StrategyImprovementInsight>,
    comparisons: List<StrategyPerformanceComparison>
) {
    val colors = LocalTerminalColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = colors.bullish, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STRATEGY IMPROVEMENT ENGINE",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Automated AI statistical insights
            insights.forEach { insight ->
                Surface(
                    color = colors.background,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(insight.category.uppercase(), color = colors.bullish, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(insight.statisticalEdge, color = colors.textSecondary, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(insight.observation, color = colors.textPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Action: ${insight.recommendation}", color = colors.textSecondary, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
            }

            // Strategy Version Benchmark Comparison Table
            Spacer(modifier = Modifier.height(4.dp))
            Text("VERSION BENCHMARK COMPARISON", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)

            comparisons.forEach { comp ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.background)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(comp.version, color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(comp.status, color = if (comp.status == "ACTIVE LIVE") colors.bullish else colors.textSecondary, fontSize = 9.sp)
                    }
                    Text("${comp.winRate}% WR", color = colors.bullish, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${comp.profitFactor} PF", color = colors.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("₹${String.format("%,.0f", comp.netPnl)}", color = if (comp.netPnl >= 0) colors.bullish else colors.bearish, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun PaperTradingPortfolioCard(
    account: PaperTradingAccount,
    onReset: () -> Unit
) {
    val colors = LocalTerminalColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = colors.bullish, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PAPER TRADING PORTFOLIO", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onReset,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Reset ₹1L", color = colors.warning, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("VIRTUAL CAPITAL", color = colors.textSecondary, fontSize = 10.sp)
                    Text("₹${String.format("%,.2f", account.currentBalance)}", color = colors.bullish, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TOTAL NET P&L", color = colors.textSecondary, fontSize = 10.sp)
                    Text(
                        text = "${if (account.totalNetPnl >= 0) "+" else ""}₹${String.format("%,.2f", account.totalNetPnl)}",
                        color = if (account.totalNetPnl >= 0) colors.bullish else colors.bearish,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Active Positions: ${account.activePositionsCount}", color = colors.textSecondary, fontSize = 11.sp)
                Text("Closed Trades: ${account.closedPositionsCount}", color = colors.textSecondary, fontSize = 11.sp)
                Text("Paper Win Rate: ${account.winRate}%", color = colors.bullish, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HistoricalSignalCard(record: HistoricalSignalRecord) {
    val colors = LocalTerminalColors.current
    var isExpanded by remember { mutableStateOf(false) }

    val outcomeColor = when (record.outcome) {
        TradeOutcome.WIN -> colors.bullish
        TradeOutcome.LOSS -> colors.bearish
        TradeOutcome.AMBIGUOUS -> colors.warning
        TradeOutcome.BREAKEVEN -> colors.textSecondary
        TradeOutcome.CANCELLED, TradeOutcome.UNFILLED -> colors.textSecondary
        null -> colors.warning
    }

    val actionColor = if (record.action == SignalAction.BUY_CE) colors.bullish else colors.bearish

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Action + Contract + Outcome Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (record.action == SignalAction.BUY_CE) colors.bullishBg else colors.bearishBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = record.action.name.replace("_", " "),
                            color = actionColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.contractSymbol,
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (record.isPaperTrade) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = colors.warning.copy(alpha = 0.2f), shape = RoundedCornerShape(3.dp)) {
                            Text("PAPER", color = colors.warning, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }

                Surface(
                    color = outcomeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = record.outcome?.name ?: record.status.name,
                        color = outcomeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Entry, Exit, Net P&L
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Entry / Exit", color = colors.textSecondary, fontSize = 10.sp)
                    Text(
                        text = "₹${record.entryPrice} → ${record.actualExitPrice?.let { "₹$it" } ?: "--"}",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Exit Reason", color = colors.textSecondary, fontSize = 10.sp)
                    Text(
                        text = record.exitReason?.name ?: "--",
                        color = outcomeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Net P&L (after fees)", color = colors.textSecondary, fontSize = 10.sp)
                    Text(
                        text = if (record.netPnl >= 0) "+₹${String.format("%.2f", record.netPnl)}" else "-₹${String.format("%.2f", kotlin.math.abs(record.netPnl))}",
                        color = if (record.netPnl >= 0) colors.bullish else colors.bearish,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Expanded audit trail
            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = colors.background, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AuditLine("Signal ID", record.id)
                    AuditLine("Timestamp", formatDateTime(record.timestamp))
                    AuditLine("Spot at Signal", "₹${record.spotPrice}")
                    AuditLine("Targets & SL", "T1: ₹${record.target1} | T2: ₹${record.target2} | T3: ₹${record.target3} | SL: ₹${record.stopLoss}")
                    AuditLine("Indicators", "RSI: ${String.format("%.1f", record.rsi14)} | VWAP: ₹${String.format("%.1f", record.vwap)} | PCR: ${record.pcr} | VIX: ${record.vix}")
                    AuditLine("Confidence", "${record.confidenceScore}%")
                    AuditLine("Friction & Taxes", "₹${record.totalCharges} (Roundtrip ₹40 + STT + GST + 0.5pt slippage)")
                    AuditLine("Holding Time", "${record.holdingTimeMinutes} min")
                    AuditLine("Invalidation", record.invalidationConditions)
                    if (record.adjustmentsLog.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Lifecycle Adjustments Log:", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        record.adjustmentsLog.forEach { logItem ->
                            Text("• $logItem", color = colors.textSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLine(label: String, value: String) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.textSecondary, fontSize = 10.sp)
        Text(value, color = colors.textPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun DisclaimerFooter() {
    val colors = LocalTerminalColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "REGULATORY NOTICE & DATA INTEGRITY GUARANTEE",
            color = colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "CloudScope terminal computes empirical win rates from immutable walk-forward signals with standard institutional statutory charges (brokerage ₹40, STT 0.1%, GST 18%, 0.5pt slippage). Ambiguous candles (target & SL reached in same window) are strictly marked breakeven with loss of charges. Past returns do not guarantee future profitability. Derivatives trading involves high financial risk under SEBI guidelines.",
            color = colors.textSecondary.copy(alpha = 0.7f),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm:ss IST", Locale.ENGLISH)
    sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    return sdf.format(Date(timestamp))
}
