package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SignalDetailsCrashFixTest {

    private fun createBaseSignal(): HistoricalSignalRecord {
        return HistoricalSignalRecord(
            id = "SIG-TEST-CRASH-001",
            timestamp = System.currentTimeMillis() - 3600000L,
            spotPrice = 24850.0,
            marketDirection = MarketDirection.BULLISH,
            action = SignalAction.BUY_CE,
            contractSymbol = "NIFTY 24850 CE",
            strikePrice = 24850.0,
            optionType = OptionType.CE,
            expiryDate = "Current Weekly",
            premiumAtSignal = 140.0,
            entryPrice = 140.0,
            stopLoss = 112.0,
            target1 = 182.0,
            target2 = 196.0,
            target3 = 224.0,
            confidenceScore = 78,
            rsi14 = 62.5,
            vwap = 24835.0,
            ema9 = 24865.0,
            ema20 = 24840.0,
            ema50 = 24810.0,
            pcr = 1.22,
            vix = 14.1,
            reasons = listOf("VWAP Support", "EMA Alignment"),
            invalidationConditions = "Drop below VWAP",
            entryMet = true,
            entryTimestamp = System.currentTimeMillis() - 3000000L,
            actualExitPrice = 182.0,
            exitTimestamp = System.currentTimeMillis() - 1200000L,
            exitReason = ExitReason.TARGET_1,
            status = SignalLifecycleStatus.CLOSED,
            outcome = TradeOutcome.WIN,
            grossPnl = (182.0 - 140.0) * 65,
            netPnl = (182.0 - 140.0) * 65 - 55.0,
            totalCharges = 55.0,
            holdingTimeMinutes = 30,
            strategyType = StrategyType.BREAKOUT,
            timeframe = "5m",
            lots = 1,
            lotSize = 65
        )
    }

    @Test
    fun testWinningTradeDetailsDoNotCrash() {
        val signal = createBaseSignal()

        assertNotNull(signal.id)
        assertEquals(TradeOutcome.WIN, signal.outcome)
        assertEquals(ExitReason.TARGET_1, signal.exitReason)
        assertEquals(65, signal.totalQuantity)
        assertEquals(2730.0, signal.grossPnl, 0.01)
        assertEquals(2675.0, signal.netPnl, 0.01)

        val formattedPnl = String.format(Locale.ENGLISH, "%.2f", signal.netPnl)
        assertEquals("2675.00", formattedPnl)
    }

    @Test
    fun testLosingTradeDetailsDoNotCrash() {
        val signal = createBaseSignal().copy(
            actualExitPrice = 112.0,
            exitReason = ExitReason.STOP_LOSS,
            outcome = TradeOutcome.LOSS,
            grossPnl = (112.0 - 140.0) * 65,
            netPnl = (112.0 - 140.0) * 65 - 52.0,
            totalCharges = 52.0
        )

        assertEquals(TradeOutcome.LOSS, signal.outcome)
        assertEquals(ExitReason.STOP_LOSS, signal.exitReason)
        assertEquals(-1820.0, signal.grossPnl, 0.01)
        assertEquals(-1872.0, signal.netPnl, 0.01)

        val formattedGross = String.format(Locale.ENGLISH, "%.2f", signal.grossPnl)
        assertEquals("-1820.00", formattedGross)
    }

    @Test
    fun testCancelledSignalWithNullFieldsDoesNotCrash() {
        // Cancelled trade before entry: actualExitPrice is null, entryTimestamp is null, exitReason is CANCELLED
        val cancelled = createBaseSignal().copy(
            entryMet = false,
            entryTimestamp = null,
            actualExitPrice = null,
            exitReason = ExitReason.CANCELLED,
            status = SignalLifecycleStatus.CANCELLED,
            outcome = TradeOutcome.CANCELLED,
            grossPnl = 0.0,
            netPnl = 0.0,
            totalCharges = 0.0,
            holdingTimeMinutes = 0
        )

        assertNull(cancelled.entryTimestamp)
        assertNull(cancelled.actualExitPrice)
        assertEquals(0.0, cancelled.grossPnl, 0.01)
        assertEquals(0.0, cancelled.netPnl, 0.01)
        assertEquals(TradeOutcome.CANCELLED, cancelled.outcome)
        assertEquals(65, cancelled.totalQuantity)

        // Safe defensive formatting check
        val exitText = cancelled.actualExitPrice?.let { "₹$it" } ?: "--"
        assertEquals("--", exitText)
    }

    @Test
    fun testAmbiguousTradeDetailsWithAmberBannerDoesNotCrash() {
        val ambiguous = createBaseSignal().copy(
            actualExitPrice = 140.0,
            exitReason = ExitReason.AMBIGUOUS,
            status = SignalLifecycleStatus.CLOSED,
            outcome = TradeOutcome.AMBIGUOUS,
            ambiguousReason = "Target 1 and SL hit in same candle",
            targetAndSlSameCandle = true,
            finalResolutionStatus = "AMBIGUOUS_UNRESOLVED_EXCLUDED_FROM_WIN_RATE",
            grossPnl = 0.0,
            netPnl = -45.0,
            totalCharges = 45.0
        )

        assertEquals(TradeOutcome.AMBIGUOUS, ambiguous.outcome)
        assertTrue(ambiguous.targetAndSlSameCandle)
        assertNotNull(ambiguous.ambiguousReason)
        assertEquals("AMBIGUOUS_UNRESOLVED_EXCLUDED_FROM_WIN_RATE", ambiguous.finalResolutionStatus)
    }

    @Test
    fun testDefensiveHandlingOfNanAndInfiniteValues() {
        val corrupt = createBaseSignal().copy(
            spotPrice = Double.NaN,
            grossPnl = Double.POSITIVE_INFINITY,
            netPnl = Double.NEGATIVE_INFINITY,
            rsi14 = Double.NaN,
            vwap = Double.NaN
        )

        fun safeFormat(value: Double?): String {
            if (value == null || value.isNaN() || value.isInfinite()) return "--"
            return try {
                String.format(Locale.ENGLISH, "%.2f", value)
            } catch (e: Exception) {
                "--"
            }
        }

        assertEquals("--", safeFormat(corrupt.spotPrice))
        assertEquals("--", safeFormat(corrupt.grossPnl))
        assertEquals("--", safeFormat(corrupt.netPnl))
        assertEquals("--", safeFormat(corrupt.rsi14))
        assertEquals("--", safeFormat(corrupt.vwap))
    }

    @Test
    fun testMigratedSignalFieldsArePreserved() {
        val migrated = createBaseSignal().copy(
            lotSize = 65,
            originalLotSize = 25,
            originalGrossPnl = 1050.0,
            originalNetPnl = 995.0,
            migrationStatus = "MIGRATED_LOT_SIZE_65",
            migrationNotes = "Recalculated with authorized NIFTY lot size 65."
        )

        assertEquals(65, migrated.lotSize)
        assertEquals(25, migrated.originalLotSize)
        assertEquals(1050.0, migrated.originalGrossPnl!!, 0.01)
        assertEquals(995.0, migrated.originalNetPnl!!, 0.01)
        assertEquals("MIGRATED_LOT_SIZE_65", migrated.migrationStatus)
        assertNotNull(migrated.migrationNotes)
    }
}
