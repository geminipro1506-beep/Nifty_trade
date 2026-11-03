package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.signal.SignalOutcomeTracker
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SignalOutcomeTrackerTest {

    private fun createSamplePendingRecord(): HistoricalSignalRecord {
        return HistoricalSignalRecord(
            id = "TEST-SIG-01",
            timestamp = System.currentTimeMillis() - (10 * 60 * 1000L), // 10 min ago
            spotPrice = 24800.0,
            marketDirection = MarketDirection.BULLISH,
            action = SignalAction.BUY_CE,
            contractSymbol = "NIFTY 24800 CE",
            strikePrice = 24800.0,
            optionType = OptionType.CE,
            expiryDate = "Upcoming Weekly",
            premiumAtSignal = 120.0,
            entryPrice = 120.0,
            stopLoss = 90.0,
            target1 = 150.0,
            target2 = 180.0,
            target3 = 210.0,
            confidenceScore = 78,
            rsi14 = 58.2,
            vwap = 24780.0,
            ema9 = 24810.0,
            ema20 = 24795.0,
            ema50 = 24760.0,
            pcr = 1.15,
            vix = 13.8,
            reasons = listOf("Breakout above 15m ORB", "Supported above VWAP"),
            invalidationConditions = "Invalidated below VWAP",
            status = SignalLifecycleStatus.PENDING_ENTRY
        )
    }

    @Test
    fun testPendingEntryTransitionToActive() {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val morningCal = Calendar.getInstance(ist).apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }
        val morningTime = morningCal.timeInMillis

        val pending = createSamplePendingRecord().copy(
            timestamp = morningTime - (15 * 60 * 1000L) // generated 15m ago
        )

        // Market reaches entry price: LTP = 119.0 (at or below limit 120.0)
        val updated = SignalOutcomeTracker.evaluateSignal(
            record = pending,
            currentOptionLtp = 119.0,
            candleHigh = 121.0,
            candleLow = 118.0,
            currentTimestamp = morningTime
        )

        assertEquals(SignalLifecycleStatus.ACTIVE, updated.status)
        assertTrue(updated.entryMet)
        assertNotNull(updated.entryTimestamp)
    }

    @Test
    fun testTarget1HitClosesTradeWithWin() {
        val pending = createSamplePendingRecord()
        val active = pending.copy(
            status = SignalLifecycleStatus.ACTIVE,
            entryMet = true,
            entryTimestamp = System.currentTimeMillis() - (20 * 60 * 1000L)
        )

        // Candle touches Target 1 (150.0)
        val updated = SignalOutcomeTracker.evaluateSignal(
            record = active,
            currentOptionLtp = 152.0,
            candleHigh = 153.0,
            candleLow = 130.0
        )

        assertEquals(SignalLifecycleStatus.CLOSED, updated.status)
        assertEquals(TradeOutcome.WIN, updated.outcome)
        assertEquals(ExitReason.TARGET_1, updated.exitReason)
        assertEquals(150.0, updated.actualExitPrice!!, 0.01)
        assertTrue(updated.netPnl > 0.0)
    }

    @Test
    fun testTarget3HitCapturesMaxProfit() {
        val active = createSamplePendingRecord().copy(
            status = SignalLifecycleStatus.ACTIVE,
            entryMet = true,
            entryTimestamp = System.currentTimeMillis() - (35 * 60 * 1000L)
        )

        // Candle touches Target 3 (210.0)
        val updated = SignalOutcomeTracker.evaluateSignal(
            record = active,
            currentOptionLtp = 212.0,
            candleHigh = 215.0,
            candleLow = 140.0
        )

        assertEquals(SignalLifecycleStatus.CLOSED, updated.status)
        assertEquals(TradeOutcome.WIN, updated.outcome)
        assertEquals(ExitReason.TARGET_3, updated.exitReason)
        assertEquals(210.0, updated.actualExitPrice!!, 0.01)
        // Gross PnL: (210 - 120) * 65 = 5850.0 (Authorized lot size 65)
        assertEquals(5850.0, updated.grossPnl, 0.01)
        assertTrue(updated.netPnl > 5600.0)
    }

    @Test
    fun testStopLossHitClosesTradeWithLoss() {
        val active = createSamplePendingRecord().copy(
            status = SignalLifecycleStatus.ACTIVE,
            entryMet = true,
            entryTimestamp = System.currentTimeMillis() - (15 * 60 * 1000L)
        )

        // Candle breaches SL (90.0)
        val updated = SignalOutcomeTracker.evaluateSignal(
            record = active,
            currentOptionLtp = 88.0,
            candleHigh = 115.0,
            candleLow = 85.0
        )

        assertEquals(SignalLifecycleStatus.CLOSED, updated.status)
        assertEquals(TradeOutcome.LOSS, updated.outcome)
        assertEquals(ExitReason.STOP_LOSS, updated.exitReason)
        assertEquals(90.0, updated.actualExitPrice!!, 0.01)
        // Gross PnL: (90 - 120) * 65 = -1950.0 (Authorized lot size 65)
        assertEquals(-1950.0, updated.grossPnl, 0.01)
        assertTrue(updated.netPnl < -1950.0)
    }

    @Test
    fun testAmbiguousCandleHandlingStrictBreakeven() {
        val active = createSamplePendingRecord().copy(
            status = SignalLifecycleStatus.ACTIVE,
            entryMet = true,
            entryTimestamp = System.currentTimeMillis() - (5 * 60 * 1000L)
        )

        // Extreme candle: High touches Target 1 (155 >= 150) AND Low touches Stop Loss (85 <= 90) in SAME candle
        val updated = SignalOutcomeTracker.evaluateSignal(
            record = active,
            currentOptionLtp = 120.0,
            candleHigh = 155.0,
            candleLow = 85.0
        )

        // Must NOT assume favorable outcome!
        assertEquals(SignalLifecycleStatus.CLOSED, updated.status)
        assertEquals(TradeOutcome.AMBIGUOUS, updated.outcome)
        assertEquals(ExitReason.AMBIGUOUS, updated.exitReason)
        assertEquals(120.0, updated.actualExitPrice!!, 0.01) // Breakeven exit at entry
        assertEquals(0.0, updated.grossPnl, 0.01)
        assertTrue(updated.targetAndSlSameCandle)
        assertNotNull(updated.ambiguousReason)
        assertEquals("AMBIGUOUS_UNRESOLVED_EXCLUDED_FROM_WIN_RATE", updated.finalResolutionStatus)
        // Net PnL must be strictly negative due to transaction friction
        assertTrue(updated.netPnl < 0.0)
        assertTrue(updated.totalCharges > 0.0)
    }

    @Test
    fun testAmbiguousCandleResolvedByFinerSubCandleSequence() {
        val active = createSamplePendingRecord().copy(
            status = SignalLifecycleStatus.ACTIVE,
            entryMet = true,
            entryTimestamp = System.currentTimeMillis() - (10 * 60 * 1000L)
        )

        val now = System.currentTimeMillis()
        // Sub-candle sequence: 1m candle 1 touches SL (88 <= 90) before 1m candle 2 touches Target 1 (152 >= 150)
        val subCandles = listOf(
            HistoricalCandle(now - 120000, open = 115.0, high = 118.0, low = 88.0, close = 95.0, volume = 5000), // SL breached here!
            HistoricalCandle(now - 60000, open = 95.0, high = 153.0, low = 94.0, close = 145.0, volume = 8000)
        )

        val updated = SignalOutcomeTracker.evaluateSignal(
            record = active,
            currentOptionLtp = 145.0,
            candleHigh = 153.0,
            candleLow = 88.0,
            subCandles = subCandles
        )

        // Sub-candle resolved SL hit first!
        assertEquals(SignalLifecycleStatus.CLOSED, updated.status)
        assertEquals(TradeOutcome.LOSS, updated.outcome)
        assertEquals(ExitReason.STOP_LOSS, updated.exitReason)
        assertEquals("RESOLVED_VIA_SUBCANDLE_SEQUENCE", updated.finalResolutionStatus)
    }

    @Test
    fun testAutoSquareOffAt1515IST() {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(ist).apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 16)
        }

        val active = createSamplePendingRecord().copy(
            status = SignalLifecycleStatus.ACTIVE,
            entryMet = true,
            entryTimestamp = cal.timeInMillis - (45 * 60 * 1000L)
        )

        // Intraday time exit triggered
        val updated = SignalOutcomeTracker.evaluateSignal(
            record = active,
            currentOptionLtp = 135.0,
            candleHigh = 140.0,
            candleLow = 110.0,
            currentTimestamp = cal.timeInMillis
        )

        assertEquals(SignalLifecycleStatus.CLOSED, updated.status)
        assertEquals(ExitReason.SQUARE_OFF_1515, updated.exitReason)
        assertEquals(135.0, updated.actualExitPrice!!, 0.01)
    }
}
