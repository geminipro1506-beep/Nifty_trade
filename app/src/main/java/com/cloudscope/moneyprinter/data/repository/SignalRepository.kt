package com.cloudscope.moneyprinter.data.repository

import com.cloudscope.moneyprinter.data.local.db.SignalDatabaseHelper
import com.cloudscope.moneyprinter.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

interface SignalRepository {
    val signalsFlow: StateFlow<List<HistoricalSignalRecord>>
    suspend fun recordGeneratedSignal(record: HistoricalSignalRecord): Long
    suspend fun updateSignalLifecycle(record: HistoricalSignalRecord): Int
    suspend fun getAllSignals(): List<HistoricalSignalRecord>
    suspend fun getSignalsFiltered(
        fromTimestamp: Long = 0L,
        optionFilter: OptionFilter = OptionFilter.ALL,
        strategyFilter: StrategyFilter = StrategyFilter.ALL,
        paperTradesOnly: Boolean? = null
    ): List<HistoricalSignalRecord>
    suspend fun getOpenSignals(): List<HistoricalSignalRecord>
    suspend fun getSignalById(id: String): HistoricalSignalRecord?
    fun computeAccuracyMetrics(signals: List<HistoricalSignalRecord>): AccuracyMetrics
}

class SignalRepositoryImpl(
    private val dbHelper: SignalDatabaseHelper? = null
) : SignalRepository {

    private val _signalsFlow = MutableStateFlow<List<HistoricalSignalRecord>>(emptyList())
    override val signalsFlow: StateFlow<List<HistoricalSignalRecord>> = _signalsFlow.asStateFlow()

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                val list = dbHelper?.getAllSignals() ?: emptyList()
                _signalsFlow.value = list
            } catch (e: Throwable) {
                _signalsFlow.value = emptyList()
            }
        }
    }

    override suspend fun recordGeneratedSignal(record: HistoricalSignalRecord): Long {
        return withContext(Dispatchers.IO) {
            try {
                val result = dbHelper?.insertSignal(record) ?: 0L
                _signalsFlow.value = dbHelper?.getAllSignals() ?: emptyList()
                result
            } catch (e: Throwable) {
                0L
            }
        }
    }

    override suspend fun updateSignalLifecycle(record: HistoricalSignalRecord): Int {
        return withContext(Dispatchers.IO) {
            try {
                val result = dbHelper?.updateSignal(record) ?: 0
                _signalsFlow.value = dbHelper?.getAllSignals() ?: emptyList()
                result
            } catch (e: Throwable) {
                0
            }
        }
    }

    override suspend fun getAllSignals(): List<HistoricalSignalRecord> {
        return withContext(Dispatchers.IO) {
            try {
                dbHelper?.getAllSignals() ?: emptyList()
            } catch (e: Throwable) {
                emptyList()
            }
        }
    }

    override suspend fun getSignalsFiltered(
        fromTimestamp: Long,
        optionFilter: OptionFilter,
        strategyFilter: StrategyFilter,
        paperTradesOnly: Boolean?
    ): List<HistoricalSignalRecord> {
        return withContext(Dispatchers.IO) {
            try {
                dbHelper?.getSignalsByFilter(fromTimestamp, optionFilter, strategyFilter, paperTradesOnly) ?: emptyList()
            } catch (e: Throwable) {
                emptyList()
            }
        }
    }

    override suspend fun getOpenSignals(): List<HistoricalSignalRecord> {
        return withContext(Dispatchers.IO) {
            try {
                dbHelper?.getOpenSignals() ?: emptyList()
            } catch (e: Throwable) {
                emptyList()
            }
        }
    }

    override suspend fun getSignalById(id: String): HistoricalSignalRecord? {
        return withContext(Dispatchers.IO) {
            try {
                dbHelper?.getSignalById(id)
            } catch (e: Throwable) {
                null
            }
        }
    }

    override fun computeAccuracyMetrics(signals: List<HistoricalSignalRecord>): AccuracyMetrics {
        val totalSignals = signals.size
        if (totalSignals == 0) {
            return AccuracyMetrics(
                totalSignals = 0,
                totalExecutedTrades = 0,
                winningTrades = 0,
                losingTrades = 0,
                breakevenTrades = 0,
                ambiguousTrades = 0,
                expiredUnfilledSignals = 0,
                cancelledSignals = 0,
                openTrades = 0,
                resolvedTradesCount = 0,
                winRate = 0.0,
                lossRate = 0.0,
                target1HitRate = 0.0,
                target2HitRate = 0.0,
                target3HitRate = 0.0,
                stopLossHitRate = 0.0,
                grossProfit = 0.0,
                grossLoss = 0.0,
                netPnl = 0.0,
                totalChargesAndTaxes = 0.0,
                profitFactor = 0.0,
                averageWin = 0.0,
                averageLoss = 0.0,
                averageWinningTradeNet = 0.0,
                averageLosingTradeNet = 0.0,
                expectancyPerTrade = 0.0,
                maxDrawdownAmount = 0.0,
                maxDrawdownPercent = 0.0,
                averageHoldingTimeMinutes = 0L,
                confidenceInterval95 = Pair(0.0, 0.0),
                statisticalReliabilityLabel = "Insufficient sample",
                sampleSizeWarning = "Sample size (0) is below statistical significance threshold.",
                hasSufficientData = false
            )
        }

        val executedTrades = signals.filter { it.entryMet }
        val executedCount = executedTrades.size

        val wins = executedTrades.filter { it.outcome == TradeOutcome.WIN }
        val losses = executedTrades.filter { it.outcome == TradeOutcome.LOSS }
        val breakevens = executedTrades.filter { it.outcome == TradeOutcome.BREAKEVEN }
        val ambiguous = executedTrades.filter { it.outcome == TradeOutcome.AMBIGUOUS }
        val expired = signals.filter { it.status == SignalLifecycleStatus.EXPIRED || it.exitReason == ExitReason.EXPIRED_UNFILLED }
        val cancelled = signals.filter { it.status == SignalLifecycleStatus.CANCELLED || it.exitReason == ExitReason.CANCELLED }
        val open = signals.filter { it.status == SignalLifecycleStatus.PENDING_ENTRY || it.status == SignalLifecycleStatus.ACTIVE }

        // Section 1 Definition:
        // Resolved trade count for win rate is strictly: Winning trades + Losing trades.
        // Ambiguous, cancelled, expired and unfilled signals are strictly excluded from resolved-trade win rate.
        val resolvedCount = wins.size + losses.size
        val closedTrades = executedTrades.filter { it.status == SignalLifecycleStatus.CLOSED }

        val winRate = if (resolvedCount > 0) (wins.size.toDouble() / resolvedCount.toDouble()) * 100.0 else 0.0
        val lossRate = if (resolvedCount > 0) (losses.size.toDouble() / resolvedCount.toDouble()) * 100.0 else 0.0

        // Target and SL hit rates over resolved trades
        val t1Hits = executedTrades.count { it.exitReason == ExitReason.TARGET_1 }
        val t2Hits = executedTrades.count { it.exitReason == ExitReason.TARGET_2 }
        val t3Hits = executedTrades.count { it.exitReason == ExitReason.TARGET_3 }
        val slHits = executedTrades.count { it.exitReason == ExitReason.STOP_LOSS }

        val t1Rate = if (resolvedCount > 0) (t1Hits.toDouble() / resolvedCount.toDouble()) * 100.0 else 0.0
        val t2Rate = if (resolvedCount > 0) (t2Hits.toDouble() / resolvedCount.toDouble()) * 100.0 else 0.0
        val t3Rate = if (resolvedCount > 0) (t3Hits.toDouble() / resolvedCount.toDouble()) * 100.0 else 0.0
        val slRate = if (resolvedCount > 0) (slHits.toDouble() / resolvedCount.toDouble()) * 100.0 else 0.0

        // Financial metrics (Gross P&L, Charges, Net P&L)
        val grossProfit = wins.sumOf { max(0.0, it.grossPnl) }
        val grossLoss = losses.sumOf { abs(it.grossPnl) }
        val totalCharges = executedTrades.sumOf { it.totalCharges }
        val netPnl = executedTrades.sumOf { it.netPnl }

        // Section 1: Profit factor = Gross winning P&L / absolute gross losing P&L
        val profitFactor = when {
            grossLoss > 0.0 -> round2(grossProfit / grossLoss)
            grossProfit > 0.0 -> 9.99
            else -> 0.0
        }

        val avgWinGross = if (wins.isNotEmpty()) round2(grossProfit / wins.size) else 0.0
        val avgLossGross = if (losses.isNotEmpty()) round2(grossLoss / losses.size) else 0.0

        val avgWinNet = if (wins.isNotEmpty()) round2(wins.sumOf { it.netPnl } / wins.size) else 0.0
        val avgLossNet = if (losses.isNotEmpty()) round2(abs(losses.sumOf { it.netPnl }) / losses.size) else 0.0

        // Section 1: Expectancy = Average net P&L per resolved trade.
        // Formula: Net P&L of resolved trades / resolved trades count
        // Note: exactly reconciles to: (Win Rate * Avg Win Net) - (Loss Rate * Avg Loss Net)
        val resolvedNetPnl = wins.sumOf { it.netPnl } + losses.sumOf { it.netPnl }
        val expectancy = if (resolvedCount > 0) round2(resolvedNetPnl / resolvedCount) else 0.0

        // Section 1: Maximum drawdown calculated from the chronological equity curve
        // Base initial capital = ₹1,00,000
        val baseCapital = 100000.0
        var peakEquity = baseCapital
        var currentEquity = baseCapital
        var maxDdAmount = 0.0
        var maxDdPercent = 0.0

        val chronologicalTrades = closedTrades.sortedBy { it.exitTimestamp ?: it.timestamp }
        for (trade in chronologicalTrades) {
            currentEquity += trade.netPnl
            if (currentEquity > peakEquity) {
                peakEquity = currentEquity
            }
            val ddAmount = peakEquity - currentEquity
            val ddPercent = if (peakEquity > 0) (ddAmount / peakEquity) * 100.0 else 0.0

            if (ddAmount > maxDdAmount) maxDdAmount = ddAmount
            if (ddPercent > maxDdPercent) maxDdPercent = ddPercent
        }

        val avgHoldingTime = if (closedTrades.isNotEmpty()) {
            closedTrades.map { it.holdingTimeMinutes }.average().toLong()
        } else {
            0L
        }

        // Section 3: 95% Wilson Score Confidence Interval for win rate
        val confidenceInterval = calculateWilsonScoreInterval(wins.size, resolvedCount)

        // Section 3: Operational sample-size labels
        val (reliabilityLabel, sampleWarning) = when {
            resolvedCount < 30 -> Pair(
                "Insufficient sample",
                "Sample size ($resolvedCount resolved) is below statistical significance threshold (N ≥ 30). Empirical win rate and expectancy may represent random distribution."
            )
            resolvedCount in 30..99 -> Pair(
                "Preliminary evidence",
                null
            )
            resolvedCount in 100..299 -> Pair(
                "Developing evidence",
                null
            )
            else -> Pair(
                "Larger sample, still subject to validation",
                null
            )
        }

        return AccuracyMetrics(
            totalSignals = totalSignals,
            totalExecutedTrades = executedCount,
            winningTrades = wins.size,
            losingTrades = losses.size,
            breakevenTrades = breakevens.size,
            ambiguousTrades = ambiguous.size,
            expiredUnfilledSignals = expired.size,
            cancelledSignals = cancelled.size,
            openTrades = open.size,
            resolvedTradesCount = resolvedCount,
            winRate = round1(winRate),
            lossRate = round1(lossRate),
            target1HitRate = round1(t1Rate),
            target2HitRate = round1(t2Rate),
            target3HitRate = round1(t3Rate),
            stopLossHitRate = round1(slRate),
            grossProfit = round2(grossProfit),
            grossLoss = round2(grossLoss),
            netPnl = round2(netPnl),
            totalChargesAndTaxes = round2(totalCharges),
            profitFactor = profitFactor,
            averageWin = avgWinGross,
            averageLoss = avgLossGross,
            averageWinningTradeNet = avgWinNet,
            averageLosingTradeNet = avgLossNet,
            expectancyPerTrade = expectancy,
            maxDrawdownAmount = round2(maxDdAmount),
            maxDrawdownPercent = round1(maxDdPercent),
            averageHoldingTimeMinutes = avgHoldingTime,
            confidenceInterval95 = confidenceInterval,
            statisticalReliabilityLabel = reliabilityLabel,
            sampleSizeWarning = sampleWarning,
            hasSufficientData = resolvedCount >= 30
        )
    }

    /**
     * Calculates 95% Wilson Score confidence interval for a binomial proportion.
     */
    private fun calculateWilsonScoreInterval(wins: Int, total: Int, z: Double = 1.95996): Pair<Double, Double> {
        if (total <= 0) return Pair(0.0, 0.0)
        val p = wins.toDouble() / total.toDouble()
        val z2 = z * z
        val denom = 1.0 + z2 / total
        val center = (p + z2 / (2.0 * total)) / denom
        val margin = (z * sqrt((p * (1.0 - p) + z2 / (4.0 * total)) / total)) / denom
        val lower = max(0.0, center - margin) * 100.0
        val upper = min(1.0, center + margin) * 100.0
        return Pair(round1(lower), round1(upper))
    }

    private fun round1(value: Double): Double = Math.round(value * 10.0) / 10.0
    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0
}
