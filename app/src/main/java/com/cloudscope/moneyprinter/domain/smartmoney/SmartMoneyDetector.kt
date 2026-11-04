package com.cloudscope.moneyprinter.domain.smartmoney

import com.cloudscope.moneyprinter.data.model.HistoricalCandle
import com.cloudscope.moneyprinter.data.model.MarketQuote
import com.cloudscope.moneyprinter.data.model.NiftyFuturesQuote
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object SmartMoneyDetector {

    /**
     * Analyzes NIFTY Spot and NIFTY Futures for institutional smart money activity.
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

        // 4. Bullish / Bearish Market Structure
        val marketStructure = detectMarketStructure(candles, spotPrice)

        // 5. Futures Open Interest changes & Price / OI confirmation
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
            append(priceOiRegime.displayName)
            append(" • ")
            append(marketStructure.displayName)
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

        // Reclaim: Currently above VWAP, but previous candle or recent low tested/dipped below VWAP
        if (spotPrice > vwap) {
            val dippedBelow = (lastCandle != null && lastCandle.low < vwap) ||
                    (prevCandle != null && prevCandle.close <= vwap)
            return if (dippedBelow) VwapRelationship.RECLAIM else VwapRelationship.ABOVE_VWAP
        }

        // Rejection: Currently below VWAP, but previous candle or recent high probed above VWAP
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

        // Look at the latest 3 candles for a sweep and reject pattern
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

            // Bullish sweep: low dips below key support, but closes back above or leaves a heavy lower wick (>= 35% of range)
            for (sup in supportLevels) {
                if (candle.low < sup) {
                    val lowerWick = min(candle.open, candle.close) - candle.low
                    val wickRatio = lowerWick / range
                    if (candle.close >= (sup - 5.0) && wickRatio >= 0.35) {
                        return LiquiditySweepState.BULLISH_SWEEP
                    }
                }
            }

            // Bearish sweep: high spikes above key resistance, but closes back below or leaves a heavy upper wick (>= 35% of range)
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

    private fun detectMarketStructure(
        candles: List<HistoricalCandle>,
        spotPrice: Double
    ): MarketStructureState {
        if (candles.size < 5) return MarketStructureState.RANGEBOUND

        // Identify local swing highs and swing lows (3-candle fractal pivots)
        val swingHighs = mutableListOf<Double>()
        val swingLows = mutableListOf<Double>()

        for (i in 1 until candles.size - 1) {
            val curr = candles[i]
            val prev = candles[i - 1]
            val next = candles[i + 1]

            if (curr.high > prev.high && curr.high >= next.high) {
                swingHighs.add(curr.high)
            }
            if (curr.low < prev.low && curr.low <= next.low) {
                swingLows.add(curr.low)
            }
        }

        val lastSwingHigh = swingHighs.lastOrNull()
        val prevSwingHigh = if (swingHighs.size >= 2) swingHighs[swingHighs.size - 2] else null

        val lastSwingLow = swingLows.lastOrNull()
        val prevSwingLow = if (swingLows.size >= 2) swingLows[swingLows.size - 2] else null

        // Break of Structure (BOS)
        if (lastSwingHigh != null && spotPrice > lastSwingHigh) {
            return MarketStructureState.BULLISH_STRUCTURE
        }
        if (lastSwingLow != null && spotPrice < lastSwingLow) {
            return MarketStructureState.BEARISH_STRUCTURE
        }

        // Higher Highs & Higher Lows
        if (lastSwingHigh != null && prevSwingHigh != null && lastSwingHigh > prevSwingHigh &&
            lastSwingLow != null && prevSwingLow != null && lastSwingLow > prevSwingLow
        ) {
            return MarketStructureState.BULLISH_STRUCTURE
        }

        // Lower Highs & Lower Lows
        if (lastSwingHigh != null && prevSwingHigh != null && lastSwingHigh < prevSwingHigh &&
            lastSwingLow != null && prevSwingLow != null && lastSwingLow < prevSwingLow
        ) {
            return MarketStructureState.BEARISH_STRUCTURE
        }

        return MarketStructureState.RANGEBOUND
    }

    private fun determinePriceOiRegime(priceChange: Double, oiChange: Long): PriceOiRegime {
        val oiThreshold = 10000L // Minimum institutional contract OI change
        val priceThreshold = 3.0 // Minimum spot/futures move in points

        return when {
            priceChange >= priceThreshold && oiChange >= oiThreshold -> PriceOiRegime.LONG_BUILDUP
            priceChange <= -priceThreshold && oiChange >= oiThreshold -> PriceOiRegime.SHORT_BUILDUP
            priceChange >= priceThreshold && oiChange <= -oiThreshold -> PriceOiRegime.SHORT_COVERING
            priceChange <= -priceThreshold && oiChange <= -oiThreshold -> PriceOiRegime.LONG_UNWINDING
            else -> PriceOiRegime.NEUTRAL
        }
    }
}
