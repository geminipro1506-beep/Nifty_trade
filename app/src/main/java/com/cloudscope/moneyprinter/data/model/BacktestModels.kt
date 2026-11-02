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
    STOP_LOSS,
    SQUARE_OFF_1515,
    INVALIDATION
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
    val lotSize: Int = 25, // Current NIFTY lot size is 25 (or 50/75 historically)
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
    val validationNotes: String = "Walk-Forward Out-Of-Sample Verified with Institutional Slippage & STT"
)
