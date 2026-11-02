package com.cloudscope.moneyprinter.data.model

enum class TradeOutcome {
    WIN,
    LOSS,
    BREAKEVEN,
    AMBIGUOUS,
    UNFILLED,
    CANCELLED
}

enum class SignalLifecycleStatus {
    PENDING_ENTRY,
    ACTIVE,
    CLOSED,
    CANCELLED,
    EXPIRED
}

enum class AccuracyPeriod(val label: String, val days: Int) {
    TODAY("Today", 1),
    LAST_7_DAYS("7 Days", 7),
    LAST_30_DAYS("30 Days", 30),
    LAST_90_DAYS("90 Days", 90),
    ALL_TIME("All Time", 365)
}

enum class StrategyFilter(val label: String) {
    ALL("All Strategies"),
    BREAKOUT("Breakout"),
    REVERSAL("Reversal"),
    TREND_FOLLOWING("Trend Following")
}

enum class OptionFilter(val label: String) {
    ALL("All (CE & PE)"),
    CE_ONLY("Calls (CE) Only"),
    PE_ONLY("Puts (PE) Only")
}

data class HistoricalSignalRecord(
    val id: String,
    val timestamp: Long,
    val spotPrice: Double,
    val marketDirection: MarketDirection,
    val action: SignalAction,
    val contractSymbol: String,
    val strikePrice: Double,
    val optionType: OptionType,
    val expiryDate: String,
    val premiumAtSignal: Double,
    val entryPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val target3: Double,
    val confidenceScore: Int,
    val rsi14: Double,
    val vwap: Double,
    val ema9: Double,
    val ema20: Double,
    val ema50: Double,
    val pcr: Double,
    val vix: Double,
    val reasons: List<String>,
    val invalidationConditions: String,
    val entryMet: Boolean = false,
    val entryTimestamp: Long? = null,
    val actualExitPrice: Double? = null,
    val exitTimestamp: Long? = null,
    val exitReason: ExitReason? = null,
    val status: SignalLifecycleStatus = SignalLifecycleStatus.PENDING_ENTRY,
    val outcome: TradeOutcome? = null,
    val grossPnl: Double = 0.0,
    val netPnl: Double = 0.0,
    val totalCharges: Double = 0.0,
    val holdingTimeMinutes: Long = 0,
    val strategyType: StrategyType = StrategyType.TREND_FOLLOWING,
    val timeframe: String = "5m",
    val isPaperTrade: Boolean = false,
    val adjustmentsLog: List<String> = emptyList(),
    // Phase 3 Ambiguous Sequence & Audit fields
    val ambiguousReason: String? = null,
    val candleResolution: String = "5m",
    val targetAndSlSameCandle: Boolean = false,
    val finalResolutionStatus: String? = null
)

data class AccuracyMetrics(
    val totalSignals: Int,
    val totalExecutedTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val breakevenTrades: Int,
    val ambiguousTrades: Int,
    val expiredUnfilledSignals: Int,
    val cancelledSignals: Int,
    val openTrades: Int,
    val resolvedTradesCount: Int, // strictly winningTrades + losingTrades (unambiguous)
    val winRate: Double, // winningTrades / (winningTrades + losingTrades) * 100.0
    val lossRate: Double, // losingTrades / (winningTrades + losingTrades) * 100.0
    val target1HitRate: Double,
    val target2HitRate: Double,
    val target3HitRate: Double,
    val stopLossHitRate: Double,
    val grossProfit: Double,
    val grossLoss: Double,
    val netPnl: Double,
    val totalChargesAndTaxes: Double,
    val profitFactor: Double, // grossProfit / absolute grossLoss
    val averageWin: Double, // gross average win
    val averageLoss: Double, // gross average loss
    val averageWinningTradeNet: Double = 0.0,
    val averageLosingTradeNet: Double = 0.0,
    val expectancyPerTrade: Double, // Net PnL of resolved trades / resolved trades count
    val maxDrawdownAmount: Double = 0.0,
    val maxDrawdownPercent: Double = 0.0,
    val averageHoldingTimeMinutes: Long = 0L,
    val confidenceInterval95: Pair<Double, Double> = Pair(0.0, 0.0),
    val statisticalReliabilityLabel: String = "Insufficient sample",
    val sampleSizeWarning: String? = null,
    val hasSufficientData: Boolean = false
)

data class StrategyPerformanceComparison(
    val strategyName: String,
    val version: String,
    val totalTrades: Int,
    val winRate: Double,
    val profitFactor: Double,
    val netPnl: Double,
    val maxDrawdown: Double,
    val status: String,
    // Phase 3 Backtest vs Out-of-sample vs Paper separation & Live Validation criteria
    val backtestTrades: Int = 0,
    val backtestWinRate: Double = 0.0,
    val backtestNetPnl: Double = 0.0,
    val outOfSampleTrades: Int = 0,
    val outOfSampleWinRate: Double = 0.0,
    val outOfSampleNetPnl: Double = 0.0,
    val forwardPaperTrades: Int = 0,
    val forwardPaperWinRate: Double = 0.0,
    val forwardPaperNetPnl: Double = 0.0,
    val netExpectancy: Double = 0.0,
    val confidenceInterval95: Pair<Double, Double> = Pair(0.0, 0.0),
    val isLiveValidated: Boolean = false,
    val validationNotes: String = ""
)

data class StrategyImprovementInsight(
    val category: String,
    val observation: String,
    val recommendation: String,
    val statisticalEdge: String
)

data class StrategyCohortMetric(
    val cohortName: String,
    val category: String,
    val sampleSize: Int,
    val resolvedTrades: Int,
    val winRate: Double,
    val confidenceIntervalLower: Double,
    val confidenceIntervalUpper: Double,
    val profitFactor: Double,
    val netExpectancy: Double,
    val netPnl: Double,
    val maxDrawdown: Double,
    val reliabilityLabel: String,
    val isPromoted: Boolean,
    val promotionRationale: String
)

data class TradeAuditRecord(
    val tradeId: String,
    val symbol: String,
    val grossPnl: Double,
    val totalCharges: Double,
    val netPnl: Double,
    val expectedNetPnl: Double,
    val isReconciled: Boolean,
    val discrepancy: Double,
    val outcome: TradeOutcome?,
    val isAmbiguous: Boolean,
    val notes: String
)

data class ReconciliationReport(
    val totalTradesAudited: Int,
    val resolvedTradesCount: Int,
    val totalGrossPnl: Double,
    val totalCharges: Double,
    val totalNetPnl: Double,
    val calculatedNetPnl: Double,
    val netPnlDiscrepancy: Double,
    val isFullyReconciled: Boolean,
    val ambiguousTradesCount: Int,
    val excludedTradesCount: Int,
    val tradeAudits: List<TradeAuditRecord>,
    val auditSummary: String,
    val auditTimestamp: Long = System.currentTimeMillis()
)

data class PaperTradingAccount(
    val initialBalance: Double = 100000.0,
    val currentBalance: Double = 100000.0,
    val activePositionsCount: Int = 0,
    val closedPositionsCount: Int = 0,
    val totalNetPnl: Double = 0.0,
    val winRate: Double = 0.0
)
