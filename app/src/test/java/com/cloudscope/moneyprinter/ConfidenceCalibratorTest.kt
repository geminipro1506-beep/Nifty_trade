package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.signal.ConfidenceCalibrator
import org.junit.Assert.*
import org.junit.Test

class ConfidenceCalibratorTest {

    private fun createMockIndicators(
        direction: MarketDirection,
        rsi: Double,
        pcr: Double,
        vix: Double,
        orbState: OrbState
    ): TechnicalIndicators {
        return TechnicalIndicators(
            rsi14 = rsi,
            ema20 = if (direction == MarketDirection.BULLISH) 24900.0 else 24700.0,
            ema50 = if (direction == MarketDirection.BULLISH) 24850.0 else 24750.0,
            ema200 = 24600.0,
            vwap = if (direction == MarketDirection.BULLISH) 24880.0 else 24720.0,
            macdLine = if (direction == MarketDirection.BULLISH) 15.0 else -15.0,
            signalLine = 10.0,
            macdHistogram = if (direction == MarketDirection.BULLISH) 5.0 else -5.0,
            atr14 = 25.0,
            pdh = 25000.0,
            pdl = 24700.0,
            pdc = 24850.0,
            orbHigh = 24920.0,
            orbLow = 24800.0,
            orbState = orbState,
            pcr = pcr,
            vix = vix,
            marketDirection = direction
        )
    }

    @Test
    fun testDataSufficiencyFallback() {
        val indicators = createMockIndicators(MarketDirection.BULLISH, 60.0, 1.25, 14.0, OrbState.BREAKOUT_UP)
        // With fewer than 15 candles, must return LOW confidence
        val (prob, rating) = ConfidenceCalibrator.calculateCalibratedProbability(
            direction = MarketDirection.BULLISH,
            indicators = indicators,
            optionChain = null,
            candleCount = 8
        )
        assertEquals(ConfidenceRating.LOW, rating)
        assertTrue(prob <= 50)
    }

    @Test
    fun testHighProbabilityBullishSetup() {
        val indicators = createMockIndicators(MarketDirection.BULLISH, 62.0, 1.35, 14.0, OrbState.BREAKOUT_UP)
        val chain = OptionChain(
            spotPrice = 24950.0,
            atmStrike = 24950.0,
            pcr = 1.35,
            maxPainStrike = 24950.0,
            totalCallOi = 1000000L,
            totalPutOi = 1350000L,
            totalCallOiChange = 50000L,
            totalPutOiChange = 250000L, // Massive put addition
            rows = emptyList()
        )

        val (prob, rating) = ConfidenceCalibrator.calculateCalibratedProbability(
            direction = MarketDirection.BULLISH,
            indicators = indicators,
            optionChain = chain,
            candleCount = 35
        )

        assertTrue("Confidence probability should be >= 75%: $prob%", prob >= 75)
        assertEquals(ConfidenceRating.HIGH, rating)
    }

    @Test
    fun testExtremeVixPenalty() {
        // High VIX > 22 should reduce confidence
        val indicators = createMockIndicators(MarketDirection.BULLISH, 60.0, 1.20, 24.0, OrbState.BREAKOUT_UP)
        val (prob, _) = ConfidenceCalibrator.calculateCalibratedProbability(
            direction = MarketDirection.BULLISH,
            indicators = indicators,
            optionChain = null,
            candleCount = 30
        )
        assertTrue("Elevated VIX should lower confidence below 72%: $prob%", prob < 72)
    }
}
