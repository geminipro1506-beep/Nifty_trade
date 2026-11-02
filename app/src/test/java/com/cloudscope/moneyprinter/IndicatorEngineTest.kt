package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.HistoricalCandle
import com.cloudscope.moneyprinter.data.model.MarketDirection
import com.cloudscope.moneyprinter.data.model.OrbState
import com.cloudscope.moneyprinter.domain.indicator.IndicatorEngine
import org.junit.Assert.*
import org.junit.Test

class IndicatorEngineTest {

    private fun generateMockCandles(prices: List<Double>, volume: Long = 100000L): List<HistoricalCandle> {
        return prices.mapIndexed { index, price ->
            HistoricalCandle(
                timestamp = 1730432700000L + (index * 300000L),
                open = price - 2.0,
                high = price + 5.0,
                low = price - 5.0,
                close = price,
                volume = volume
            )
        }
    }

    @Test
    fun testRsiCalculation_UpwardTrend() {
        // Continuous upward series
        val prices = (1..30).map { 24000.0 + (it * 10.0) }
        val candles = generateMockCandles(prices)
        val rsi = IndicatorEngine.calculateRsi(candles, 14)
        assertTrue("RSI should be high on upward trend: $rsi", rsi > 70.0)
    }

    @Test
    fun testRsiCalculation_DownwardTrend() {
        // Continuous downward series
        val prices = (1..30).map { 25000.0 - (it * 10.0) }
        val candles = generateMockCandles(prices)
        val rsi = IndicatorEngine.calculateRsi(candles, 14)
        assertTrue("RSI should be low on downward trend: $rsi", rsi < 30.0)
    }

    @Test
    fun testEmaCalculation() {
        val prices = (1..25).map { 24500.0 + it }
        val candles = generateMockCandles(prices)
        val ema20 = IndicatorEngine.calculateEma(candles, 20)
        assertTrue("EMA20 should be near the recent prices", ema20 in 24510.0..24530.0)
    }

    @Test
    fun testVwapCalculation() {
        val c1 = HistoricalCandle(1000L, 24000.0, 24010.0, 23990.0, 24000.0, 100000L) // TP = 24000
        val c2 = HistoricalCandle(2000L, 24100.0, 24110.0, 24090.0, 24100.0, 200000L) // TP = 24100
        val vwap = IndicatorEngine.calculateVwap(listOf(c1, c2))
        // Expected: (24000 * 100k + 24100 * 200k) / 300k = (2400000k + 4820000k) / 300k = 7220000 / 300 = 24066.67
        assertEquals(24066.67, vwap, 0.5)
    }

    @Test
    fun testMacdOutput() {
        val prices = (1..40).map { 24000.0 + (it * 5.0) }
        val candles = generateMockCandles(prices)
        val macd = IndicatorEngine.calculateMacd(candles)
        assertTrue("Histogram should be positive in steady uptrend", macd.histogram > 0.0)
    }

    @Test
    fun testOpeningRangeBreakout() {
        // First 3 candles define ORB (High: 24820, Low: 24780)
        val c1 = HistoricalCandle(1000L, 24800.0, 24810.0, 24790.0, 24805.0, 100000L)
        val c2 = HistoricalCandle(2000L, 24805.0, 24820.0, 24795.0, 24815.0, 120000L)
        val c3 = HistoricalCandle(3000L, 24815.0, 24818.0, 24780.0, 24800.0, 110000L)
        val (orbHigh, orbLow) = IndicatorEngine.calculateOpeningRange15Min(listOf(c1, c2, c3))

        assertEquals(24820.0, orbHigh, 0.01)
        assertEquals(24780.0, orbLow, 0.01)
    }

    @Test
    fun testSupportResistancePivotLevels() {
        val levels = IndicatorEngine.calculateSupportResistance(pdh = 25000.0, pdl = 24800.0, pdc = 24900.0)
        // Pivot = (25000 + 24800 + 24900) / 3 = 24900
        assertEquals(24900.0, levels.pivot, 0.01)
        // R1 = 2 * 24900 - 24800 = 25000
        assertEquals(25000.0, levels.r1, 0.01)
        // S1 = 2 * 24900 - 25000 = 24800
        assertEquals(24800.0, levels.s1, 0.01)
        // R2 = 24900 + 200 = 25100
        assertEquals(25100.0, levels.r2, 0.01)
        // S2 = 24900 - 200 = 24700
        assertEquals(24700.0, levels.s2, 0.01)
    }
}
