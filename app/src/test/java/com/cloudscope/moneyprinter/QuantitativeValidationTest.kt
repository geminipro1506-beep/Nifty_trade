package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.data.repository.SignalRepositoryImpl
import com.cloudscope.moneyprinter.domain.strategy.StrategyImprovementEngine
import org.junit.Assert.*
import org.junit.Test

class QuantitativeValidationTest {

    private fun createTestRecord(
        id: String,
        outcome: TradeOutcome?,
        status: SignalLifecycleStatus,
        entryPrice: Double = 100.0,
        exitPrice: Double = 100.0,
        grossPnl: Double = 0.0,
        charges: Double = 50.0,
        netPnl: Double = grossPnl - charges,
        entryMet: Boolean = true,
        strategyType: StrategyType = StrategyType.TREND_FOLLOWING,
        optionType: OptionType = OptionType.CE,
        vix: Double = 14.0,
        timestamp: Long = System.currentTimeMillis()
    ): HistoricalSignalRecord {
        return HistoricalSignalRecord(
            id = id,
            timestamp = timestamp,
            spotPrice = 24800.0,
            marketDirection = MarketDirection.BULLISH,
            action = if (optionType == OptionType.CE) SignalAction.BUY_CE else SignalAction.BUY_PE,
            contractSymbol = "NIFTY 24800 ${optionType.name}",
            strikePrice = 24800.0,
            optionType = optionType,
            expiryDate = "Current Weekly",
            premiumAtSignal = entryPrice,
            entryPrice = entryPrice,
            stopLoss = entryPrice - 20.0,
            target1 = entryPrice + 30.0,
            target2 = entryPrice + 50.0,
            target3 = entryPrice + 80.0,
            confidenceScore = 75,
            rsi14 = 55.0,
            vwap = 24780.0,
            ema9 = 24810.0,
            ema20 = 24790.0,
            ema50 = 24760.0,
            pcr = 1.15,
            vix = vix,
            reasons = listOf("Quantitative validation"),
            invalidationConditions = "VWAP break",
            entryMet = entryMet,
            actualExitPrice = exitPrice,
            status = status,
            outcome = outcome,
            grossPnl = grossPnl,
            netPnl = netPnl,
            totalCharges = charges,
            holdingTimeMinutes = 30,
            strategyType = strategyType
        )
    }

    @Test
    fun testWinRateStrictlyExcludesAmbiguousCancelledExpiredAndUnfilled() {
        val repo = SignalRepositoryImpl()

        val signals = listOf(
            // 3 Wins
            createTestRecord("W1", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED, grossPnl = 1000.0, charges = 50.0),
            createTestRecord("W2", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED, grossPnl = 1200.0, charges = 50.0),
            createTestRecord("W3", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED, grossPnl = 800.0, charges = 50.0),
            // 1 Loss
            createTestRecord("L1", TradeOutcome.LOSS, SignalLifecycleStatus.CLOSED, grossPnl = -600.0, charges = 50.0),
            // 1 Ambiguous (must be excluded from win rate denominator!)
            createTestRecord("A1", TradeOutcome.AMBIGUOUS, SignalLifecycleStatus.CLOSED, grossPnl = 0.0, charges = 45.0),
            // 1 Breakeven
            createTestRecord("B1", TradeOutcome.BREAKEVEN, SignalLifecycleStatus.CLOSED, grossPnl = 0.0, charges = 45.0),
            // 1 Cancelled before entry
            createTestRecord("C1", TradeOutcome.CANCELLED, SignalLifecycleStatus.CANCELLED, entryMet = false, grossPnl = 0.0, charges = 0.0),
            // 1 Expired unfilled
            createTestRecord("E1", TradeOutcome.UNFILLED, SignalLifecycleStatus.EXPIRED, entryMet = false, grossPnl = 0.0, charges = 0.0)
        )

        val metrics = repo.computeAccuracyMetrics(signals)

        // Winning trades = 3, Losing trades = 1
        assertEquals(3, metrics.winningTrades)
        assertEquals(1, metrics.losingTrades)
        assertEquals(4, metrics.resolvedTradesCount) // 3 + 1
        assertEquals(1, metrics.ambiguousTrades)
        assertEquals(1, metrics.cancelledSignals)
        assertEquals(1, metrics.expiredUnfilledSignals)

        // Strict Section 1 Win Rate Formula:
        // Winning trades / (Winning trades + Losing trades) = 3 / (3 + 1) = 75.0%
        assertEquals(75.0, metrics.winRate, 0.01)
        // Loss Rate = 1 / (3 + 1) = 25.0%
        assertEquals(25.0, metrics.lossRate, 0.01)
        assertEquals(100.0, metrics.winRate + metrics.lossRate, 0.01)
    }

