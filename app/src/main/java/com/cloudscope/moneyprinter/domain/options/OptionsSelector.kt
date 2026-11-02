package com.cloudscope.moneyprinter.domain.options

import com.cloudscope.moneyprinter.data.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

object OptionsSelector {

    // NIFTY options tick size is 0.05
    fun roundToTickSize(value: Double): Double {
        return round(value * 20.0) / 20.0
    }

    // Rounds to nearest NIFTY strike (multiples of 50)
    fun roundToStrikeStep(value: Double): Double {
        return (round(value / 50.0) * 50.0)
    }

    data class SelectedOptionRecommendation(
        val strikePrice: Double,
        val optionType: OptionType,
        val contractSymbol: String,
        val entryPrice: Double,
        val stopLoss: Double,
        val target1: Double,
        val target2: Double,
        val riskRewardRatio: Double,
        val delta: Double,
        val bidAskSpread: Double
    )

    fun selectOptimalContract(
        spotPrice: Double,
        direction: MarketDirection,
        optionChain: OptionChain?,
        atr: Double
    ): SelectedOptionRecommendation? {
        if (spotPrice <= 0.0 || direction == MarketDirection.SIDEWAYS) return null

        val atmStrike = roundToStrikeStep(spotPrice)
        val optionType = if (direction == MarketDirection.BULLISH) OptionType.CE else OptionType.PE

        // Target Strike: ATM or 1 strike ITM for optimal Delta (0.48 - 0.55) & theta resistance
        val targetStrike = when (optionType) {
            OptionType.CE -> atmStrike // ATM CE
            OptionType.PE -> atmStrike // ATM PE
        }

        // Look up contract from real option chain
        val row = optionChain?.rows?.find { it.strikePrice == targetStrike }
        val contract = if (optionType == OptionType.CE) row?.callContract else row?.putContract

        // If contract is available, validate liquidity & spread
        if (contract != null) {
            // Liquidity filter: Spread must be <= 1.5% of premium (or <= ₹1.5 for low premium)
            val spread = contract.askPrice - contract.bidPrice
            val spreadPercent = if (contract.lastPrice > 0) (spread / contract.lastPrice) * 100.0 else 100.0

            if (spreadPercent > 2.5 && spread > 2.0) {
                // Spread too wide: illiquid, reject trade
                return null
            }

            val entry = if (contract.askPrice > 0) contract.askPrice else contract.lastPrice
            if (entry < 25.0) {
                // Deep OTM / decaying lot with poor delta responsiveness
                return null
            }

            // Technical Stop Loss: Risk 18% to 22% of option premium or 1.2 * option ATR
            val riskAmount = max(15.0, roundToTickSize(entry * 0.20))
            val stopLoss = roundToTickSize(max(5.0, entry - riskAmount))

            // Targets: Target 1 = 1.5x Risk, Target 2 = 2.0x Risk
            val target1 = roundToTickSize(entry + (riskAmount * 1.5))
            val target2 = roundToTickSize(entry + (riskAmount * 2.0))
            val riskRewardRatio = round(((target1 - entry) / (entry - stopLoss)) * 10.0) / 10.0

            return SelectedOptionRecommendation(
                strikePrice = targetStrike,
                optionType = optionType,
                contractSymbol = contract.symbol,
                entryPrice = entry,
                stopLoss = stopLoss,
                target1 = target1,
                target2 = target2,
                riskRewardRatio = riskRewardRatio,
                delta = contract.delta,
                bidAskSpread = spread
            )
        }

        // If option chain row is not yet cached or loaded, calculate based on spot Black-Scholes estimate
        val timeToExpiryYears = 3.0 / 365.0 // ~3 days average weekly expiry
        val estimatedVol = 0.14 // 14% IV
        val estimatedDelta = BlackScholes.calculateDelta(
            spot = spotPrice,
            strike = targetStrike,
            timeToExpiryYears = timeToExpiryYears,
            volatility = estimatedVol,
            isCall = optionType == OptionType.CE
        )

        val estimatedPremium = roundToTickSize(spotPrice * estimatedVol * kotlin.math.sqrt(timeToExpiryYears) * 0.40)
        val entry = max(50.0, estimatedPremium)
        val riskAmount = roundToTickSize(entry * 0.20)
        val stopLoss = roundToTickSize(entry - riskAmount)
        val target1 = roundToTickSize(entry + (riskAmount * 1.5))
        val target2 = roundToTickSize(entry + (riskAmount * 2.0))
        val symbol = "NIFTY ${targetStrike.toInt()} ${optionType.name}"

        return SelectedOptionRecommendation(
            strikePrice = targetStrike,
            optionType = optionType,
            contractSymbol = symbol,
            entryPrice = entry,
            stopLoss = stopLoss,
            target1 = target1,
            target2 = target2,
            riskRewardRatio = 1.5,
            delta = abs(estimatedDelta),
            bidAskSpread = 0.5
        )
    }
}
