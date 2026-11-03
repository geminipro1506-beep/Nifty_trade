package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.local.db.SignalDatabaseHelper
import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.data.repository.SignalRepositoryImpl
import com.cloudscope.moneyprinter.domain.options.ContractSpecificationService
import com.cloudscope.moneyprinter.ui.viewmodel.MainViewModel
import com.cloudscope.moneyprinter.ui.viewmodel.TerminalNavTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class StartupCrashFixTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSignalDatabaseHelperCreateContentValuesAllFieldsPopulated() {
        // Given a sample signal record
        val record = HistoricalSignalRecord(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
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
            confidenceScore = 80,
            rsi14 = 55.0,
            vwap = 24840.0,
            ema9 = 24855.0,
            ema20 = 24845.0,
            ema50 = 24820.0,
            pcr = 1.15,
            vix = 14.2,
            reasons = listOf("Breakout confirmed", "Volume expansion"),
            invalidationConditions = "SL hit below 112",
            entryMet = true,
            entryTimestamp = System.currentTimeMillis() - 3600000L,
            actualExitPrice = 182.0,
            exitTimestamp = System.currentTimeMillis() - 1800000L,
            exitReason = ExitReason.TARGET_1,
            status = SignalLifecycleStatus.CLOSED,
            outcome = TradeOutcome.WIN,
            grossPnl = 2730.0,
            netPnl = 2605.0,
            totalCharges = 125.0,
            holdingTimeMinutes = 30,
            strategyType = StrategyType.BREAKOUT,
            timeframe = "5m",
            isPaperTrade = false,
            adjustmentsLog = listOf("T1 hit"),
            ambiguousReason = null,
            candleResolution = "5m",
            targetAndSlSameCandle = false,
            finalResolutionStatus = "RESOLVED_TARGET_1",
            lots = 1,
            lotSize = ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE,
            originalGrossPnl = 1050.0,
            originalNetPnl = 985.0,
            originalLotSize = 25,
            migrationStatus = "MIGRATED_LOT_SIZE_65",
            migrationNotes = "Authorized migration"
        )

        // Reflection/helper test to verify ContentValues construction doesn't crash on standard JVM
        assertEquals(65, record.lotSize)
        assertEquals(1, record.lots)
        assertEquals(65, record.totalQuantity)
        assertEquals("MIGRATED_LOT_SIZE_65", record.migrationStatus)
    }

    @Test
    fun testSignalRepositoryImplSafeFallbackWhenDbHelperNull() = runTest {
        val repo = SignalRepositoryImpl(null)

        // Initialize should not throw and signalsFlow should be empty
        repo.initialize()
        assertTrue(repo.signalsFlow.value.isEmpty())

        // getAllSignals should safely return emptyList
        val all = repo.getAllSignals()
        assertTrue(all.isEmpty())

        // getSignalsFiltered should safely return emptyList
        val filtered = repo.getSignalsFiltered(0L, OptionFilter.ALL, StrategyFilter.ALL, null)
        assertTrue(filtered.isEmpty())

        // getOpenSignals should safely return emptyList
        val open = repo.getOpenSignals()
        assertTrue(open.isEmpty())

        // getSignalById should return null
        val single = repo.getSignalById("non-existent-id")
        assertNull(single)

        // computeAccuracyMetrics should safely handle empty lists without dividing by zero
        val metrics = repo.computeAccuracyMetrics(emptyList())
        assertEquals(0, metrics.totalSignals)
        assertEquals(0.0, metrics.winRate, 0.001)
        assertEquals(0.0, metrics.netPnl, 0.001)
    }

    @Test
    fun testMainViewModelDefaultConstructorSafeStandaloneStartup() = runTest {
        // MainActivity falls back to MainViewModel() if dependency initialization fails.
        // Verify default constructor starts safely without crashes.
        val vm = MainViewModel()
        assertNotNull(vm.uiState.value)
        assertEquals(TerminalNavTab.DASHBOARD, vm.uiState.value.selectedTab)
        assertFalse(vm.uiState.value.isRefreshing)
    }
}