    @Test
    fun testProfitFactorAndExpectancyReconciliation() {
        val repo = SignalRepositoryImpl()

        val signals = listOf(
            createTestRecord("W1", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED, grossPnl = 1500.0, charges = 60.0), // Net: 1440
            createTestRecord("W2", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED, grossPnl = 1000.0, charges = 50.0), // Net: 950
            createTestRecord("L1", TradeOutcome.LOSS, SignalLifecycleStatus.CLOSED, grossPnl = -500.0, charges = 50.0)  // Net: -550
        )

        val metrics = repo.computeAccuracyMetrics(signals)

        // Profit Factor: Gross Winning (2500) / Absolute Gross Losing (500) = 5.0
        assertEquals(2500.0, metrics.grossProfit, 0.01)
        assertEquals(500.0, metrics.grossLoss, 0.01)
        assertEquals(5.0, metrics.profitFactor, 0.01)

        // Net PnL of resolved trades = 1440 + 950 - 550 = 1840.0
        // Expectancy = 1840 / 3 = 613.33
        assertEquals(613.33, metrics.expectancyPerTrade, 0.05)

        // Mathematical equivalence test: (WinRate * AvgNetWin) - (LossRate * AvgNetLoss)
        val winRateFrac = 2.0 / 3.0
        val lossRateFrac = 1.0 / 3.0
        val avgWinNet = (1440.0 + 950.0) / 2.0 // 1195.0
        val avgLossNet = 550.0
        val expectedFormula = (winRateFrac * avgWinNet) - (lossRateFrac * avgLossNet)
        assertEquals(expectedFormula, metrics.expectancyPerTrade, 0.05)
    }

    @Test
    fun testChronologicalDrawdownFromEquityCurve() {
        val repo = SignalRepositoryImpl()
        val now = 10000000L

        val signals = listOf(
            createTestRecord("T1", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED, grossPnl = 2050.0, charges = 50.0, timestamp = now + 1000),
            createTestRecord("T2", TradeOutcome.LOSS, SignalLifecycleStatus.CLOSED, grossPnl = -2950.0, charges = 50.0, timestamp = now + 2000),
            createTestRecord("T3", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED, grossPnl = 1050.0, charges = 50.0, timestamp = now + 3000),
            createTestRecord("T4", TradeOutcome.LOSS, SignalLifecycleStatus.CLOSED, grossPnl = -1950.0, charges = 50.0, timestamp = now + 4000)
        )

        val metrics = repo.computeAccuracyMetrics(signals)

        assertEquals(4000.0, metrics.maxDrawdownAmount, 0.01)
        val expectedDdPct = (4000.0 / 102000.0) * 100.0
        assertEquals(expectedDdPct, metrics.maxDrawdownPercent, 0.1)
    }

    @Test
    fun testStatisticalReliabilityLabels() {
        val repo = SignalRepositoryImpl()

        // Less than 30 trades
        val smallBatch = (1..15).map {
            createTestRecord("S$it", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED)
        }
        val smallMetrics = repo.computeAccuracyMetrics(smallBatch)
        assertEquals("Insufficient sample", smallMetrics.statisticalReliabilityLabel)
        assertNotNull(smallMetrics.sampleSizeWarning)

        // 30 to 99 trades
        val prelimBatch = (1..40).map {
            createTestRecord("P$it", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED)
        }
        val prelimMetrics = repo.computeAccuracyMetrics(prelimBatch)
        assertEquals("Preliminary evidence", prelimMetrics.statisticalReliabilityLabel)
        assertNull(prelimMetrics.sampleSizeWarning)
    }

    @Test
    fun testCohortOptimizationDoesNotPromoteHigherWinRateWithoutSampleThreshold() {
        // High win rate (100%) but only 4 trades (N < 30)
        val tinyBatch = listOf(
            createTestRecord("T1", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED, strategyType = StrategyType.BREAKOUT),
            createTestRecord("T2", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED, strategyType = StrategyType.BREAKOUT),
            createTestRecord("T3", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED, strategyType = StrategyType.BREAKOUT),
            createTestRecord("T4", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED, strategyType = StrategyType.BREAKOUT)
        )

        val cohorts = StrategyImprovementEngine.evaluateStrategyCohorts(tinyBatch)
        val orbCohort = cohorts.find { it.cohortName == "ORB 15m Breakout" }

        assertNotNull(orbCohort)
        assertEquals(100.0, orbCohort!!.winRate, 0.01)
        // Must NOT be promoted solely due to high win rate!
        assertFalse("Strategy with N < 30 must not be promoted despite 100% win rate", orbCohort.isPromoted)
        assertTrue(orbCohort.promotionRationale.contains("CANNOT PROMOTE", ignoreCase = true))
    }

    @Test
    fun testVersionComparisonPredefinedValidationCriteria() {
        val smallTrades = (1..10).map {
            createTestRecord("TR$it", TradeOutcome.WIN, SignalLifecycleStatus.CLOSED)
        }

        val comparisons = StrategyImprovementEngine.compareStrategyVersions(smallTrades)
        val v11 = comparisons.find { it.version.contains("v1.1") }
        assertNotNull(v11)
        assertFalse("Strategy v1.1 cannot be LIVE VALIDATED with only 10 trades (< 100)", v11!!.isLiveValidated)
        assertTrue(v11.status.contains("INCUBATING") || v11.status.contains("PAPER ACTIVE"))
    }
}
