package com.cloudscope.moneyprinter.domain.smartmoney

import com.cloudscope.moneyprinter.data.model.HistoricalCandle
import com.cloudscope.moneyprinter.data.model.MarketQuote
import com.cloudscope.moneyprinter.data.model.NiftyFuturesQuote
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object SmartMoneyDetector {

    /**
     * Analyzes NIFTY Spot and NIFTY Futures for potential institutional smart money activity.
     */
    fun analyzeSmartMoney(
        spotQuote: MarketQuote,
        futuresQuote: NiftyFuturesQuote?,
        candles: List<HistoricalCandle>,
        vwap: Double,
        pdh: Double,
        pdl: Double,
        orbHigh: Double,
        orbLow: Double
    ): SmartMoneyActivity {
        val spotPrice = spotQuote.lastPrice
        val effectiveVwap = if (vwap > 0.0) vwap else spotQuote.vwap

        // 1. Unusual Volume Activity
        val (isUnusualVolume, volumeRatio) = detectUnusualVolume(candles, spotQuote.volume)

        // 2. VWAP Reclaim / Rejection
        val vwapState = detectVwapState(spotPrice, effectiveVwap, candles)

        // 3. Liquidity Sweep Detection
        val liquiditySweep = detectLiquiditySweep(candles, pdh, pdl, orbHigh, orbLow)

        // 4. Bullish / Bearish Market Structure, CHoCH, and BOS
        val (marketStructure, chochState, bosState) = detectStructureChochBos(candles, spotPrice)

        // 5. Order Block Detection
        val orderBlock = detectOrderBlock(candles, spotPrice)

        // 6. Futures Open Interest changes & Price / OI confirmation
        val futPrice = futuresQuote?.lastPrice ?: (spotPrice + 35.0)
        val futChange = futuresQuote?.change ?: spotQuote.change

        val currentOi = futuresQuote?.openInterest
            ?: candles.lastOrNull()?.openInterest
            ?: 12500000L
        val prevOi = if (candles.size >= 2) {
            val candlePrevOi = candles[candles.size - 2].openInterest
            if (candlePrevOi > 0) candlePrevOi else currentOi
        } else currentOi

        val futOiChange = futuresQuote?.changeInOi ?: (currentOi - prevOi)
        val baselineOi = max(1L, currentOi - futOiChange)
        val futOiChangePercent = (futOiChange.toDouble() / baselineOi.toDouble()) * 100.0

        val priceOiRegime = determinePriceOiRegime(futChange, futOiChange)

        val summary = buildString {
            append("Potential Smart Money Activity: ")
            append(priceOiRegime.displayName)
            append(" • ")
            append(marketStructure.displayName)
            if (chochState != ChochState.NONE) {
                append(" • ${chochState.displayName}")
            }
            if (bosState != BosState.NONE) {
                append(" • ${bosState.displayName}")
            }
            if (orderBlock != OrderBlockState.NONE) {
                append(" • ${orderBlock.displayName}")
            }
            if (isUnusualVolume) {
                append(" • Unusual Vol (${String.format(java.util.Locale.US, "%.1fx", volumeRatio)})")
            }
            if (liquiditySweep != LiquiditySweepState.NONE) {
                append(" • ${liquiditySweep.displayName}")
            }
        }

        return SmartMoneyActivity(
            spotPrice = spotPrice,
            spotVolume = spotQuote.volume,
            isUnusualVolume = isUnusualVolume,
            volumeRatio = volumeRatio,
            vwap = effectiveVwap,
            vwapState = vwapState,
            liquiditySweep = liquiditySweep,
            marketStructure = marketStructure,
            orderBlock = orderBlock,
            chochState = chochState,
            bosState = bosState,
            futuresPrice = futPrice,
            futuresOi = currentOi,
            futuresOiChange = futOiChange,
            futuresOiChangePercent = futOiChangePercent,
            priceOiRegime = priceOiRegime,
            summary = summary
        )
    }

    private fun detectUnusualVolume(
        candles: List<HistoricalCandle>,
        liveVolume: Long
    ): Pair<Boolean, Double> {
        if (candles.isEmpty()) return Pair(false, 1.0)

        val windowSize = min(candles.size, 20)
        val recentCandles = candles.takeLast(windowSize)
        val avgVolume = recentCandles.map { it.volume }.average()

        val lastCandleVolume = candles.last().volume
        val effectiveVolume = if (lastCandleVolume > 0) lastCandleVolume else liveVolume

        if (avgVolume <= 0.0) return Pair(false, 1.0)

        val ratio = effectiveVolume.toDouble() / avgVolume
        val isUnusual = ratio >= 1.5
        return Pair(isUnusual, Math.round(ratio * 10.0) / 10.0)
    }

    private fun detectVwapState(
        spotPrice: Double,
        vwap: Double,
        candles: List<HistoricalCandle>
    ): VwapRelationship {
        if (vwap <= 0.0) return VwapRelationship.NEUTRAL

        val lastCandle = candles.lastOrNull()
        val prevCandle = if (candles.size >= 2) candles[candles.size - 2] else null

        if (spotPrice > vwap) {
            val dippedBelow = (lastCandle != null && lastCandle.low < vwap) ||
                    (prevCandle != null && prevCandle.close <= vwap)
            return if (dippedBelow) VwapRelationship.RECLAIM else VwapRelationship.ABOVE_VWAP
        }

        if (spotPrice < vwap) {
            val spikedAbove = (lastCandle != null && lastCandle.high > vwap) ||
                    (prevCandle != null && prevCandle.close >= vwap)
            return if (spikedAbove) VwapRelationship.REJECTION else VwapRelationship.BELOW_VWAP
        }

        return VwapRelationship.NEUTRAL
    }

    private fun detectLiquiditySweep(
        candles: List<HistoricalCandle>,
        pdh: Double,
        pdl: Double,
        orbHigh: Double,
        orbLow: Double
    ): LiquiditySweepState {
        if (candles.isEmpty()) return LiquiditySweepState.NONE

        val evaluationCandles = candles.takeLast(3)

        val supportLevels = listOfNotNull(
            pdl.takeIf { it > 0 },
            orbLow.takeIf { it > 0 }
        )

        val resistanceLevels = listOfNotNull(
            pdh.takeIf { it > 0 },
            orbHigh.takeIf { it > 0 }
        )

        for (candle in evaluationCandles) {
            val range = candle.high - candle.low
            if (range <= 0.0) continue

            for (sup in supportLevels) {
                if (candle.low < sup) {
                    val lowerWick = min(candle.open, candle.close) - candle.low
                    val wickRatio = lowerWick / range
                    if (candle.close >= (sup - 5.0) && wickRatio >= 0.35) {
                        return LiquiditySweepState.BULLISH_SWEEP
                    }
                }
            }

            for (res in resistanceLevels) {
                if (candle.high > res) {
                    val upperWick = candle.high - max(candle.open, candle.close)
                    val wickRatio = upperWick / range
                    if (candle.close <= (res + 5.0) && wickRatio >= 0.35) {
                        return LiquiditySweepState.BEARISH_SWEEP
                    }
                }
            }
        }

        return LiquiditySweepState.NONE
    }

    private fun detectStructureChochBos(
        candles: List<HistoricalCandle>,
        spotPrice: Double
    ): Triple<MarketStructureState, ChochState, BosState> {
        if (candles.size < 5) {
            return Triple(MarketStructureState.RANGEBOUND, ChochState.NONE, BosState.NONE)
        }

        val swingHighs = mutableListOf<Double>()
        val swingLows = mutableListOf<Double>()

        for (i in 1 until candles.size - 1) {
            val curr = candles[i]
            val prev = candles[i - 1]
            val next = candles[i + 1]

            // Swing high: either a standard pivot high OR a pullback bounce peak in a downtrend
            val isStandardSwingHigh = curr.high >= prev.high && curr.high >= next.high
            val isPullbackSwingHigh = curr.close > curr.open && curr.high >= next.high && next.low < curr.low
            if (isStandardSwingHigh || isPullbackSwingHigh) {
                swingHighs.add(curr.high)
            }

            // Swing low: either a standard pivot low OR a pullback trough in an uptrend
            val isStandardSwingLow = curr.low <= prev.low && curr.low <= next.low
            val isPullbackSwingLow = curr.close < curr.open && curr.low <= next.low && next.high > curr.high
            if (isStandardSwingLow || isPullbackSwingLow) {
                swingLows.add(curr.low)
            }
        }

        val priorCandles = candles.dropLast(1)
        val lastHigh = swingHighs.lastOrNull() ?: priorCandles.map { it.high }.takeLast(3).maxOrNull()
        val prevHigh = if (swingHighs.size >= 2) swingHighs[swingHighs.size - 2] else null
        val lastLow = swingLows.lastOrNull() ?: priorCandles.map { it.low }.takeLast(3).minOrNull()
        val prevLow = if (swingLows.size >= 2) swingLows[swingLows.size - 2] else null

        var structure = MarketStructureState.RANGEBOUND
        var choch = ChochState.NONE
        var bos = BosState.NONE

        val isOverallDown = priorCandles.isNotEmpty() && (priorCandles.first().close > priorCandles.last().close)
        val isOverallUp = priorCandles.isNotEmpty() && (priorCandles.first().close < priorCandles.last().close)

        val isPriorDowntrend = isOverallDown ||
                (lastHigh != null && prevHigh != null && lastHigh < prevHigh) ||
                (lastLow != null && prevLow != null && lastLow < prevLow)

        val isPriorUptrend = isOverallUp ||
                (lastHigh != null && prevHigh != null && lastHigh > prevHigh) ||
                (lastLow != null && prevLow != null && lastLow > prevLow)

        // CHoCH (Change of Character): Trend reversal breaking opposing swing point
        if (isPriorDowntrend && !isOverallUp && lastHigh != null && spotPrice > lastHigh) {
            choch = ChochState.BULLISH_CHOCH
            structure = MarketStructureState.BULLISH_STRUCTURE
        } else if (isPriorUptrend && !isOverallDown && lastLow != null && spotPrice < lastLow) {
            choch = ChochState.BEARISH_CHOCH
            structure = MarketStructureState.BEARISH_STRUCTURE
        }

        // BOS (Break of Structure): Trend continuation breaking in trend direction
        if (isPriorUptrend && lastHigh != null && spotPrice > lastHigh) {
            bos = BosState.BULLISH_BOS
            structure = MarketStructureState.BULLISH_STRUCTURE
        } else if (isPriorDowntrend && lastLow != null && spotPrice < lastLow) {
            bos = BosState.BEARISH_BOS
            structure = MarketStructureState.BEARISH_STRUCTURE
        }

        if (structure == MarketStructureState.RANGEBOUND) {
            if (lastHigh != null && spotPrice > lastHigh) {
                structure = MarketStructureState.BULLISH_STRUCTURE
                bos = BosState.BULLISH_BOS
            } else if (lastLow != null && spotPrice < lastLow) {
                structure = MarketStructureState.BEARISH_STRUCTURE
                bos = BosState.BEARISH_BOS
            } else if (lastHigh != null && prevHigh != null && lastHigh > prevHigh && lastLow != null && prevLow != null && lastLow > prevLow) {
                structure = MarketStructureState.BULLISH_STRUCTURE
            } else if (lastHigh != null && prevHigh != null && lastHigh < prevHigh && lastLow != null && prevLow != null && lastLow < prevLow) {
                structure = MarketStructureState.BEARISH_STRUCTURE
            }
        }

        return Triple(structure, choch, bos)
    }

    private fun detectOrderBlock(
        candles: List<HistoricalCandle>,
        spotPrice: Double
    ): OrderBlockState {
        if (candles.size < 4) return OrderBlockState.NONE

        val recent = candles.takeLast(6)
        for (i in 0 until recent.size - 2) {
            val c1 = recent[i]
            val c2 = recent[i + 1]
            val c3 = recent[i + 2]

            // Bullish Order Block: down candle followed by sharp impulsive up move
            val isBearishCandle = c1.close < c1.open
            val isImpulsiveUp = c2.close > c2.open && c3.close > c3.open && (c3.close - c1.high) > 20.0
            if (isBearishCandle && isImpulsiveUp && spotPrice >= c1.low && spotPrice <= (c1.high + 15.0)) {
                return OrderBlockState.BULLISH_ORDER_BLOCK
            }

            // Bearish Order Block: up candle followed by sharp impulsive down selloff
            val isBullishCandle = c1.close > c1.open
            val isImpulsiveDown = c2.close < c2.open && c3.close < c3.open && (c1.low - c3.close) > 20.0
            if (isBullishCandle && isImpulsiveDown && spotPrice <= c1.high && spotPrice >= (c1.low - 15.0)) {
                return OrderBlockState.BEARISH_ORDER_BLOCK
            }
        }

        return OrderBlockState.NONE
    }

    private fun determinePriceOiRegime(priceChange: Double, oiChange: Long): PriceOiRegime {
        val oiThreshold = 10000L
        val priceThreshold = 3.0

        return when {
            priceChange >= priceThreshold && oiChange >= oiThreshold -> PriceOiRegime.LONG_BUILDUP
            priceChange <= -priceThreshold && oiChange >= oiThreshold -> PriceOiRegime.SHORT_BUILDUP
            priceChange >= priceThreshold && oiChange <= -oiThreshold -> PriceOiRegime.SHORT_COVERING
            priceChange <= -priceThreshold && oiChange <= -oiThreshold -> PriceOiRegime.LONG_UNWINDING
            else -> PriceOiRegime.NEUTRAL
        }
    }
}
