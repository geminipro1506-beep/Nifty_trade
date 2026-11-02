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

        // Extreme VIX check: If VIX > 25, options have extreme gamma risk and huge bid-ask slippage
        if (indicators.vix > 25.0) {
            return TradingSignal.noTrade("India VIX elevated at ${String.format("%.2f", indicators.vix)}. Volatility regime unfavorable for naked options.")
        }

        // Calibrate AI confidence using validated probability model
        val (confidencePercent, confidenceRating) = ConfidenceCalibrator.calculateCalibratedProbability(
            direction = indicators.marketDirection,
            indicators = indicators,
            optionChain = optionChain,
            candleCount = candleCount
        )

        val reasons = mutableListOf<String>()
        val summaryParts = mutableListOf<String>()

        if (indicators.marketDirection == MarketDirection.BULLISH) {
            if (quote.lastPrice > indicators.vwap) {
                reasons.add("Price trading above intraday VWAP (₹${String.format("%.1f", indicators.vwap)})")
                summaryParts.add("VWAP Bullish")
            }
            if (indicators.ema9 > indicators.ema20) {
                reasons.add("9 EMA above 20 EMA short-term momentum")
                summaryParts.add("9/20 EMA Bullish")
            }
            if (indicators.ema20 > indicators.ema50) {
                reasons.add("20 EMA above 50 EMA golden alignment")
                summaryParts.add("EMA Trend Bullish")
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
            if (indicators.ema9 < indicators.ema20 && indicators.ema9 > 0) {
                reasons.add("9 EMA below 20 EMA short-term breakdown")
                summaryParts.add("9/20 EMA Bearish")
            }
            if (indicators.ema20 < indicators.ema50) {
                reasons.add("20 EMA below 50 EMA death alignment")
                summaryParts.add("EMA Trend Bearish")
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

        // Threshold check:
        // If confidence is between 50% and 61%, show WAIT state with explanation of what's missing
        if (confidencePercent in 50..61) {
            val missingReason = "Awaiting confirmation: directional bias building (${confidencePercent}% confidence), but multi-factor confluence is incomplete. Waiting for volume & EMA confirmation."
            return TradingSignal.wait(
                reason = missingReason,
                spot = quote.lastPrice,
                direction = indicators.marketDirection,
                confidence = confidencePercent,
                factors = if (summaryParts.isNotEmpty()) summaryParts.joinToString(" | ") else "Setup Forming"
            )
        }

        // If confidence < 50%, completely discard as NO TRADE
        if (confidencePercent < 50 || confidenceRating == ConfidenceRating.LOW) {
            return TradingSignal.noTrade("Low statistical edge (${confidencePercent}% confidence). Setup does not meet confirmation threshold.")
        }

        // Select optimal contract
        val opt = OptionsSelector.selectOptimalContract(
            spotPrice = quote.lastPrice,
            direction = indicators.marketDirection,
            optionChain = optionChain,
            atr = indicators.atr14
        ) ?: return TradingSignal.noTrade("No liquid option contract with acceptable bid-ask spread found.")

        val strategy = when {
            (indicators.marketDirection == MarketDirection.BULLISH && indicators.orbState == OrbState.BREAKOUT_UP) ||
            (indicators.marketDirection == MarketDirection.BEARISH && indicators.orbState == OrbState.BREAKOUT_DOWN) -> StrategyType.BREAKOUT
            (indicators.marketDirection == MarketDirection.BULLISH && indicators.rsi14 < 45.0) ||
            (indicators.marketDirection == MarketDirection.BEARISH && indicators.rsi14 > 55.0) -> StrategyType.REVERSAL
            else -> StrategyType.TREND_FOLLOWING
        }

        val quality = when {
            confidencePercent >= 80 -> "A+ (Elite Confluence)"
            confidencePercent >= 70 -> "A (High Probability)"
            else -> "B (Moderate Confluence)"
        }

        val secondaryText = if (summaryParts.isNotEmpty()) summaryParts.joinToString(" | ") else "Technical Confirmation"
        val capitalReq = opt.entryPrice * 25.0 // 1 lot of NIFTY = 25 qty

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
            confirmationReasons = reasons,
            strategyType = strategy,
            setupQuality = quality,
            expiryDate = "Current Weekly (07 NOV)",
            lots = 1,
            lotSize = 25,
            capitalRequired = capitalReq,
            estimatedCharges = 55.0
        )
    }
}
