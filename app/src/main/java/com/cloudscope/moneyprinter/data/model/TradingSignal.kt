package com.cloudscope.moneyprinter.data.model

enum class MarketDirection {
    BULLISH,
    BEARISH,
    SIDEWAYS
}

enum class SignalAction {
    BUY_CE,
    BUY_PE,
    NO_TRADE
}

enum class SignalStatus {
    ACTIVE,
    TARGET_1_HIT,
    TARGET_2_HIT,
    STOP_LOSS_HIT,
    INACTIVE,
    CLOSED
}

enum class ConfidenceRating {
    HIGH,
    MEDIUM,
    LOW
}

enum class OrbState {
    BREAKOUT_UP,
    BREAKOUT_DOWN,
    INSIDE_RANGE
}

data class TechnicalIndicators(
    val rsi14: Double,
    val ema20: Double,
    val ema50: Double,
    val ema200: Double,
    val vwap: Double,
    val macdLine: Double,
    val signalLine: Double,
    val macdHistogram: Double,
    val atr14: Double,
    val pdh: Double,
    val pdl: Double,
    val pdc: Double,
    val orbHigh: Double,
    val orbLow: Double,
    val orbState: OrbState,
    val pcr: Double,
    val vix: Double,
    val marketDirection: MarketDirection
)

data class SupportResistanceLevels(
    val pivot: Double,
    val r1: Double,
    val r2: Double,
    val s1: Double,
    val s2: Double,
    val pdh: Double,
    val pdl: Double
)

data class TradingSignal(
    val action: SignalAction,
    val underlyingSpot: Double,
    val recommendedStrike: Double,
    val optionType: OptionType,
    val contractSymbol: String,
    val entryPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val riskRewardRatio: Double,
    val confidencePercent: Int,
    val confidenceRating: ConfidenceRating,
    val status: SignalStatus,
    val marketDirection: MarketDirection,
    val summaryFactors: String,
    val confirmationReasons: List<String>,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun noTrade(reason: String = "Market conditions are not favourable.") = TradingSignal(
            action = SignalAction.NO_TRADE,
            underlyingSpot = 0.0,
            recommendedStrike = 0.0,
            optionType = OptionType.CE,
            contractSymbol = "",
            entryPrice = 0.0,
            stopLoss = 0.0,
            target1 = 0.0,
            target2 = 0.0,
            riskRewardRatio = 0.0,
            confidencePercent = 0,
            confidenceRating = ConfidenceRating.LOW,
            status = SignalStatus.INACTIVE,
            marketDirection = MarketDirection.SIDEWAYS,
            summaryFactors = "No High-Probability Setup",
            confirmationReasons = listOf(reason)
        )
    }
}
