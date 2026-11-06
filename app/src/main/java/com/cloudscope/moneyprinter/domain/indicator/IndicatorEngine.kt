package com.cloudscope.moneyprinter.domain.indicator

import com.cloudscope.moneyprinter.data.model.*
import kotlin.math.max

object IndicatorEngine {

    fun calculateRsi(candles: List<HistoricalCandle>, period: Int = 14): Double {
        if (candles.size <= period) return 50.0

        var sumGain = 0.0
        var sumLoss = 0.0

        for (i in 1..period) {
            val change = candles[i].close - candles[i - 1].close
            if (change > 0) sumGain += change else sumLoss += -change
        }

        var avgGain = sumGain / period
        var avgLoss = sumLoss / period

        for (i in (period + 1) until candles.size) {
            val change = candles[i].close - candles[i - 1].close
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) -change else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        if (avgGain == 0.0) return 0.0

        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    fun calculateEma(candles: List<HistoricalCandle>, period: Int): Double {
        if (candles.isEmpty()) return 0.0
        if (candles.size <= period) {
            return candles.map { it.close }.average()
        }

        val multiplier = 2.0 / (period + 1.0)
        var ema = candles.take(period).map { it.close }.average()

        for (i in period until candles.size) {
            ema = (candles[i].close - ema) * multiplier + ema
        }
        return ema
    }

    fun calculateVwap(candles: List<HistoricalCandle>): Double {
        if (candles.isEmpty()) return 0.0

        var cumulativeTypicalVolume = 0.0
        var cumulativeVolume = 0L

        for (candle in candles) {
            val typicalPrice = (candle.high + candle.low + candle.close) / 3.0
            cumulativeTypicalVolume += typicalPrice * candle.volume
            cumulativeVolume += candle.volume
        }

        return if (cumulativeVolume > 0) cumulativeTypicalVolume / cumulativeVolume else candles.last().close
    }

    data class MacdOutput(val macdLine: Double, val signalLine: Double, val histogram: Double)

    fun calculateMacd(
        candles: List<HistoricalCandle>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): MacdOutput {
        if (candles.size < slowPeriod + signalPeriod) {
            return MacdOutput(0.0, 0.0, 0.0)
        }

        val fastMultiplier = 2.0 / (fastPeriod + 1.0)
        val slowMultiplier = 2.0 / (slowPeriod + 1.0)

        var fastEma = candles.take(fastPeriod).map { it.close }.average()
        var slowEma = candles.take(slowPeriod).map { it.close }.average()

        // Track MACD values for signal line calculation
        val macdValues = mutableListOf<Double>()

        for (i in slowPeriod until candles.size) {
            fastEma = (candles[i].close - fastEma) * fastMultiplier + fastEma
            slowEma = (candles[i].close - slowEma) * slowMultiplier + slowEma
            macdValues.add(fastEma - slowEma)
        }

        if (macdValues.size < signalPeriod) {
            val lastMacd = macdValues.lastOrNull() ?: 0.0
            return MacdOutput(lastMacd, 0.0, lastMacd)
        }

        val signalMultiplier = 2.0 / (signalPeriod + 1.0)
        var signalLine = macdValues.take(signalPeriod).average()

        for (i in signalPeriod until macdValues.size) {
            signalLine = (macdValues[i] - signalLine) * signalMultiplier + signalLine
        }

        val currentMacd = macdValues.last()
        val histogram = currentMacd - signalLine
        return MacdOutput(currentMacd, signalLine, histogram)
    }

    fun calculateAtr(candles: List<HistoricalCandle>, period: Int = 14): Double {
        if (candles.size <= 1) return 15.0

        val trList = mutableListOf<Double>()
        for (i in 1 until candles.size) {
            val high = candles[i].high
            val low = candles[i].low
            val prevClose = candles[i - 1].close

            val tr = max(high - low, max(kotlin.math.abs(high - prevClose), kotlin.math.abs(low - prevClose)))
            trList.add(tr)
        }

        if (trList.size <= period) return trList.average()

        var atr = trList.take(period).average()
        for (i in period until trList.size) {
            atr = (atr * (period - 1) + trList[i]) / period
        }
        return atr
    }

    fun calculateOpeningRange15Min(candles: List<HistoricalCandle>): Pair<Double, Double> {
        // Typically the first 3 5-minute candles of the day (9:15 - 9:30 IST)
        val orbCandles = candles.take(3)
        if (orbCandles.isEmpty()) return Pair(0.0, 0.0)

        val orbHigh = orbCandles.maxOf { it.high }
        val orbLow = orbCandles.minOf { it.low }
        return Pair(orbHigh, orbLow)
    }

    fun calculateSupportResistance(pdh: Double, pdl: Double, pdc: Double): SupportResistanceLevels {
        val pivot = (pdh + pdl + pdc) / 3.0
        val r1 = (2.0 * pivot) - pdl
        val s1 = (2.0 * pivot) - pdh
        val r2 = pivot + (pdh - pdl)
        val s2 = pivot - (pdh - pdl)
        return SupportResistanceLevels(pivot, r1, r2, s1, s2, pdh, pdl)
    }

    data class SupertrendOutput(
        val value: Double,
        val isBullish: Boolean,
        val upperBand: Double = 0.0,
        val lowerBand: Double = 0.0
    )

    fun calculateSupertrend(
        candles: List<HistoricalCandle>,
        period: Int = 10,
        multiplier: Double = 3.0
    ): SupertrendOutput {
        if (candles.isEmpty()) return SupertrendOutput(0.0, isBullish = true, 0.0, 0.0)
        if (candles.size < period) {
            val lastClose = candles.last().close
            return SupertrendOutput(lastClose, isBullish = true, lastClose, lastClose)
        }

        val trList = mutableListOf<Double>()
        for (i in candles.indices) {
            if (i == 0) {
                trList.add(candles[i].high - candles[i].low)
            } else {
                val high = candles[i].high
                val low = candles[i].low
                val prevClose = candles[i - 1].close
                val tr = max(high - low, max(kotlin.math.abs(high - prevClose), kotlin.math.abs(low - prevClose)))
                trList.add(tr)
            }
        }

        val atrList = mutableListOf<Double>()
        var runningAtr = trList.take(period).average()
        for (i in 0 until period - 1) {
            atrList.add(runningAtr)
        }
        atrList.add(runningAtr)

        for (i in period until candles.size) {
            runningAtr = (runningAtr * (period - 1) + trList[i]) / period
            atrList.add(runningAtr)
        }

        var prevFinalUpper = 0.0
        var prevFinalLower = 0.0
        var prevSupertrend = 0.0
        var isBullish = true

        for (i in candles.indices) {
            val candle = candles[i]
            val atr = atrList[i]
            val hl2 = (candle.high + candle.low) / 2.0
            val basicUpper = hl2 + (multiplier * atr)
            val basicLower = hl2 - (multiplier * atr)

            val prevClose = if (i > 0) candles[i - 1].close else candle.close

            val finalUpper = if (i == 0 || basicUpper < prevFinalUpper || prevClose > prevFinalUpper) {
                basicUpper
            } else {
                prevFinalUpper
            }

            val finalLower = if (i == 0 || basicLower > prevFinalLower || prevClose < prevFinalLower) {
                basicLower
            } else {
                prevFinalLower
            }

            val currentSupertrend: Double
            if (i == 0) {
                isBullish = candle.close >= finalLower
                currentSupertrend = if (isBullish) finalLower else finalUpper
            } else {
                if (prevSupertrend == prevFinalUpper) {
                    if (candle.close > finalUpper) {
                        isBullish = true
                        currentSupertrend = finalLower
                    } else {
                        isBullish = false
                        currentSupertrend = finalUpper
                    }
                } else {
                    if (candle.close < finalLower) {
                        isBullish = false
                        currentSupertrend = finalUpper
                    } else {
                        isBullish = true
                        currentSupertrend = finalLower
                    }
                }
            }

            prevFinalUpper = finalUpper
            prevFinalLower = finalLower
            prevSupertrend = currentSupertrend
        }

        return SupertrendOutput(
            value = prevSupertrend,
            isBullish = isBullish,
            upperBand = prevFinalUpper,
            lowerBand = prevFinalLower
        )
    }

    fun evaluateIndicators(
        candles: List<HistoricalCandle>,
        pdh: Double,
        pdl: Double,
        pdc: Double,
        pcr: Double,
        vix: Double
    ): TechnicalIndicators {
        val lastPrice = candles.lastOrNull()?.close ?: 0.0
        val rsi = calculateRsi(candles, 14)
        val ema9 = calculateEma(candles, 9)
        val ema20 = calculateEma(candles, 20)
        val ema50 = calculateEma(candles, 50)
        val ema200 = calculateEma(candles, 200)
        val vwap = calculateVwap(candles)
        val macd = calculateMacd(candles)
        val atr = calculateAtr(candles, 14)
        val (orbHigh, orbLow) = calculateOpeningRange15Min(candles)

        val orbState = when {
            orbHigh > 0 && lastPrice > orbHigh -> OrbState.BREAKOUT_UP
            orbLow > 0 && lastPrice < orbLow -> OrbState.BREAKOUT_DOWN
            else -> OrbState.INSIDE_RANGE
        }

        // Direction determination based on multi-indicator alignment
        val bullishPoints = (if (lastPrice > vwap) 1 else 0) +
                (if (lastPrice > ema20) 1 else 0) +
                (if (ema20 > ema50) 1 else 0) +
                (if (rsi in 52.0..72.0) 1 else 0) +
                (if (macd.histogram > 0) 1 else 0) +
                (if (pcr > 1.10) 1 else 0)

        val bearishPoints = (if (lastPrice < vwap) 1 else 0) +
                (if (lastPrice < ema20) 1 else 0) +
                (if (ema20 < ema50) 1 else 0) +
                (if (rsi in 28.0..48.0) 1 else 0) +
                (if (macd.histogram < 0) 1 else 0) +
                (if (pcr < 0.90) 1 else 0)

        val direction = when {
            bullishPoints >= 4 && bearishPoints <= 1 -> MarketDirection.BULLISH
            bearishPoints >= 4 && bullishPoints <= 1 -> MarketDirection.BEARISH
            else -> MarketDirection.SIDEWAYS
        }

        return TechnicalIndicators(
            rsi14 = rsi,
            ema20 = ema20,
            ema50 = ema50,
            ema200 = ema200,
            vwap = vwap,
            macdLine = macd.macdLine,
            signalLine = macd.signalLine,
            macdHistogram = macd.histogram,
            atr14 = atr,
            pdh = pdh,
            pdl = pdl,
            pdc = pdc,
            orbHigh = orbHigh,
            orbLow = orbLow,
            orbState = orbState,
            pcr = pcr,
            vix = vix,
            marketDirection = direction,
            ema9 = ema9
        )
    }
}
