package com.cloudscope.moneyprinter.domain.signal

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.options.OptionsSelector

object SignalEngine {

    fun generateSignal(
        quote: MarketQuote,
        indicators: TechnicalIndicators,
        optionChain: OptionChain?,
        candleCount: Int,
        isMarketHours: Boolean = true
    ): TradingSignal {
        // Enforce market hours and trade safety guard
        if (!isMarketHours) {
            return TradingSignal.noTrade("Market is currently closed. Signals generate between 09:20 and 15:00 IST.")
        }

        // If market regime is sideways or no clear direction
        if (indicators.marketDirection == MarketDirection.SIDEWAYS) {
            return TradingSignal.noTrade("Market conditions are not favourable. Price is rangebound between VWAP and EMA.")
        }

        // Calibrate AI confidence using validated probability model
        val (confidencePercent, confidenceRating) = ConfidenceCalibrator.calculateCalibratedProbability(
            direction = indicators.marketDirection,
            indicators = indicators,
            optionChain = optionChain,
            candleCount = candleCount
        )

        // Threshold check: never force a trade if confidence < 62%
        if (confidencePercent < 62 || confidenceRating == ConfidenceRating.LOW) {
            return TradingSignal.noTrade("Low statistical edge (${confidencePercent}% confidence). Setup does not meet confirmation threshold.")
        }

        // Extreme VIX check: If VIX > 25, options have extreme gamma risk and huge bid-ask slippage
        if (indicators.vix > 25.0) {
            return TradingSignal.noTrade("India VIX elevated at ${String.format("%.2f", indicators.vix)}. Volatility regime unfavorable for naked options.")
        }

        // Select optimal contract
        val opt = OptionsSelector.selectOptimalContract(
            spotPrice = quote.lastPrice,
            direction = indicators.marketDirection,
            optionChain = optionChain,
            atr = indicators.atr14
        ) ?: return TradingSignal.noTrade("No liquid option contract with acceptable bid-ask spread found.")

        val reasons = mutableListOf<String>()
        val summaryParts = mutableListOf<String>()

        if (indicators.marketDirection == MarketDirection.BULLISH) {
            if (quote.lastPrice > indicators.vwap) {
                reasons.add("Price trading above intraday VWAP (₹${String.format("%.1f", indicators.vwap)})")
                summaryParts.add("VWAP Bullish")
            }
            if (indicators.ema20 > indicators.ema50) {
                reasons.add("20 EMA above 50 EMA golden alignment")
                summaryParts.add("EMA Bullish")
            }
            if (indicators.pcr >= 1.05) {
                reasons.add("PCR at ${String.format("%.2f", indicators.pcr)} confirms put writing support")
                summaryParts.add("OI Support")
            }
            if (indicators.rsi14 in 54.0..70.0) {
                reasons.add("RSI 14 at ${indicators.rsi14.toInt()} indicates steady upward momentum")
                summaryParts.add("RSI ${indicators.rsi14.toInt()}")
            }
            if (indicators.orbState == OrbState.BREAKOUT_UP) {
                reasons.add("Opening 15-minute range high (₹${String.format("%.1f", indicators.orbHigh)}) broken upward")
            }
        } else {
            if (quote.lastPrice < indicators.vwap) {
                reasons.add("Price trading below intraday VWAP (₹${String.format("%.1f", indicators.vwap)})")
                summaryParts.add("VWAP Bearish")
            }
            if (indicators.ema20 < indicators.ema50) {
                reasons.add("20 EMA below 50 EMA death alignment")
                summaryParts.add("EMA Bearish")
            }
            if (indicators.pcr <= 0.95) {
                reasons.add("PCR at ${String.format("%.2f", indicators.pcr)} confirms call writing resistance")
                summaryParts.add("OI Resistance")
            }
            if (indicators.rsi14 in 30.0..46.0) {
                reasons.add("RSI 14 at ${indicators.rsi14.toInt()} indicates downward momentum")
                summaryParts.add("RSI ${indicators.rsi14.toInt()}")
            }
            if (indicators.orbState == OrbState.BREAKOUT_DOWN) {
                reasons.add("Opening 15-minute range low (₹${String.format("%.1f", indicators.orbLow)}) broken downward")
            }
        }

        val secondaryText = if (summaryParts.isNotEmpty()) summaryParts.joinToString(" | ") else "Technical Confirmation"

        return TradingSignal(
            action = if (indicators.marketDirection == MarketDirection.BULLISH) SignalAction.BUY_CE else SignalAction.BUY_PE,
            underlyingSpot = quote.lastPrice,
            recommendedStrike = opt.strikePrice,
            optionType = opt.optionType,
            contractSymbol = opt.contractSymbol,
            entryPrice = opt.entryPrice,
            stopLoss = opt.stopLoss,
            target1 = opt.target1,
            target2 = opt.target2,
            riskRewardRatio = opt.riskRewardRatio,
            confidencePercent = confidencePercent,
            confidenceRating = confidenceRating,
            status = SignalStatus.ACTIVE,
            marketDirection = indicators.marketDirection,
            summaryFactors = secondaryText,
            confirmationReasons = reasons
        )
    }
}
