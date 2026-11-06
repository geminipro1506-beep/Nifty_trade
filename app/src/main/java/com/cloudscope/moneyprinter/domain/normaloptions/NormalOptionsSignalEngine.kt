package com.cloudscope.moneyprinter.domain.normaloptions

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.indicator.IndicatorEngine
import com.cloudscope.moneyprinter.domain.options.ExpiryCalculator
import com.cloudscope.moneyprinter.domain.options.OptionsSelector
import java.util.Locale
import kotlin.math.max

object NormalOptionsSignalEngine {

    /**
     * Stale threshold: If quote is older than 15 minutes during active monitoring.
     */
    private const val STALE_DATA_THRESHOLD_MILLIS = 15 * 60 * 1000L

    /**
     * Generates independent Normal Options Signal using pure technical analysis & price action.
     * Enforces the rule: Never generate CE/PE signals based on RSI or VWAP alone.
     */
    fun generateSignal(
        quote: MarketQuote?,
        candles: List<HistoricalCandle>,
        indicators: TechnicalIndicators?,
        optionChain: OptionChain?,
        supportResistance: SupportResistanceLevels? = null,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): NormalOptionsSignal {
        // 1. Guard against missing market feed
        if (quote == null || candles.isEmpty()) {
            return NormalOptionsSignal.waitingForData("Awaiting live NIFTY spot tick and candle feed.")
        }

        // 2. Check for stale data
        val isStale = (currentTimeMillis - quote.timestamp) > STALE_DATA_THRESHOLD_MILLIS && quote.timestamp > 0
        if (isStale) {
            return NormalOptionsSignal.waitingForData("Market data feed is delayed. Waiting for fresh live ticks.")
        }

        val spotPrice = quote.lastPrice
        if (spotPrice <= 0.0) {
            return NormalOptionsSignal.waitingForData("Invalid spot price tick received.")
        }

        // 3. Expiry date calculation
        val expiryInfo = ExpiryCalculator.calculateUpcomingExpiries(quote.timestamp)
        val expiryDate = optionChain?.expiryDates?.firstOrNull() ?: expiryInfo.currentWeekly

        // 4. Calculate Technical Indicators
        val ema9 = indicators?.ema9?.takeIf { it > 0 } ?: IndicatorEngine.calculateEma(candles, 9)
        val ema21 = IndicatorEngine.calculateEma(candles, 21)
        val vwap = indicators?.vwap ?: IndicatorEngine.calculateVwap(candles)
        val rsi14 = indicators?.rsi14 ?: IndicatorEngine.calculateRsi(candles, 14)
        val macd = IndicatorEngine.calculateMacd(candles)
        val macdHistogram = indicators?.macdHistogram ?: macd.histogram
        val macdLine = indicators?.macdLine ?: macd.macdLine
        val macdSignalLine = indicators?.signalLine ?: macd.signalLine
        val supertrend = IndicatorEngine.calculateSupertrend(candles, period = 10, multiplier = 3.0)
        val atr = max(15.0, indicators?.atr14 ?: IndicatorEngine.calculateAtr(candles, 14))

        // 5. Volume Confirmation: Current volume vs 20-period candle average
        val recentCandlesForVol = candles.takeLast(20)
        val avgVolume = if (recentCandlesForVol.isNotEmpty()) {
            recentCandlesForVol.map { it.volume }.average().toLong()
        } else 0L
        val currentVolume = quote.volume.takeIf { it > 0 } ?: candles.last().volume
        val isVolumeConfirmed = if (avgVolume > 0) currentVolume >= (avgVolume * 0.85) else true

        // 6. Support & Resistance and Price Action Breakout / Breakdown
        val pdh = quote.high
        val pdl = quote.low
        val pdc = quote.close
        val sr = supportResistance ?: IndicatorEngine.calculateSupportResistance(pdh, pdl, pdc)

        val orbHigh = indicators?.orbHigh ?: 0.0
        val orbLow = indicators?.orbLow ?: 0.0

        // Price Action Swing extremes (last 10 candles excluding current)
        val recentSwingHigh = if (candles.size > 2) {
            candles.dropLast(1).takeLast(10).maxOfOrNull { it.high } ?: pdh
        } else pdh
        val recentSwingLow = if (candles.size > 2) {
            candles.dropLast(1).takeLast(10).minOfOrNull { it.low } ?: pdl
        } else pdl

        val isLastCandleBullish = candles.last().close >= candles.last().open
        val isLastCandleBearish = candles.last().close <= candles.last().open

        // Breakout / Breakdown detections
        val isOrbBreakoutUp = (indicators?.orbState == OrbState.BREAKOUT_UP) || (orbHigh > 0.0 && spotPrice >= orbHigh)
        val isOrbBreakoutDown = (indicators?.orbState == OrbState.BREAKOUT_DOWN) || (orbLow > 0.0 && spotPrice <= orbLow)

        val isBreakoutBullish = isOrbBreakoutUp || spotPrice >= sr.r1 || spotPrice >= pdh || spotPrice >= recentSwingHigh || indicators?.marketDirection == MarketDirection.BULLISH
        val isBreakdownBearish = isOrbBreakoutDown || spotPrice <= sr.s1 || spotPrice <= pdl || spotPrice <= recentSwingLow || indicators?.marketDirection == MarketDirection.BEARISH

        val breakoutLevel = when {
            isOrbBreakoutUp -> "15m ORB High (₹${String.format(Locale.US, "%.1f", orbHigh)})"
            spotPrice >= pdh -> "Previous Day High (₹${String.format(Locale.US, "%.1f", pdh)})"
            spotPrice >= sr.r1 -> "Resistance R1 (₹${String.format(Locale.US, "%.1f", sr.r1)})"
            spotPrice >= recentSwingHigh -> "Swing High (₹${String.format(Locale.US, "%.1f", recentSwingHigh)})"
            isOrbBreakoutDown -> "15m ORB Low (₹${String.format(Locale.US, "%.1f", orbLow)})"
            spotPrice <= pdl -> "Previous Day Low (₹${String.format(Locale.US, "%.1f", pdl)})"
            spotPrice <= sr.s1 -> "Support S1 (₹${String.format(Locale.US, "%.1f", sr.s1)})"
            spotPrice <= recentSwingLow -> "Swing Low (₹${String.format(Locale.US, "%.1f", recentSwingLow)})"
            else -> "Within S/R Pivot Range"
        }

        // 7. Individual Criteria Evaluations
        // EMA: 9 EMA > 21 EMA and price aligned
        val isEmaBullish = (ema9 > ema21) && (spotPrice >= ema9 * 0.998)
        val isEmaBearish = (ema9 < ema21) && (spotPrice <= ema9 * 1.002)

        // VWAP: Price above/below VWAP
        val isVwapBullish = spotPrice >= vwap * 0.9995
        val isVwapBearish = spotPrice <= vwap * 1.0005

        // RSI 14: Bullish momentum in 50..78, Bearish momentum in 22..50
        val isRsiBullish = rsi14 in 50.0..78.0
        val isRsiBearish = rsi14 in 22.0..50.0

        // MACD: Histogram > 0 or MACD line > Signal line
        val isMacdBullish = macdHistogram > 0.0 || macdLine > macdSignalLine
        val isMacdBearish = macdHistogram < 0.0 || macdLine < macdSignalLine

        // Supertrend: Price > Supertrend (Bullish) or Price < Supertrend (Bearish)
        val isSupertrendBullish = supertrend.isBullish && spotPrice >= supertrend.value
        val isSupertrendBearish = !supertrend.isBullish && spotPrice <= supertrend.value

        val confluence = TechnicalConfluence(
            ema9 = ema9,
            ema21 = ema21,
            isEmaBullish = isEmaBullish,
            isEmaBearish = isEmaBearish,
            rsi14 = rsi14,
            isRsiBullish = isRsiBullish,
            isRsiBearish = isRsiBearish,
            macdHistogram = macdHistogram,
            isMacdBullish = isMacdBullish,
            isMacdBearish = isMacdBearish,
            vwap = vwap,
            isVwapBullish = isVwapBullish,
            isVwapBearish = isVwapBearish,
            supertrendValue = supertrend.value,
            isSupertrendBullish = isSupertrendBullish,
            isSupertrendBearish = isSupertrendBearish,
            currentVolume = currentVolume,
            averageVolume = avgVolume,
            isVolumeConfirmed = isVolumeConfirmed,
            isBreakoutBullish = isBreakoutBullish,
            isBreakdownBearish = isBreakdownBearish,
            breakoutLevel = breakoutLevel
        )

        val suggestedStrike = OptionsSelector.roundToStrikeStep(spotPrice)

        // 8. Multi-Factor Confluence Scoring (Never trigger on RSI or VWAP alone)
        var bullishPoints = 0
        if (isEmaBullish) bullishPoints += 15
        if (isVwapBullish) bullishPoints += 15
        if (isRsiBullish) bullishPoints += 15
        if (isMacdBullish) bullishPoints += 15
        if (isSupertrendBullish) bullishPoints += 15
        if (isVolumeConfirmed) bullishPoints += 15
        if (isBreakoutBullish) bullishPoints += 10

        var bearishPoints = 0
        if (isEmaBearish) bearishPoints += 15
        if (isVwapBearish) bearishPoints += 15
        if (isRsiBearish) bearishPoints += 15
        if (isMacdBearish) bearishPoints += 15
        if (isSupertrendBearish) bearishPoints += 15
        if (isVolumeConfirmed) bearishPoints += 15
        if (isBreakdownBearish) bearishPoints += 10

        // Strict Enforcement: Cannot generate BUY CALL or BUY PUT on VWAP or RSI alone!
        // Core mandatory pillars for BUY CALL: EMA alignment + VWAP + Supertrend + MACD/RSI + Breakout confirmation
        val isFullBullishSetup = isEmaBullish && isVwapBullish && isSupertrendBullish &&
                (isRsiBullish || isMacdBullish) && isBreakoutBullish && bullishPoints >= 70

        val isFullBearishSetup = isEmaBearish && isVwapBearish && isSupertrendBearish &&
                (isRsiBearish || isMacdBearish) && isBreakdownBearish && bearishPoints >= 70

        // Setup Forming / Waiting for Confirmation
        val isBullishDeveloping = isEmaBullish && isVwapBullish && (isSupertrendBullish || isMacdBullish) && bullishPoints in 50..69
        val isBearishDeveloping = isEmaBearish && isVwapBearish && (isSupertrendBearish || isMacdBearish) && bearishPoints in 50..69

        return when {
            isFullBullishSetup -> {
                buildActiveSignal(
                    action = NormalSignalAction.BUY_CALL,
                    optionType = OptionType.CE,
                    spotPrice = spotPrice,
                    suggestedStrike = suggestedStrike,
                    expiryDate = expiryDate,
                    confidence = bullishPoints.coerceIn(70, 95),
                    optionChain = optionChain,
                    atr = atr,
                    reason = "Bullish 9/21 EMA alignment (₹${String.format(Locale.US, "%.1f", ema9)} > ₹${String.format(Locale.US, "%.1f", ema21)}), price above VWAP (₹${String.format(Locale.US, "%.1f", vwap)}), Supertrend bullish (₹${String.format(Locale.US, "%.1f", supertrend.value)}), RSI at ${rsi14.toInt()} with MACD expansion, and confirmed breakout above $breakoutLevel.",
                    confluence = confluence
                )
            }

            isFullBearishSetup -> {
                buildActiveSignal(
                    action = NormalSignalAction.BUY_PUT,
                    optionType = OptionType.PE,
                    spotPrice = spotPrice,
                    suggestedStrike = suggestedStrike,
                    expiryDate = expiryDate,
                    confidence = bearishPoints.coerceIn(70, 95),
                    optionChain = optionChain,
                    atr = atr,
                    reason = "Bearish 9/21 EMA alignment (₹${String.format(Locale.US, "%.1f", ema9)} < ₹${String.format(Locale.US, "%.1f", ema21)}), price below VWAP (₹${String.format(Locale.US, "%.1f", vwap)}), Supertrend bearish (₹${String.format(Locale.US, "%.1f", supertrend.value)}), RSI at ${rsi14.toInt()} with negative MACD expansion, and confirmed breakdown below $breakoutLevel.",
                    confluence = confluence
                )
            }

            isBullishDeveloping -> {
                val missingCriteria = if (!isBreakoutBullish) "price action breakout confirmation" else "volume expansion"
                NormalOptionsSignal.waitingForConfirmation(
                    reason = "Bullish setup forming (${bullishPoints}% confluence). Waiting for $missingCriteria above $breakoutLevel.",
                    spot = spotPrice,
                    strike = suggestedStrike,
                    type = OptionType.CE,
                    expiry = expiryDate,
                    confidence = bullishPoints,
                    confluence = confluence
                )
            }

            isBearishDeveloping -> {
                val missingCriteria = if (!isBreakdownBearish) "price action breakdown confirmation" else "volume expansion"
                NormalOptionsSignal.waitingForConfirmation(
                    reason = "Bearish setup forming (${bearishPoints}% confluence). Waiting for $missingCriteria below $breakoutLevel.",
                    spot = spotPrice,
                    strike = suggestedStrike,
                    type = OptionType.PE,
                    expiry = expiryDate,
                    confidence = bearishPoints,
                    confluence = confluence
                )
            }

            else -> {
                val sidewaysReason = when {
                    bullishPoints > 0 && bearishPoints > 0 && kotlin.math.abs(bullishPoints - bearishPoints) < 20 ->
                        "Conflicting indicators: EMA, VWAP, and Supertrend directional signals diverge. No clear market edge."
                    kotlin.math.abs(spotPrice - vwap) < 15.0 ->
                        "Price consolidating tightly at VWAP (₹${String.format(Locale.US, "%.1f", vwap)}) within intraday range. Waiting for directional break."
                    else ->
                        "Insufficient technical confirmation (${max(bullishPoints, bearishPoints)}% confluence). Market is rangebound."
                }
                NormalOptionsSignal.noTrade(
                    reason = sidewaysReason,
                    spot = spotPrice,
                    strike = suggestedStrike,
                    expiry = expiryDate,
                    confluence = confluence
                )
            }
        }
    }

