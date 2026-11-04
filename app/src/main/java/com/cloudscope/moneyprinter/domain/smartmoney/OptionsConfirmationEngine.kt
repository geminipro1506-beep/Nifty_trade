package com.cloudscope.moneyprinter.domain.smartmoney

import com.cloudscope.moneyprinter.data.model.OptionChain
import com.cloudscope.moneyprinter.data.model.OptionContract
import com.cloudscope.moneyprinter.domain.options.OptionsSelector

object OptionsConfirmationEngine {

    /**
     * Evaluates option chain data for institutional confirmation.
     * Distinguishes between Long Buildup, Short Buildup (Writing), Short Covering, and Long Unwinding.
     */
    fun confirmOptions(
        spotPrice: Double,
        optionChain: OptionChain?
    ): OptionConfirmationDetails {
        if (optionChain == null || optionChain.rows.isEmpty() || spotPrice <= 0.0) {
            return OptionConfirmationDetails(
                isAvailable = false,
                pcr = 0.0,
                atmStrike = 0.0,
                atmCeBuildup = OptionBuildupType.NEUTRAL,
                atmPeBuildup = OptionBuildupType.NEUTRAL,
                atmCeVolume = 0L,
                atmPeVolume = 0L,
                atmCeChange = 0.0,
                atmPeChange = 0.0,
                atmCeBidAskSpread = 0.0,
                atmPeBidAskSpread = 0.0,
                isBidAskAcceptable = false,
                isBullishConfirmed = false,
                isBearishConfirmed = false,
                confirmationReason = "Option-chain confirmation unavailable (awaiting live feed)"
            )
        }

        val atmStrike = OptionsSelector.roundToStrikeStep(spotPrice)
        val atmRow = optionChain.rows.find { it.strikePrice == atmStrike }
        val atmCe = atmRow?.callContract
        val atmPe = atmRow?.putContract

        val pcr = optionChain.pcr

        // Classify contract buildup strictly separating writing from buying
        val ceBuildup = classifyContractBuildup(atmCe)
        val peBuildup = classifyContractBuildup(atmPe)

        val ceVolume = atmCe?.volume ?: 0L
        val peVolume = atmPe?.volume ?: 0L

        val ceChange = atmCe?.change ?: 0.0
        val peChange = atmPe?.change ?: 0.0

        val ceSpread = if (atmCe != null && atmCe.askPrice > 0 && atmCe.bidPrice > 0) {
            atmCe.askPrice - atmCe.bidPrice
        } else 0.50

        val peSpread = if (atmPe != null && atmPe.askPrice > 0 && atmPe.bidPrice > 0) {
            atmPe.askPrice - atmPe.bidPrice
        } else 0.50

        val isCeLiquid = isContractLiquid(atmCe, ceSpread)
        val isPeLiquid = isContractLiquid(atmPe, peSpread)

        // Near-ATM Aggregate Shift (ATM-1, ATM, ATM+1)
        val nearRows = optionChain.rows.filter { kotlin.math.abs(it.strikePrice - atmStrike) <= 50.0 }
        val nearCeOiChange = nearRows.sumOf { it.callContract?.changeInOi ?: 0L }
        val nearPeOiChange = nearRows.sumOf { it.putContract?.changeInOi ?: 0L }

        // Institutional Bullish Confirmation:
        // 1. PCR >= 1.0 (Put OI exceeds Call OI)
        // 2. Put writing (Put Short Buildup or heavier Put addition than Call addition) OR Call Short Covering / Call Buying
        // 3. CE premium movement positive
        // 4. Liquid spread
        val isPutWritingDominant = peBuildup == OptionBuildupType.SHORT_BUILDUP ||
                nearPeOiChange > nearCeOiChange ||
                optionChain.totalPutOiChange > optionChain.totalCallOiChange
        val isCallSqueeze = ceBuildup == OptionBuildupType.SHORT_COVERING || ceBuildup == OptionBuildupType.LONG_BUILDUP
        val isCePremiumExpanding = ceChange >= 0.0 || (atmCe?.pChange ?: 0.0) >= 0.0

        val isBullishConfirmed = isCeLiquid &&
                pcr >= 1.0 &&
                (isPutWritingDominant || isCallSqueeze) &&
                isCePremiumExpanding

        // Institutional Bearish Confirmation:
        // 1. PCR <= 0.95 (Call OI exceeds Put OI)
        // 2. Call writing (Call Short Buildup or heavier Call addition than Put addition) OR Put Short Covering / Put Buying
        // 3. PE premium movement positive
        // 4. Liquid spread
        val isCallWritingDominant = ceBuildup == OptionBuildupType.SHORT_BUILDUP ||
                nearCeOiChange > nearPeOiChange ||
                optionChain.totalCallOiChange > optionChain.totalPutOiChange
        val isPutSqueeze = peBuildup == OptionBuildupType.SHORT_COVERING || peBuildup == OptionBuildupType.LONG_BUILDUP
        val isPePremiumExpanding = peChange >= 0.0 || (atmPe?.pChange ?: 0.0) >= 0.0

        val isBearishConfirmed = isPeLiquid &&
                pcr <= 0.95 &&
                (isCallWritingDominant || isPutSqueeze) &&
                isPePremiumExpanding

        val reason = buildString {
            if (isBullishConfirmed) {
                append("Bullish: PCR ${String.format(java.util.Locale.US, "%.2f", pcr)} confirms Put writing support at ${atmStrike.toInt()} PE (${peBuildup.displayName})")
            } else if (isBearishConfirmed) {
                append("Bearish: PCR ${String.format(java.util.Locale.US, "%.2f", pcr)} confirms Call writing resistance at ${atmStrike.toInt()} CE (${ceBuildup.displayName})")
            } else {
                append("Neutral: PCR ${String.format(java.util.Locale.US, "%.2f", pcr)} with balanced CE/PE activity (${ceBuildup.displayName} CE vs ${peBuildup.displayName} PE)")
            }
        }

        return OptionConfirmationDetails(
            isAvailable = true,
            pcr = pcr,
            atmStrike = atmStrike,
            atmCeBuildup = ceBuildup,
            atmPeBuildup = peBuildup,
            atmCeVolume = ceVolume,
            atmPeVolume = peVolume,
            atmCeChange = ceChange,
            atmPeChange = peChange,
            atmCeBidAskSpread = ceSpread,
            atmPeBidAskSpread = peSpread,
            isBidAskAcceptable = isCeLiquid && isPeLiquid,
            isBullishConfirmed = isBullishConfirmed,
            isBearishConfirmed = isBearishConfirmed,
            confirmationReason = reason
        )
    }

