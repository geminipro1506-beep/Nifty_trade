package com.cloudscope.moneyprinter.domain.normaloptions

import com.cloudscope.moneyprinter.data.model.OptionType

enum class NormalSignalAction(val displayName: String) {
    BUY_CALL("BUY CALL"),
    BUY_PUT("BUY PUT"),
    NO_TRADE("NO TRADE"),
    WAITING_FOR_CONFIRMATION("WAITING FOR CONFIRMATION"),
    WAITING_FOR_DATA("WAITING FOR DATA")
}

data class TechnicalConfluence(
    val ema9: Double,
    val ema21: Double,
    val isEmaBullish: Boolean,
    val isEmaBearish: Boolean,
    val rsi14: Double,
    val isRsiBullish: Boolean,
    val isRsiBearish: Boolean,
    val macdHistogram: Double,
    val isMacdBullish: Boolean,
    val isMacdBearish: Boolean,
    val vwap: Double,
    val isVwapBullish: Boolean,
    val isVwapBearish: Boolean,
    val supertrendValue: Double,
    val isSupertrendBullish: Boolean,
    val isSupertrendBearish: Boolean,
    val currentVolume: Long,
    val averageVolume: Long,
    val isVolumeConfirmed: Boolean,
    val isBreakoutBullish: Boolean,
    val isBreakdownBearish: Boolean,
    val breakoutLevel: String
)

data class NormalOptionsSignal(
    val action: NormalSignalAction,
    val confidencePercent: Int,
    val suggestedStrike: Double,
    val strikeDisplay: String,
    val optionType: OptionType,
    val expiryDate: String,
    val entryPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val riskRewardRatio: String,
    val signalReason: String,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val confluence: TechnicalConfluence? = null,
    val underlyingSpot: Double = 0.0,
    val isStale: Boolean = false
) {
    companion object {
        fun waitingForData(reason: String = "Waiting for live market data and candle feed.") = NormalOptionsSignal(
            action = NormalSignalAction.WAITING_FOR_DATA,
            confidencePercent = 0,
            suggestedStrike = 0.0,
            strikeDisplay = "—",
            optionType = OptionType.CE,
            expiryDate = "—",
            entryPrice = 0.0,
            stopLoss = 0.0,
            target1 = 0.0,
            target2 = 0.0,
            riskRewardRatio = "—",
            signalReason = reason
        )

        fun noTrade(
            reason: String,
            spot: Double = 0.0,
            strike: Double = 0.0,
            expiry: String = "—",
            confluence: TechnicalConfluence? = null
        ) = NormalOptionsSignal(
            action = NormalSignalAction.NO_TRADE,
            confidencePercent = 0,
            suggestedStrike = strike,
            strikeDisplay = if (strike > 0.0) "${strike.toInt()}" else "—",
            optionType = OptionType.CE,
            expiryDate = expiry,
            entryPrice = 0.0,
            stopLoss = 0.0,
            target1 = 0.0,
            target2 = 0.0,
            riskRewardRatio = "—",
            signalReason = reason,
            confluence = confluence,
            underlyingSpot = spot
        )

        fun waitingForConfirmation(
            reason: String,
            spot: Double,
            strike: Double,
            type: OptionType,
            expiry: String,
            confidence: Int,
            confluence: TechnicalConfluence? = null
        ) = NormalOptionsSignal(
            action = NormalSignalAction.WAITING_FOR_CONFIRMATION,
            confidencePercent = confidence,
            suggestedStrike = strike,
            strikeDisplay = "${strike.toInt()} ${type.name}",
            optionType = type,
            expiryDate = expiry,
            entryPrice = 0.0,
            stopLoss = 0.0,
            target1 = 0.0,
            target2 = 0.0,
            riskRewardRatio = "—",
            signalReason = reason,
            confluence = confluence,
            underlyingSpot = spot
        )
    }
}
