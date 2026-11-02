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
    val adjustmentsLog: List<String> = emptyList()
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
    val winRate: Double,
    val lossRate: Double,
    val target1HitRate: Double,
    val target2HitRate: Double,
    val target3HitRate: Double,
    val stopLossHitRate: Double,
    val grossProfit: Double,
    val grossLoss: Double,
    val netPnl: Double,
    val totalChargesAndTaxes: Double,
    val profitFactor: Double,
    val averageWin: Double,
    val averageLoss: Double,
    val expectancyPerTrade: Double,
    val maxDrawdownPercent: Double,
    val averageHoldingTimeMinutes: Long,
    val hasSufficientData: Boolean
)

data class StrategyPerformanceComparison(
    val strategyName: String,
    val version: String,
    val totalTrades: Int,
    val winRate: Double,
    val profitFactor: Double,
    val netPnl: Double,
    val maxDrawdown: Double,
    val status: String
)

data class StrategyImprovementInsight(
    val category: String,
    val observation: String,
    val recommendation: String,
    val statisticalEdge: String
)

data class PaperTradingAccount(
    val initialBalance: Double = 100000.0,
    val currentBalance: Double = 100000.0,
    val activePositionsCount: Int = 0,
    val closedPositionsCount: Int = 0,
    val totalNetPnl: Double = 0.0,
    val winRate: Double = 0.0
)
