package com.cloudscope.moneyprinter.data.model

enum class MarketDirection {
    BULLISH,
    BEARISH,
    SIDEWAYS
}

enum class SignalAction {
    BUY_CE,
    BUY_PE,
    WAIT,
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

enum class StrategyType(val displayName: String) {
    BREAKOUT("15m ORB Breakout"),
    REVERSAL("VWAP / RSI Mean Reversion"),
    TREND_FOLLOWING("EMA 9/20/50 Momentum Trend"),
    CONSOLIDATION("Rangebound / Sideways")
}

enum class SetupQuality(val label: String, val rating: String) {
    A_PLUS("A+ (Elite Confluence)", "A+"),
    A("A (High Probability)", "A"),
    B("B (Moderate Confluence)", "B"),
    C("C (Low Edge)", "C")
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
    val marketDirection: MarketDirection,
    val ema9: Double = 0.0
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
    val timestamp: Long = System.currentTimeMillis(),
    val strategyType: StrategyType = StrategyType.TREND_FOLLOWING,
    val setupQuality: String = "A+",
    val expiryDate: String = "Current Weekly (07 NOV)",
    val lots: Int = 1,
    val lotSize: Int = 25,
    val capitalRequired: Double = 0.0,
    val estimatedCharges: Double = 55.0,
    val waitReason: String? = null
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
            confirmationReasons = listOf(reason),
            strategyType = StrategyType.CONSOLIDATION,
            setupQuality = "N/A",
            capitalRequired = 0.0
        )

        fun wait(
            reason: String,
            spot: Double = 0.0,
            direction: MarketDirection = MarketDirection.SIDEWAYS,
            confidence: Int = 45,
            factors: String = "Awaiting Multi-Factor Confirmation"
        ) = TradingSignal(
            action = SignalAction.WAIT,
            underlyingSpot = spot,
            recommendedStrike = 0.0,
            optionType = if (direction == MarketDirection.BULLISH) OptionType.CE else OptionType.PE,
            contractSymbol = "WAITING FOR CONFIRMATION",
            entryPrice = 0.0,
            stopLoss = 0.0,
            target1 = 0.0,
            target2 = 0.0,
            riskRewardRatio = 0.0,
            confidencePercent = confidence,
            confidenceRating = ConfidenceRating.LOW,
            status = SignalStatus.INACTIVE,
            marketDirection = direction,
            summaryFactors = factors,
            confirmationReasons = listOf(reason),
            strategyType = StrategyType.TREND_FOLLOWING,
            setupQuality = "Pending",
            waitReason = reason,
            capitalRequired = 0.0
        )
    }
}
