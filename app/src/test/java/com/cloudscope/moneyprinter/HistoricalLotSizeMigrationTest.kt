package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.options.ContractSpecificationService
import com.cloudscope.moneyprinter.domain.options.HistoricalLotSizeMigrator
import org.junit.Assert.*
import org.junit.Test

class HistoricalLotSizeMigrationTest {

    private fun createLegacyTrade(
        id: String = "LEGACY-01",
        entryPrice: Double = 100.0,
        actualExitPrice: Double? = 120.0,
        lots: Int = 1,
        lotSize: Int = 25,
        status: SignalLifecycleStatus = SignalLifecycleStatus.CLOSED,
        grossPnl: Double = 500.0,
        netPnl: Double = 420.0
    ): HistoricalSignalRecord {
        return HistoricalSignalRecord(
            id = id,
            timestamp = 1715000000000L, // May 2024
            spotPrice = 22500.0,
            marketDirection = MarketDirection.BULLISH,
            action = SignalAction.BUY_CE,
            contractSymbol = "NIFTY24MAY22500CE",
            strikePrice = 22500.0,
            optionType = OptionType.CE,
            expiryDate = "30-May-2024",
            premiumAtSignal = entryPrice,
            entryPrice = entryPrice,
            stopLoss = 80.0,
            target1 = 120.0,
            target2 = 140.0,
            target3 = 160.0,
            confidenceScore = 85,
            rsi14 = 59.0,
            vwap = 22480.0,
            ema9 = 22510.0,
            ema20 = 22490.0,
            ema50 = 22450.0,
            pcr = 1.1,
            vix = 13.0,
            reasons = listOf("Breakout"),
            invalidationConditions = "SL breach",
            status = status,
            outcome = TradeOutcome.WIN,
            grossPnl = grossPnl,
            netPnl = netPnl,
            lots = lots,
            lotSize = lotSize,
            actualExitPrice = actualExitPrice,
            exitReason = ExitReason.TARGET_1
        )
    }

    @Test
    fun testMigrateClosedTradeWithValidInputs() {
        val legacy = createLegacyTrade(
            entryPrice = 100.0,
            actualExitPrice = 120.0,
            lots = 1,
            lotSize = 25,
            grossPnl = 500.0, // (120 - 100) * 25
            netPnl = 425.0
        )

        val migrated = HistoricalLotSizeMigrator.migrateRecord(legacy)

        // Verifies new authorized lot size 65
        assertEquals(65, migrated.lotSize)
        assertEquals(65, migrated.totalQuantity)

        // Gross should be recalculated from original inputs: (120 - 100) * 65 = 1300
        assertEquals(1300.0, migrated.grossPnl, 0.01)

        // Net PnL must be recalculated and be greater than old net
        assertTrue(migrated.netPnl > legacy.netPnl)
        assertTrue(migrated.netPnl > 1150.0)

        // Audit preservation
        assertEquals(25, migrated.originalLotSize)
        assertEquals(500.0, migrated.originalGrossPnl ?: 0.0, 0.01)
        assertEquals(425.0, migrated.originalNetPnl ?: 0.0, 0.01)
        assertEquals(HistoricalLotSizeMigrator.STATUS_MIGRATED_LOT_SIZE_65, migrated.migrationStatus)
        assertNotNull(migrated.migrationNotes)
        assertTrue(migrated.migrationNotes!!.contains("authorized NIFTY lot size 65"))
    }

    @Test
    fun testMigrateMultipleLotsTrade() {
        // 2 Lots = 130 Qty
        val legacy = createLegacyTrade(
            entryPrice = 100.0,
            actualExitPrice = 115.0,
            lots = 2,
            lotSize = 25,
            grossPnl = 750.0, // (115 - 100) * 50
            netPnl = 640.0
        )

        val migrated = HistoricalLotSizeMigrator.migrateRecord(legacy)

        assertEquals(2, migrated.lots)
        assertEquals(65, migrated.lotSize)
        assertEquals(130, migrated.totalQuantity)
        // (115 - 100) * 130 = 1950
        assertEquals(1950.0, migrated.grossPnl, 0.01)
        assertEquals(25, migrated.originalLotSize)
        assertEquals(750.0, migrated.originalGrossPnl ?: 0.0, 0.01)
    }

    @Test
    fun testMigrateCancelledOrderSetsZeroFriction() {
        val cancelled = createLegacyTrade(
            status = SignalLifecycleStatus.CANCELLED,
            actualExitPrice = null,
            grossPnl = 0.0,
            netPnl = 0.0
        )

        val migrated = HistoricalLotSizeMigrator.migrateRecord(cancelled)

        assertEquals(65, migrated.lotSize)
        assertEquals(0.0, migrated.grossPnl, 0.01)
        assertEquals(0.0, migrated.netPnl, 0.01)
        assertEquals(HistoricalLotSizeMigrator.STATUS_MIGRATED_UNFILLED, migrated.migrationStatus)
    }

    @Test
    fun testMissingExecutionPriceFlagsManualReview() {
        val missingExit = createLegacyTrade(
            status = SignalLifecycleStatus.CLOSED,
            actualExitPrice = null, // Missing!
            grossPnl = 250.0,
            netPnl = 200.0
        )

        val migrated = HistoricalLotSizeMigrator.migrateRecord(missingExit)

        assertEquals(65, migrated.lotSize)
        // Original gross & net preserved without fabricating execution price
        assertEquals(250.0, migrated.originalGrossPnl ?: 0.0, 0.01)
        assertEquals(HistoricalLotSizeMigrator.STATUS_FLAGGED_MANUAL_REVIEW, migrated.migrationStatus)
        assertTrue(migrated.migrationNotes!!.contains("unavailable"))
    }

    @Test
    fun testAlreadyMigratedRecordNotModified() {
        val modern = createLegacyTrade(
            lotSize = 65,
            grossPnl = 1300.0,
            netPnl = 1210.0
        )

        val result = HistoricalLotSizeMigrator.migrateRecord(modern)

        assertSame(modern, result)
    }
}
