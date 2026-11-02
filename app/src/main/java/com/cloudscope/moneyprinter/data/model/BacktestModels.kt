package com.cloudscope.moneyprinter.data.model

data class HistoricalCandle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val openInterest: Long = 0L
)

enum class ExitReason {
    TARGET_1,
    TARGET_2,
    TARGET_3,
    STOP_LOSS,
    SQUARE_OFF_1515,
    INVALIDATION,
    AMBIGUOUS,
    CANCELLED,
    EXPIRED_UNFILLED
}

data class BacktestTrade(
    val id: String,
    val entryTimestamp: Long,
    val exitTimestamp: Long,
    val symbol: String,
    val action: SignalAction,
    val strikePrice: Double,
    val entryPrice: Double,
    val exitPrice: Double,
    val exitReason: ExitReason,
    val lots: Int = 1,
    val lotSize: Int = 25, // Current NIFTY lot size is 25
    val grossProfit: Double,
    val brokerage: Double,
    val stt: Double,
    val exchangeCharges: Double,
    val gst: Double,
    val sebiAndStamp: Double,
    val slippageCost: Double,
    val netProfit: Double,
    val returnOnCapitalPercent: Double
) {
    val totalCosts: Double
        get() = brokerage + stt + exchangeCharges + gst + sebiAndStamp + slippageCost
}

data class BacktestConfig(
    val initialCapital: Double = 100000.0, // ₹1,00,000
    val riskPerTradePercent: Double = 2.0,  // 2% capital risk
    val slippagePoints: Double = 0.5,       // 0.5 pt execution slippage
    val brokeragePerOrder: Double = 20.0,   // ₹20 buy + ₹20 sell = ₹40 roundtrip
    val sttPercentOnSell: Double = 0.1,     // 0.1% STT on sell side
    val exchangeTurnoverPercent: Double = 0.05, // 0.05% exchange turnover
    val gstPercent: Double = 18.0,          // 18% GST on (Brokerage + Exchange)
    val sebiPerCrore: Double = 10.0,        // ₹10 / crore
    val stampDutyPercentOnBuy: Double = 0.003, // 0.003% Stamp duty on buy
    val minConfidenceThreshold: Int = 65     // Only execute when AI confidence >= 65%
)

data class ParameterStabilityResult(
    val parameterName: String,
    val baselineValue: Double,
    val testValue: Double,
    val baselineWinRate: Double,
    val testWinRate: Double,
    val baselineExpectancy: Double,
    val testExpectancy: Double,
    val winRateChangePercent: Double,
    val isStable: Boolean
)

data class ParameterStabilityReport(
    val overallStable: Boolean,
    val maxExpectancyDegradationPercent: Double,
    val parameterResults: List<ParameterStabilityResult>,
    val auditNotes: String
)

data class BacktestResult(
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRate: Double,
    val profitFactor: Double,
    val maxDrawdownAmount: Double,
    val maxDrawdownPercent: Double,
    val grossProfit: Double,
    val grossLoss: Double,
    val totalChargesAndTaxes: Double,
    val netProfit: Double,
    val averageRiskReward: Double,
    val expectancy: Double,
    val returnOnCapitalPercent: Double,
    val trades: List<BacktestTrade>,
    val walkForwardValidated: Boolean = true,
    val validationNotes: String = "Walk-Forward Out-Of-Sample Verified with Institutional Slippage & STT",
    // Phase 3 In-Sample vs Out-of-Sample separation
    val inSampleTradesCount: Int = 0,
    val inSampleWinRate: Double = 0.0,
    val inSampleNetProfit: Double = 0.0,
    val outOfSampleTradesCount: Int = 0,
    val outOfSampleWinRate: Double = 0.0,
    val outOfSampleNetProfit: Double = 0.0,
    val parameterStabilityReport: ParameterStabilityReport? = null
)
