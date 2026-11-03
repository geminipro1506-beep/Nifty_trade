package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.data.repository.SignalRepository
import com.cloudscope.moneyprinter.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoricalAuditClickTest {

    private val testDispatcher = StandardTestDispatcher()

    private class MockSignalRepository(
        private val initialSignals: List<HistoricalSignalRecord> = emptyList()
    ) : SignalRepository {
        private val _flow = MutableStateFlow(initialSignals)
        override val signalsFlow: StateFlow<List<HistoricalSignalRecord>> = _flow

        override suspend fun recordGeneratedSignal(record: HistoricalSignalRecord): Long = 1L
        override suspend fun updateSignalLifecycle(record: HistoricalSignalRecord): Int = 1
        override suspend fun getAllSignals(): List<HistoricalSignalRecord> = initialSignals
        override suspend fun getSignalsFiltered(
            fromTimestamp: Long,
            optionFilter: OptionFilter,
            strategyFilter: StrategyFilter,
            paperTradesOnly: Boolean?
        ): List<HistoricalSignalRecord> = initialSignals

        override suspend fun getOpenSignals(): List<HistoricalSignalRecord> = emptyList()
        override suspend fun getSignalById(id: String): HistoricalSignalRecord? =
            initialSignals.find { it.id == id }

        private val realRepo = com.cloudscope.moneyprinter.data.repository.SignalRepositoryImpl()
        override fun computeAccuracyMetrics(signals: List<HistoricalSignalRecord>): AccuracyMetrics {
            return realRepo.computeAccuracyMetrics(signals)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createBaseSignal(
        id: String,
        outcome: TradeOutcome? = TradeOutcome.WIN,
        status: SignalLifecycleStatus = SignalLifecycleStatus.CLOSED,
        grossPnl: Double = 1000.0,
        netPnl: Double = 890.0,
        lotSize: Int = 65,
        lots: Int = 1
    ): HistoricalSignalRecord {
        return HistoricalSignalRecord(
            id = id,
            timestamp = 1732000000000L,
            spotPrice = 24500.0,
            marketDirection = MarketDirection.BULLISH,
            action = SignalAction.BUY_CE,
            contractSymbol = "NIFTY24DEC24500CE",
            strikePrice = 24500.0,
            optionType = OptionType.CE,
            expiryDate = "26-Dec-2024",
            premiumAtSignal = 120.0,
            entryPrice = 120.0,
            stopLoss = 100.0,
            target1 = 150.0,
            target2 = 180.0,
            target3 = 210.0,
            confidenceScore = 80,
            rsi14 = 62.0,
            vwap = 24480.0,
            ema9 = 24510.0,
            ema20 = 24490.0,
            ema50 = 24450.0,
            pcr = 1.2,
            vix = 13.5,
            reasons = listOf("ORB Breakout", "VWAP Support"),
            invalidationConditions = "SL breach",
            status = status,
            outcome = outcome,
            grossPnl = grossPnl,
            netPnl = netPnl,
            lots = lots,
            lotSize = lotSize,
            actualExitPrice = 135.0,
            exitReason = ExitReason.TARGET_1
        )
    }

    @Test
    fun testClickWinningSignalUpdatesUiState() = runTest(testDispatcher) {
        val winningRecord = createBaseSignal("WIN-01", outcome = TradeOutcome.WIN, grossPnl = 1500.0, netPnl = 1380.0)
        val mockRepo = MockSignalRepository(listOf(winningRecord))
        val viewModel = MainViewModel(signalRepository = mockRepo)

        viewModel.selectSignalDetail(winningRecord)

        val state = viewModel.uiState.value
        assertNotNull(state.selectedDetailSignal)
        assertEquals("WIN-01", state.selectedDetailSignal?.id)
        assertEquals(TradeOutcome.WIN, state.selectedDetailSignal?.outcome)
        assertEquals(65, state.selectedDetailSignal?.lotSize)
        assertFalse(state.isSignalDetailLoading)
        assertNull(state.signalDetailError)
    }

    @Test
    fun testClickLosingSignalUpdatesUiState() = runTest(testDispatcher) {
        val losingRecord = createBaseSignal("LOSS-01", outcome = TradeOutcome.LOSS, grossPnl = -1300.0, netPnl = -1420.0)
        val mockRepo = MockSignalRepository(listOf(losingRecord))
        val viewModel = MainViewModel(signalRepository = mockRepo)

        viewModel.selectSignalDetail(losingRecord)

        val state = viewModel.uiState.value
        assertNotNull(state.selectedDetailSignal)
        assertEquals("LOSS-01", state.selectedDetailSignal?.id)
        assertEquals(TradeOutcome.LOSS, state.selectedDetailSignal?.outcome)
        assertEquals(-1300.0, state.selectedDetailSignal?.grossPnl ?: 0.0, 0.01)
    }

    @Test
    fun testClickAmbiguousSignalWithSpecialDetails() = runTest(testDispatcher) {
        val ambiguousRecord = createBaseSignal("AMBIG-01", outcome = TradeOutcome.AMBIGUOUS, grossPnl = 0.0, netPnl = -110.0).copy(
            targetAndSlSameCandle = true,
            ambiguousReason = "Target 1 and SL hit within the same 5-minute bar",
            finalResolutionStatus = "AMBIGUOUS_UNRESOLVED_EXCLUDED_FROM_WIN_RATE"
        )
        val mockRepo = MockSignalRepository(listOf(ambiguousRecord))
        val viewModel = MainViewModel(signalRepository = mockRepo)

        viewModel.selectSignalDetail(ambiguousRecord)

        val state = viewModel.uiState.value
        assertNotNull(state.selectedDetailSignal)
        assertEquals(TradeOutcome.AMBIGUOUS, state.selectedDetailSignal?.outcome)
        assertTrue(state.selectedDetailSignal?.targetAndSlSameCandle == true)
        assertEquals("Target 1 and SL hit within the same 5-minute bar", state.selectedDetailSignal?.ambiguousReason)
    }

    @Test
    fun testClickCancelledAndUnfilledSignals() = runTest(testDispatcher) {
        val cancelledRecord = createBaseSignal("CANCEL-01", outcome = TradeOutcome.CANCELLED, status = SignalLifecycleStatus.CANCELLED).copy(
            actualExitPrice = null,
            exitReason = ExitReason.CANCELLED
        )
        val mockRepo = MockSignalRepository(listOf(cancelledRecord))
        val viewModel = MainViewModel(signalRepository = mockRepo)

        viewModel.selectSignalDetail(cancelledRecord)

        val state = viewModel.uiState.value
        assertEquals("CANCEL-01", state.selectedDetailSignal?.id)
        assertEquals(TradeOutcome.CANCELLED, state.selectedDetailSignal?.outcome)
        assertNull(state.selectedDetailSignal?.actualExitPrice)
    }

    @Test
    fun testClickSignalWithMissingOptionalFieldsDoesNotCrash() = runTest(testDispatcher) {
        // Construct record with null optional fields, empty collections, and zeros
        val sparseRecord = HistoricalSignalRecord(
            id = "SPARSE-01",
            timestamp = 0L,
            spotPrice = 0.0,
            marketDirection = MarketDirection.SIDEWAYS,
            action = SignalAction.BUY_CE,
            contractSymbol = "NIFTY-MINIMAL",
            strikePrice = 0.0,
            optionType = OptionType.CE,
            expiryDate = "",
            premiumAtSignal = 0.0,
            entryPrice = 0.0,
            stopLoss = 0.0,
            target1 = 0.0,
            target2 = 0.0,
            target3 = 0.0,
            confidenceScore = 0,
            rsi14 = 0.0,
            vwap = 0.0,
            ema9 = 0.0,
            ema20 = 0.0,
            ema50 = 0.0,
            pcr = 0.0,
            vix = 0.0,
            reasons = emptyList(),
            invalidationConditions = "",
            status = SignalLifecycleStatus.PENDING_ENTRY,
            actualExitPrice = null,
            exitTimestamp = null,
            exitReason = null,
            ambiguousReason = null,
            migrationStatus = null,
            migrationNotes = null,
            originalLotSize = null,
            originalGrossPnl = null,
            originalNetPnl = null,
            adjustmentsLog = emptyList()
        )

        val mockRepo = MockSignalRepository(listOf(sparseRecord))
        val viewModel = MainViewModel(signalRepository = mockRepo)

        // Must not throw any exception
        viewModel.selectSignalDetail(sparseRecord)

        val state = viewModel.uiState.value
        assertNotNull(state.selectedDetailSignal)
        assertEquals("SPARSE-01", state.selectedDetailSignal?.id)
        assertEquals(0, state.selectedDetailSignal?.reasons?.size)
        assertEquals(65, state.selectedDetailSignal?.lotSize) // Default authorized lot size
    }

    @Test
    fun testSelectSignalByIdNonExistentTriggersErrorState() = runTest(testDispatcher) {
        val mockRepo = MockSignalRepository(emptyList())
        val viewModel = MainViewModel(signalRepository = mockRepo)

        viewModel.selectSignalDetailById("NON-EXISTENT-99")
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.selectedDetailSignal)
        assertFalse(state.isSignalDetailLoading)
        assertNotNull(state.signalDetailError)
        assertTrue(state.signalDetailError!!.contains("NON-EXISTENT-99"))
    }

    @Test
    fun testDismissSignalDetailResetsState() = runTest(testDispatcher) {
        val record = createBaseSignal("SIG-RESET")
        val mockRepo = MockSignalRepository(listOf(record))
        val viewModel = MainViewModel(signalRepository = mockRepo)

        viewModel.selectSignalDetail(record)
        assertNotNull(viewModel.uiState.value.selectedDetailSignal)

        viewModel.dismissSignalDetail()
        val state = viewModel.uiState.value
        assertNull(state.selectedDetailSignal)
        assertFalse(state.isSignalDetailLoading)
        assertNull(state.signalDetailError)
    }
}
