package com.cloudscope.moneyprinter.data.repository

import com.cloudscope.moneyprinter.data.local.db.SignalDatabaseHelper
import com.cloudscope.moneyprinter.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.max

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
    fun computeAccuracyMetrics(signals: List<HistoricalSignalRecord>): AccuracyMetrics
}

class SignalRepositoryImpl(
    private val dbHelper: SignalDatabaseHelper
) : SignalRepository {

    private val _signalsFlow = MutableStateFlow<List<HistoricalSignalRecord>>(emptyList())
    override val signalsFlow: StateFlow<List<HistoricalSignalRecord>> = _signalsFlow.asStateFlow()

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            val list = dbHelper.getAllSignals()
            _signalsFlow.value = list
        }
    }

    override suspend fun recordGeneratedSignal(record: HistoricalSignalRecord): Long {
        return withContext(Dispatchers.IO) {
            val result = dbHelper.insertSignal(record)
            _signalsFlow.value = dbHelper.getAllSignals()
            result
        }
    }

    override suspend fun updateSignalLifecycle(record: HistoricalSignalRecord): Int {
        return withContext(Dispatchers.IO) {
            val result = dbHelper.updateSignal(record)
            _signalsFlow.value = dbHelper.getAllSignals()
            result
        }
    }

    override suspend fun getAllSignals(): List<HistoricalSignalRecord> {
        return withContext(Dispatchers.IO) {
            dbHelper.getAllSignals()
        }
    }

    override suspend fun getSignalsFiltered(
        fromTimestamp: Long,
        optionFilter: OptionFilter,
        strategyFilter: StrategyFilter,
        paperTradesOnly: Boolean?
    ): List<HistoricalSignalRecord> {
        return withContext(Dispatchers.IO) {
            dbHelper.getSignalsByFilter(fromTimestamp, optionFilter, strategyFilter, paperTradesOnly)
        }
    }

    override suspend fun getOpenSignals(): List<HistoricalSignalRecord> {
        return withContext(Dispatchers.IO) {
            dbHelper.getOpenSignals()
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
                expectancyPerTrade = 0.0,
                maxDrawdownPercent = 0.0,
                averageHoldingTimeMinutes = 0L,
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

        val t1Hits = executedTrades.filter { it.exitReason == ExitReason.TARGET_1 }.size
        val t2Hits = executedTrades.filter { it.exitReason == ExitReason.TARGET_2 }.size
        val t3Hits = executedTrades.filter { it.exitReason == ExitReason.TARGET_3 }.size
        val slHits = executedTrades.filter { it.exitReason == ExitReason.STOP_LOSS }.size

        val closedTrades = executedTrades.filter { it.status == SignalLifecycleStatus.CLOSED }
        val closedCount = closedTrades.size

        val winRate = if (closedCount > 0) (wins.size.toDouble() / closedCount.toDouble()) * 100.0 else 0.0
        val lossRate = if (closedCount > 0) (losses.size.toDouble() / closedCount.toDouble()) * 100.0 else 0.0
        val t1Rate = if (closedCount > 0) (t1Hits.toDouble() / closedCount.toDouble()) * 100.0 else 0.0
        val t2Rate = if (closedCount > 0) (t2Hits.toDouble() / closedCount.toDouble()) * 100.0 else 0.0
        val t3Rate = if (closedCount > 0) (t3Hits.toDouble() / closedCount.toDouble()) * 100.0 else 0.0
        val slRate = if (closedCount > 0) (slHits.toDouble() / closedCount.toDouble()) * 100.0 else 0.0

        val grossProfit = wins.sumOf { max(0.0, it.grossPnl) }
        val grossLoss = losses.sumOf { kotlin.math.abs(it.grossPnl) }
        val totalCharges = executedTrades.sumOf { it.totalCharges }
        val netPnl = executedTrades.sumOf { it.netPnl }

        val profitFactor = if (grossLoss > 0.0) grossProfit / grossLoss else if (grossProfit > 0) 9.99 else 0.0
        val avgWin = if (wins.isNotEmpty()) grossProfit / wins.size else 0.0
        val avgLoss = if (losses.isNotEmpty()) grossLoss / losses.size else 0.0

        val avgChargesPerTrade = if (closedCount > 0) totalCharges / closedCount else 0.0
        val expectancy = if (closedCount > 0) {
            val winProb = wins.size.toDouble() / closedCount.toDouble()
            val lossProb = losses.size.toDouble() / closedCount.toDouble()
            (winProb * avgWin) - (lossProb * avgLoss) - avgChargesPerTrade
        } else {
            0.0
        }

        // Drawdown calculation
        var peak = 0.0
        var cumulative = 0.0
        var maxDd = 0.0
        for (trade in closedTrades.sortedBy { it.timestamp }) {
            cumulative += trade.netPnl
            if (cumulative > peak) peak = cumulative
            val dd = peak - cumulative
            if (dd > maxDd) maxDd = dd
        }
        val maxDdPercent = if (peak > 0) (maxDd / peak) * 100.0 else 0.0

        val avgHoldingTime = if (closedCount > 0) {
            closedTrades.map { it.holdingTimeMinutes }.average().toLong()
        } else {
            0L
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
            winRate = winRate,
            lossRate = lossRate,
            target1HitRate = t1Rate,
            target2HitRate = t2Rate,
            target3HitRate = t3Rate,
            stopLossHitRate = slRate,
            grossProfit = grossProfit,
            grossLoss = grossLoss,
            netPnl = netPnl,
            totalChargesAndTaxes = totalCharges,
            profitFactor = profitFactor,
            averageWin = avgWin,
            averageLoss = avgLoss,
            expectancyPerTrade = expectancy,
            maxDrawdownPercent = maxDdPercent,
            averageHoldingTimeMinutes = avgHoldingTime,
            hasSufficientData = closedCount >= 5
        )
    }
}
