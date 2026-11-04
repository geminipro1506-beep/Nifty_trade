package com.cloudscope.moneyprinter.domain.smartmoney

import com.cloudscope.moneyprinter.data.model.OptionType

enum class SmartMoneySignalAction(val displayName: String) {
    BUY_CE("BUY CE"),
    BUY_PE("BUY PE"),
    NO_TRADE("NO TRADE")
}

enum class SmartMoneyConfidence(val displayName: String) {
    STRONG("Strong"),
    MODERATE("Moderate"),
    LOW("Low")
}

enum class PriceOiRegime(val displayName: String, val interpretation: String) {
    LONG_BUILDUP("Long Buildup", "Fresh institutional long accumulation (Price UP + OI UP)"),
    SHORT_BUILDUP("Short Buildup", "Aggressive institutional short writing/selling (Price DOWN + OI UP)"),
    SHORT_COVERING("Short Covering", "Bears covering short positions (Price UP + OI DOWN)"),
    LONG_UNWINDING("Long Unwinding", "Bulls unwinding long inventory (Price DOWN + OI DOWN)"),
    NEUTRAL("Neutral / Flat OI", "No significant institutional positioning shift")
}

enum class MarketStructureState(val displayName: String) {
    BULLISH_STRUCTURE("Bullish Structure (HH/HL / Bullish BOS)"),
    BEARISH_STRUCTURE("Bearish Structure (LH/LL / Bearish BOS)"),
    RANGEBOUND("Rangebound Consolidation")
}

enum class VwapRelationship(val displayName: String) {
    RECLAIM("VWAP Reclaim (Bullish Absorption)"),
    REJECTION("VWAP Rejection (Bearish Supply)"),
    ABOVE_VWAP("Holding Above VWAP"),
    BELOW_VWAP("Holding Below VWAP"),
    NEUTRAL("At VWAP Axis")
}

enum class LiquiditySweepState(val displayName: String) {
    BULLISH_SWEEP("Bullish Liquidity Sweep (SSL Stop Hunt & Reversal)"),
    BEARISH_SWEEP("Bearish Liquidity Sweep (BSL Buy Trap & Reversal)"),
    NONE("No Sweep Detected")
}

enum class OptionBuildupType(val displayName: String, val marketBias: String) {
    LONG_BUILDUP("Long Buildup", "Premium buying pressure"),
    SHORT_BUILDUP("Short Buildup", "Writing / Seller dominance"),
    SHORT_COVERING("Short Covering", "Sellers closing / Short squeeze"),
    LONG_UNWINDING("Long Unwinding", "Buyers liquidating positions"),
    NEUTRAL("Neutral", "Indecisive OI change")
}

data class SmartMoneyActivity(
    val spotPrice: Double,
    val spotVolume: Long,
    val isUnusualVolume: Boolean,
    val volumeRatio: Double,
    val vwap: Double,
    val vwapState: VwapRelationship,
    val liquiditySweep: LiquiditySweepState,
    val marketStructure: MarketStructureState,
    val futuresPrice: Double,
    val futuresOi: Long,
    val futuresOiChange: Long,
    val futuresOiChangePercent: Double,
    val priceOiRegime: PriceOiRegime,
    val summary: String
)

data class OptionConfirmationDetails(
    val isAvailable: Boolean,
    val pcr: Double,
    val atmStrike: Double,
    val atmCeBuildup: OptionBuildupType,
    val atmPeBuildup: OptionBuildupType,
    val atmCeVolume: Long,
    val atmPeVolume: Long,
    val atmCeChange: Double,
    val atmPeChange: Double,
    val atmCeBidAskSpread: Double,
    val atmPeBidAskSpread: Double,
    val isBidAskAcceptable: Boolean,
    val isBullishConfirmed: Boolean,
    val isBearishConfirmed: Boolean,
    val confirmationReason: String
)

data class SmartMoneySignal(
    val action: SmartMoneySignalAction,
    val confidence: SmartMoneyConfidence,
    val suggestedStrike: String,
    val strikePrice: Double = 0.0,
    val optionType: OptionType? = null,
    val entryZone: String,
    val entryPrice: Double = 0.0,
    val stopLoss: String,
    val stopLossValue: Double = 0.0,
    val target1: String,
    val target1Value: Double = 0.0,
    val target2: String,
    val target2Value: Double = 0.0,
    val shortReason: String,
    val riskRewardRatio: Double = 0.0,
    val netRiskRewardRatio: Double = 0.0,
    val estimatedCharges: Double = 55.0,
    val timestamp: Long = System.currentTimeMillis(),
    val optionConfirmationAvailable: Boolean = true,
    val smartMoneyActivity: SmartMoneyActivity? = null,
    val optionConfirmationDetails: OptionConfirmationDetails? = null
)