    /**
     * Distinguishes option buildup based on price and OI direction:
     * - Price UP + OI UP = Long Buildup (Aggressive buying)
     * - Price DOWN + OI UP = Short Buildup (Aggressive writing / selling)
     * - Price UP + OI DOWN = Short Covering (Sellers covering / short squeeze)
     * - Price DOWN + OI DOWN = Long Unwinding (Buyers booking losses / liquidating)
     */
    fun classifyContractBuildup(contract: OptionContract?): OptionBuildupType {
        if (contract == null) return OptionBuildupType.NEUTRAL

        val priceChange = contract.change
        val oiChange = contract.changeInOi

        val oiThreshold = 1000L // Minimum OI change to consider significant

        return when {
            priceChange > 0 && oiChange > oiThreshold -> OptionBuildupType.LONG_BUILDUP
            priceChange < 0 && oiChange > oiThreshold -> OptionBuildupType.SHORT_BUILDUP
            priceChange > 0 && oiChange < -oiThreshold -> OptionBuildupType.SHORT_COVERING
            priceChange < 0 && oiChange < -oiThreshold -> OptionBuildupType.LONG_UNWINDING
            else -> OptionBuildupType.NEUTRAL
        }
    }

    private fun isContractLiquid(contract: OptionContract?, spread: Double): Boolean {
        if (contract == null) return false
        val ltp = contract.lastPrice
        if (ltp <= 10.0) return false // Deep OTM illiquid lot
        val spreadPercent = if (ltp > 0) (spread / ltp) * 100.0 else 100.0
        return spreadPercent <= 2.5 || spread <= 2.50
    }
}
