package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.strategy.StrategyImprovementEngine
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StrategyImprovementEngineTest {

    private fun createTestTrade(
        id: String,
        outcome: TradeOutcome,
        optionType: OptionType,
        strategyType: StrategyType,
        grossPnl: Double,
        netPnl: Double,
        holdingTime: Long,
        hourIST: Int = 10
    ): HistoricalSignalRecord {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(ist).apply {
            set(Calendar.HOUR_OF_DAY, hourIST)
            set(Calendar.MINUTE, 15)
        }

        return HistoricalSignalRecord(
            id = id,
            timestamp = cal.timeInMillis,
            spotPrice = 24800.0,
            marketDirection = if (optionType == OptionType.CE) MarketDirection.BULLISH else MarketDirection.BEARISH,
            action = if (optionType == OptionType.CE) SignalAction.BUY_CE else SignalAction.BUY_PE,
            contractSymbol = "NIFTY 24800 ${optionType.name}",
            strikePrice = 24800.0,
            optionType = optionType,
            expiryDate = "Upcoming Weekly",
            premiumAtSignal = 120.0,
            entryPrice = 120.0,
            stopLoss = 90.0,
            target1 = 150.0,
            target2 = 180.0,
            target3 = 210.0,
            confidenceScore = 80,
            rsi14 = 55.0,
            vwap = 24780.0,
            ema9 = 24810.0,
            ema20 = 24795.0,
            ema50 = 24760.0,
            pcr = 1.1,
            vix = 14.0,
            reasons = listOf("Confluence test"),
            invalidationConditions = "VWAP break",
            entryMet = true,
            entryTimestamp = cal.timeInMillis,
            actualExitPrice = 150.0,
            exitTimestamp = cal.timeInMillis + (holdingTime * 60 * 1000L),
            exitReason = if (outcome == TradeOutcome.WIN) ExitReason.TARGET_1 else ExitReason.STOP_LOSS,
            status = SignalLifecycleStatus.CLOSED,
            outcome = outcome,
            grossPnl = grossPnl,
            netPnl = netPnl,
            totalCharges = 55.0,
            holdingTimeMinutes = holdingTime,
            strategyType = strategyType
        )
    }

    @Test
    fun testGenerateInsightsWithTrades() {
        val trades = listOf(
            createTestTrade("T1", TradeOutcome.WIN, OptionType.CE, StrategyType.BREAKOUT, 750.0, 695.0, 25, hourIST = 10),
            createTestTrade("T2", TradeOutcome.WIN, OptionType.CE, StrategyType.BREAKOUT, 750.0, 695.0, 30, hourIST = 10),
            createTestTrade("T3", TradeOutcome.LOSS, OptionType.PE, StrategyType.TREND_FOLLOWING, -750.0, -805.0, 20, hourIST = 12),
            createTestTrade("T4", TradeOutcome.WIN, OptionType.PE, StrategyType.TREND_FOLLOWING, 1500.0, 1440.0, 45, hourIST = 14)
        )

        val insights = StrategyImprovementEngine.generateInsights(trades)
        assertTrue(insights.isNotEmpty())
        assertTrue(insights.any { it.category.contains("Timing", ignoreCase = true) || it.category.contains("Option", ignoreCase = true) || it.category.contains("Breakout", ignoreCase = true) })
    }

    @Test
    fun testVersionComparisonReturnsBenchmarkEntries() {
        val trades = listOf(
            createTestTrade("T1", TradeOutcome.WIN, OptionType.CE, StrategyType.TREND_FOLLOWING, 750.0, 695.0, 25),
            createTestTrade("T2", TradeOutcome.WIN, OptionType.CE, StrategyType.BREAKOUT, 750.0, 695.0, 30)
        )

        val comparisons = StrategyImprovementEngine.compareStrategyVersions(trades)
        assertEquals(3, comparisons.size)
        assertTrue(comparisons.any { it.version.contains("v1.0") })
        assertTrue(comparisons.any { it.version.contains("v1.1") })
        assertTrue(comparisons.any { it.version.contains("v1.2") })
    }
}
