package com.cloudscope.moneyprinter.domain.paper

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.data.repository.SignalRepository
import com.cloudscope.moneyprinter.domain.signal.ChargesCalculator
import com.cloudscope.moneyprinter.domain.signal.SignalOutcomeTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class PaperTradingManager(
    private val signalRepository: SignalRepository
) {
    private val initialCapital = 100000.0

    private val _accountState = MutableStateFlow(
        PaperTradingAccount(
            initialBalance = initialCapital,
            currentBalance = initialCapital,
            activePositionsCount = 0,
            closedPositionsCount = 0,
            totalNetPnl = 0.0,
            winRate = 0.0
        )
    )
    val accountState: StateFlow<PaperTradingAccount> = _accountState.asStateFlow()

    suspend fun refreshAccountState() {
        val paperTrades = signalRepository.getSignalsFiltered(paperTradesOnly = true)
        val activeTrades = paperTrades.filter { it.status == SignalLifecycleStatus.ACTIVE || it.status == SignalLifecycleStatus.PENDING_ENTRY }
        val closedTrades = paperTrades.filter { it.status == SignalLifecycleStatus.CLOSED }

        val totalNetPnl = closedTrades.sumOf { it.netPnl }
        val winningTrades = closedTrades.count { it.outcome == TradeOutcome.WIN }
        val winRate = if (closedTrades.isNotEmpty()) (winningTrades.toDouble() / closedTrades.size) * 100.0 else 0.0

        val currentBalance = initialCapital + totalNetPnl

        _accountState.value = PaperTradingAccount(
            initialBalance = initialCapital,
            currentBalance = Math.round(currentBalance * 100.0) / 100.0,
            activePositionsCount = activeTrades.size,
            closedPositionsCount = closedTrades.size,
            totalNetPnl = Math.round(totalNetPnl * 100.0) / 100.0,
            winRate = Math.round(winRate * 10.0) / 10.0
        )
    }

    /**
     * Executes a paper trade with realistic slippage (+0.5 pts on entry) and records it.
     */
    suspend fun executePaperTrade(
        signal: TradingSignal,
        indicators: TechnicalIndicators
    ): HistoricalSignalRecord? {
        val lots = if (signal.lots > 0) signal.lots else 1
        val lotSize = if (signal.lotSize > 0) signal.lotSize else com.cloudscope.moneyprinter.domain.options.ContractSpecificationService.getLotSize("NIFTY", signal.expiryDate)
        val slippage = 0.5
        val executedEntryPrice = signal.entryPrice + slippage
        val capitalRequired = executedEntryPrice * lots * lotSize

        if (_accountState.value.currentBalance < capitalRequired) {
            return null // Insufficient virtual balance
        }

        val baseRecord = SignalOutcomeTracker.createRecordFromSignal(signal, indicators, isPaperTrade = true)
        val paperRecord = baseRecord.copy(
            id = "PAPER-${UUID.randomUUID().toString().take(8)}",
            entryPrice = executedEntryPrice,
            entryMet = true,
            entryTimestamp = System.currentTimeMillis(),
            status = SignalLifecycleStatus.ACTIVE,
            lots = lots,
            lotSize = lotSize,
            adjustmentsLog = listOf("Paper Trade executed at ₹$executedEntryPrice (incl. 0.5pt slippage) with $lots lot(s) = ${lots * lotSize} Qty")
        )

        signalRepository.recordGeneratedSignal(paperRecord)
        refreshAccountState()
        return paperRecord
    }

    /**
     * Closes an active paper position with slippage (-0.5 pts) and statutory costs
     */
    suspend fun closePaperPosition(
        record: HistoricalSignalRecord,
        exitOptionPrice: Double,
        reason: ExitReason
    ): HistoricalSignalRecord {
        val lots = if (record.lots > 0) record.lots else 1
        val lotSize = if (record.lotSize > 0) record.lotSize else com.cloudscope.moneyprinter.domain.options.ContractSpecificationService.getLotSize("NIFTY", record.expiryDate)
        val slippage = 0.5
        val executedExitPrice = kotlin.math.max(0.05, exitOptionPrice - slippage)

        val (gross, net) = ChargesCalculator.calculateNetPnl(record.entryPrice, executedExitPrice, lots, lotSize)
        val charges = ChargesCalculator.calculate(record.entryPrice, executedExitPrice, lots, lotSize)

        val outcome = when {
            net > 0 -> TradeOutcome.WIN
            net < 0 -> TradeOutcome.LOSS
            else -> TradeOutcome.BREAKEVEN
        }

        val updatedRecord = record.copy(
            actualExitPrice = executedExitPrice,
            exitTimestamp = System.currentTimeMillis(),
            exitReason = reason,
            outcome = outcome,
            grossPnl = gross,
            netPnl = net,
            totalCharges = charges.totalCharges,
            status = SignalLifecycleStatus.CLOSED,
            holdingTimeMinutes = kotlin.math.max(1L, (System.currentTimeMillis() - (record.entryTimestamp ?: record.timestamp)) / 60000L),
            adjustmentsLog = record.adjustmentsLog + listOf("Paper trade closed at ₹$executedExitPrice via $reason. Net PnL: ₹$net")
        )

        signalRepository.updateSignalLifecycle(updatedRecord)
        refreshAccountState()
        return updatedRecord
    }

    /**
     * Resets virtual paper trading account to ₹1,00,000 initial capital
     */
    suspend fun resetAccount() {
        _accountState.value = PaperTradingAccount(
            initialBalance = initialCapital,
            currentBalance = initialCapital,
            activePositionsCount = 0,
            closedPositionsCount = 0,
            totalNetPnl = 0.0,
            winRate = 0.0
        )
    }
}
