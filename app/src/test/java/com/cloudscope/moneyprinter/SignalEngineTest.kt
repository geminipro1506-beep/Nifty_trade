package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.signal.SignalEngine
import org.junit.Assert.*
import org.junit.Test

class SignalEngineTest {

    private fun createQuote(spot: Double): MarketQuote {
        return MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = spot,
            change = 120.0,
            pChange = 0.5,
            open = spot - 100.0,
            high = spot + 20.0,
            low = spot - 110.0,
            close = spot,
            vwap = spot - 30.0,
            volume = 5000000L
        )
    }

    private fun createIndicators(
        direction: MarketDirection,
        rsi: Double = 62.0,
        vix: Double = 14.5
    ): TechnicalIndicators {
        return TechnicalIndicators(
            rsi14 = rsi,
            ema20 = 24900.0,
            ema50 = 24850.0,
            ema200 = 24600.0,
            vwap = 24880.0,
            macdLine = 15.0,
            signalLine = 10.0,
            macdHistogram = 5.0,
            atr14 = 25.0,
            pdh = 25000.0,
            pdl = 24700.0,
            pdc = 24850.0,
            orbHigh = 24920.0,
            orbLow = 24800.0,
            orbState = if (direction == MarketDirection.BULLISH) OrbState.BREAKOUT_UP else OrbState.BREAKOUT_DOWN,
            pcr = if (direction == MarketDirection.BULLISH) 1.25 else 0.75,
            vix = vix,
            marketDirection = direction
        )
    }

    @Test
    fun testBullishSignalGeneration() {
        val quote = createQuote(24950.0)
        val indicators = createIndicators(MarketDirection.BULLISH, rsi = 62.0, vix = 14.0)

        val signal = SignalEngine.generateSignal(
            quote = quote,
            indicators = indicators,
            optionChain = null,
            candleCount = 30,
            isMarketHours = true
        )

        assertEquals(SignalAction.BUY_CE, signal.action)
        assertEquals(OptionType.CE, signal.optionType)
        assertTrue("Confidence must be calibrated and >= 62%", signal.confidencePercent >= 62)
        assertEquals(SignalStatus.ACTIVE, signal.status)
        assertTrue(signal.confirmationReasons.isNotEmpty())
    }

    @Test
    fun testSidewaysMarket_YieldsNoTrade() {
        val quote = createQuote(24800.0)
        val indicators = createIndicators(MarketDirection.SIDEWAYS)

        val signal = SignalEngine.generateSignal(
            quote = quote,
            indicators = indicators,
            optionChain = null,
            candleCount = 30,
            isMarketHours = true
        )

        assertEquals(SignalAction.NO_TRADE, signal.action)
    }

    @Test
    fun testExtremeVix_YieldsNoTrade() {
        val quote = createQuote(24950.0)
        // India VIX at 28.0 = elevated gamma risk
        val indicators = createIndicators(MarketDirection.BULLISH, vix = 28.0)

        val signal = SignalEngine.generateSignal(
            quote = quote,
            indicators = indicators,
            optionChain = null,
            candleCount = 30,
            isMarketHours = true
        )

        assertEquals(SignalAction.NO_TRADE, signal.action)
    }

    @Test
    fun testMarketClosed_YieldsNoTrade() {
        val quote = createQuote(24950.0)
        val indicators = createIndicators(MarketDirection.BULLISH)

        val signal = SignalEngine.generateSignal(
            quote = quote,
            indicators = indicators,
            optionChain = null,
            candleCount = 30,
            isMarketHours = false // Market closed
        )

        assertEquals(SignalAction.NO_TRADE, signal.action)
    }
}