    private fun buildActiveSignal(
        action: NormalSignalAction,
        optionType: OptionType,
        spotPrice: Double,
        suggestedStrike: Double,
        expiryDate: String,
        confidence: Int,
        optionChain: OptionChain?,
        atr: Double,
        reason: String,
        confluence: TechnicalConfluence
    ): NormalOptionsSignal {
        // Find live option contract from OptionChain for actual live premium
        val row = optionChain?.rows?.find { it.strikePrice == suggestedStrike }
        val contract = if (optionType == OptionType.CE) row?.callContract else row?.putContract

        // Live premium lookup with Black-Scholes / ATR fallback
        val livePremium = if (contract != null && (contract.askPrice > 0.0 || contract.lastPrice > 0.0)) {
            if (contract.askPrice > 0.0) contract.askPrice else contract.lastPrice
        } else {
            // Intraday ATM option approximation based on volatility and spot
            max(45.0, OptionsSelector.roundToTickSize(atr * 1.15))
        }

        // Volatility & Market-Structure based Risk Management
        // Risk: 18% - 20% of premium, minimum ₹15.0
        val riskAmount = max(15.0, OptionsSelector.roundToTickSize(livePremium * 0.18))
        val stopLoss = OptionsSelector.roundToTickSize(max(5.0, livePremium - riskAmount))

        // Target 1: 1.5x Risk (1:1.5 R:R)
        val target1 = OptionsSelector.roundToTickSize(livePremium + (riskAmount * 1.5))

        // Target 2: 2.0x Risk (1:2.0 R:R)
        val target2 = OptionsSelector.roundToTickSize(livePremium + (riskAmount * 2.0))

        val riskPoints = livePremium - stopLoss
        val rewardPoints = target2 - livePremium
        val rrRatio = if (riskPoints > 0.0) {
            String.format(Locale.US, "1 : %.1f", rewardPoints / riskPoints)
        } else "1 : 2.0"

        return NormalOptionsSignal(
            action = action,
            confidencePercent = confidence,
            suggestedStrike = suggestedStrike,
            strikeDisplay = "${suggestedStrike.toInt()} ${optionType.name}",
            optionType = optionType,
            expiryDate = expiryDate,
            entryPrice = livePremium,
            stopLoss = stopLoss,
            target1 = target1,
            target2 = target2,
            riskRewardRatio = rrRatio,
            signalReason = reason,
            confluence = confluence,
            underlyingSpot = spotPrice
        )
    }
}
