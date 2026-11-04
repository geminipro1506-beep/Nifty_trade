package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.alert.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MarketAlertEngineTest {

    private lateinit var engine: MarketAlertEngine

    @Before
    fun setUp() {
        engine = MarketAlertEngine()
    }

    private fun mockQuote(price: Double, timestamp: Long = System.currentTimeMillis()): MarketQuote {
        return MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = price,
            change = 25.0,
            pChange = 0.1,
            open = 24800.0,
            high = 24900.0,
            low = 24750.0,
            close = 24800.0,
            vwap = 24800.0,
            volume = 1500000L,
            timestamp = timestamp
        )
    }

    private fun mockIndicators(
        direction: MarketDirection = MarketDirection.SIDEWAYS,
        vwap: Double = 24800.0,
        rsi: Double = 50.0,
        macdHist: Double = 0.0,
        ema9: Double = 24810.0,
        ema20: Double = 24800.0,
        vix: Double = 14.5
    ): TechnicalIndicators {
        return TechnicalIndicators(
            rsi14 = rsi,
            ema20 = ema20,
            ema50 = 24750.0,
            ema200 = 24500.0,
            vwap = vwap,
            macdLine = 5.0,
            signalLine = 5.0 - macdHist,
            macdHistogram = macdHist,
            atr14 = 25.0,
            pdh = 24900.0,
            pdl = 24700.0,
            pdc = 24800.0,
            orbHigh = 24850.0,
            orbLow = 24780.0,
            orbState = OrbState.INSIDE_RANGE,
            pcr = 1.05,
            vix = vix,
            marketDirection = direction,
            ema9 = ema9
        )
    }

    private fun mockSignal(action: SignalAction = SignalAction.NO_TRADE, confidence: Int = 50, direction: MarketDirection = MarketDirection.SIDEWAYS): TradingSignal {
        return TradingSignal.noTrade("Test signal").copy(
            action = action,
            confidencePercent = confidence,
            marketDirection = direction
        )
    }

    @Test
    fun testDirectionShiftTriggersAlert() {
        val now = 10000000L

        // Step 1: Baseline established with SIDEWAYS
        val firstAlerts = engine.evaluate(
            quote = mockQuote(24800.0, now),
            indicators = mockIndicators(direction = MarketDirection.SIDEWAYS),
            signal = mockSignal(confidence = 50, direction = MarketDirection.SIDEWAYS),
            now = now
        )
        assertTrue(firstAlerts.isEmpty())

        // Step 2: Direction shifts from SIDEWAYS to BULLISH
        val secondAlerts = engine.evaluate(
            quote = mockQuote(24850.0, now + 1000),
            indicators = mockIndicators(direction = MarketDirection.BULLISH),
            signal = mockSignal(action = SignalAction.BUY_CE, confidence = 75, direction = MarketDirection.BULLISH),
            now = now + 1000
        )

        val dirAlert = secondAlerts.find { it.type == MarketAlertType.DIRECTION_BULLISH }
        assertNotNull("Expected DIRECTION_BULLISH alert", dirAlert)
        assertEquals(MarketDirection.SIDEWAYS, dirAlert?.previousDirection)
        assertEquals(MarketDirection.BULLISH, dirAlert?.newDirection)
        assertEquals(24850.0, dirAlert!!.spotPrice, 0.01)
    }

    @Test
    fun testDirectionBearishShiftTriggersAlert() {
        val now = 10000000L

        // Baseline BULLISH
        engine.evaluate(
            quote = mockQuote(24900.0, now),
            indicators = mockIndicators(direction = MarketDirection.BULLISH),
            signal = mockSignal(confidence = 75, direction = MarketDirection.BULLISH),
            now = now
        )

        // Shifts to BEARISH
        val alerts = engine.evaluate(
            quote = mockQuote(24750.0, now + 2000),
            indicators = mockIndicators(direction = MarketDirection.BEARISH),
            signal = mockSignal(action = SignalAction.BUY_PE, confidence = 72, direction = MarketDirection.BEARISH),
            now = now + 2000
        )

        val bearishAlert = alerts.find { it.type == MarketAlertType.DIRECTION_BEARISH }
        assertNotNull(bearishAlert)
        assertEquals(MarketDirection.BULLISH, bearishAlert?.previousDirection)
        assertEquals(MarketDirection.BEARISH, bearishAlert?.newDirection)
    }

    @Test
    fun testConfidenceIncreaseAndDecreaseAlerts() {
        val now = 10000000L

        // Baseline: 60% confidence
        engine.evaluate(
            quote = mockQuote(24800.0, now),
            indicators = mockIndicators(),
            signal = mockSignal(confidence = 60),
            now = now
        )

        // Case A: Confidence increases by +15% (threshold is 10%)
        val increaseAlerts = engine.evaluate(
            quote = mockQuote(24820.0, now + 5000),
            indicators = mockIndicators(),
            signal = mockSignal(confidence = 75),
            now = now + 5000
        )
        val confAlert = increaseAlerts.find { it.type == MarketAlertType.CONFIDENCE_INCREASE }
        assertNotNull(confAlert)
        assertEquals(60, confAlert?.previousConfidence)
        assertEquals(75, confAlert?.newConfidence)

        // Case B: Small change (+3%) should NOT trigger
        val smallChangeAlerts = engine.evaluate(
            quote = mockQuote(24825.0, now + 10000),
            indicators = mockIndicators(),
            signal = mockSignal(confidence = 78),
            now = now + 10000
        )
        assertNull(smallChangeAlerts.find { it.type == MarketAlertType.CONFIDENCE_INCREASE })

        // Case C: Confidence drops by 18% (78 -> 60)
        val dropAlerts = engine.evaluate(
            quote = mockQuote(24790.0, now + 15000),
            indicators = mockIndicators(),
            signal = mockSignal(confidence = 60),
            now = now + 15000
        )
        val dropAlert = dropAlerts.find { it.type == MarketAlertType.CONFIDENCE_DECREASE }
        assertNotNull(dropAlert)
        assertEquals(78, dropAlert?.previousConfidence)
        assertEquals(60, dropAlert?.newConfidence)
    }

    @Test
    fun testVwapBreakoutAndBreakdown() {
        val now = 10000000L

        // Baseline: Price below VWAP (24790 vs 24800)
        engine.evaluate(
            quote = mockQuote(24790.0, now),
            indicators = mockIndicators(vwap = 24800.0),
            signal = mockSignal(),
            now = now
        )

        // Price crosses above VWAP
        val breakoutAlerts = engine.evaluate(
            quote = mockQuote(24810.0, now + 2000),
            indicators = mockIndicators(vwap = 24800.0),
            signal = mockSignal(),
            now = now + 2000
        )
        val breakout = breakoutAlerts.find { it.type == MarketAlertType.VWAP_BREAKOUT }
        assertNotNull(breakout)
        assertTrue(breakout?.title?.contains("VWAP") == true)

        // Price crosses back down below VWAP after cooldown
        val laterNow = now + 400000L // 6.6 min later
        val breakdownAlerts = engine.evaluate(
            quote = mockQuote(24785.0, laterNow),
            indicators = mockIndicators(vwap = 24800.0),
            signal = mockSignal(),
            now = laterNow
        )
        val breakdown = breakdownAlerts.find { it.type == MarketAlertType.VWAP_BREAKDOWN }
        assertNotNull(breakdown)
    }

    @Test
    fun testRsiOverboughtAndOversold() {
        val now = 10000000L

        // Baseline RSI: 65.0
        engine.evaluate(
            quote = mockQuote(24800.0, now),
            indicators = mockIndicators(rsi = 65.0),
            signal = mockSignal(),
            now = now
        )

        // RSI rises to 72.5 (Overbought >= 70)
        val obAlerts = engine.evaluate(
            quote = mockQuote(24850.0, now + 1000),
            indicators = mockIndicators(rsi = 72.5),
            signal = mockSignal(),
            now = now + 1000
        )
        val obAlert = obAlerts.find { it.type == MarketAlertType.RSI_OVERBOUGHT }
        assertNotNull(obAlert)

        // Cooldown passes, RSI drops to 28.0 (Oversold <= 30)
        val laterNow = now + 300000L
        val osAlerts = engine.evaluate(
            quote = mockQuote(24650.0, laterNow),
            indicators = mockIndicators(rsi = 28.0),
            signal = mockSignal(),
            now = laterNow
        )
        val osAlert = osAlerts.find { it.type == MarketAlertType.RSI_OVERSOLD }
        assertNotNull(osAlert)
    }

    @Test
    fun testMacdBullishCross() {
        val now = 10000000L

        // Baseline: MACD histogram negative (-2.0)
        engine.evaluate(
            quote = mockQuote(24800.0, now),
            indicators = mockIndicators(macdHist = -2.0),
            signal = mockSignal(),
            now = now
        )

        // MACD turns positive (+1.5)
        val alerts = engine.evaluate(
            quote = mockQuote(24830.0, now + 1000),
            indicators = mockIndicators(macdHist = 1.5),
            signal = mockSignal(),
            now = now + 1000
        )
        val macdAlert = alerts.find { it.type == MarketAlertType.MACD_BULLISH_CROSS }
        assertNotNull(macdAlert)
    }

    @Test
    fun testCooldownPreventsDuplicateSpam() {
        val now = 10000000L
        val settings = AlertSettings(cooldownMinutes = 3)

        // Baseline SIDEWAYS
        engine.evaluate(
            quote = mockQuote(24800.0, now),
            indicators = mockIndicators(direction = MarketDirection.SIDEWAYS),
            signal = mockSignal(),
            settings = settings,
            now = now
        )

        // First trigger: Bullish Turn
        val firstAlerts = engine.evaluate(
            quote = mockQuote(24850.0, now + 1000),
            indicators = mockIndicators(direction = MarketDirection.BULLISH),
            signal = mockSignal(),
            settings = settings,
            now = now + 1000
        )
        assertEquals(1, firstAlerts.count { it.type == MarketAlertType.DIRECTION_BULLISH })

        // 1 minute later: same Bullish event (suppressed by 3m cooldown)
        val suppressedAlerts = engine.evaluate(
            quote = mockQuote(24860.0, now + 61000L),
            indicators = mockIndicators(direction = MarketDirection.BULLISH),
            signal = mockSignal(),
            settings = settings,
            now = now + 61000L
        )
        assertEquals(0, suppressedAlerts.count { it.type == MarketAlertType.DIRECTION_BULLISH })

        // 4 minutes later: cooldown expired
        val newShiftTime = now + 250000L // > 3 min
        // First flip to sideways
        engine.evaluate(
            quote = mockQuote(24820.0, newShiftTime),
            indicators = mockIndicators(direction = MarketDirection.SIDEWAYS),
            signal = mockSignal(),
            settings = settings,
            now = newShiftTime
        )
        // Then flip to bullish again
        val afterCooldownAlerts = engine.evaluate(
            quote = mockQuote(24870.0, newShiftTime + 1000L),
            indicators = mockIndicators(direction = MarketDirection.BULLISH),
            signal = mockSignal(),
            settings = settings,
            now = newShiftTime + 1000L
        )
        assertEquals(1, afterCooldownAlerts.count { it.type == MarketAlertType.DIRECTION_BULLISH })
    }

    @Test
    fun testStaleDataRejection() {
        val now = 10000000L
        // Quote timestamp is 6 minutes older than evaluation time
        val staleTimestamp = now - 360000L
        val staleQuote = mockQuote(24800.0, timestamp = staleTimestamp)

        val alerts = engine.evaluate(
            quote = staleQuote,
            indicators = mockIndicators(direction = MarketDirection.BULLISH),
            signal = mockSignal(),
            now = now
        )
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun testMasterNotificationsToggleOff() {
        val now = 10000000L
        val disabledSettings = AlertSettings(notificationsEnabled = false)

        engine.evaluate(
            quote = mockQuote(24800.0, now),
            indicators = mockIndicators(direction = MarketDirection.SIDEWAYS),
            signal = mockSignal(),
            settings = disabledSettings,
            now = now
        )

        val alerts = engine.evaluate(
            quote = mockQuote(24900.0, now + 1000),
            indicators = mockIndicators(direction = MarketDirection.BULLISH),
            signal = mockSignal(),
            settings = disabledSettings,
            now = now + 1000
        )
        assertTrue(alerts.isEmpty())
    }
}
