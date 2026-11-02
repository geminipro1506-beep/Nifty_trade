package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.data.repository.SignalRepositoryImpl
import com.cloudscope.moneyprinter.domain.audit.ReconciliationAuditor
import org.junit.Assert.*
import org.junit.Test

class ReconciliationAuditorTest {

    private fun createTrade(
        id: String,
        grossPnl: Double,
        charges: Double,
        netPnl: Double,
        outcome: TradeOutcome = TradeOutcome.WIN
    ): HistoricalSignalRecord {
        return HistoricalSignalRecord(
            id = id,
            timestamp = System.currentTimeMillis(),
            spotPrice = 24800.0,
            marketDirection = MarketDirection.BULLISH,
            action = SignalAction.BUY_CE,
            contractSymbol = "NIFTY 24800 CE",
            strikePrice = 24800.0,
            optionType = OptionType.CE,
            expiryDate = "Upcoming Weekly",
            premiumAtSignal = 120.0,
            entryPrice = 120.0,
            stopLoss = 95.0,
            target1 = 155.0,
            target2 = 180.0,
            target3 = 210.0,
            confidenceScore = 75,
            rsi14 = 55.0,
            vwap = 24780.0,
            ema9 = 24810.0,
            ema20 = 24790.0,
            ema50 = 24760.0,
            pcr = 1.15,
            vix = 14.0,
            reasons = listOf("Audit test"),
            invalidationConditions = "VWAP break",
            entryMet = true,
            status = SignalLifecycleStatus.CLOSED,
            outcome = outcome,
            grossPnl = grossPnl,
            netPnl = netPnl,
            totalCharges = charges,
            holdingTimeMinutes = 25
        )
    }

    @Test
    fun testReconciliationReportWithPerfectMath() {
        val repo = SignalRepositoryImpl()

        val trades = listOf(
            createTrade("T1", grossPnl = 1000.0, charges = 55.0, netPnl = 945.0),
            createTrade("T2", grossPnl = 1500.0, charges = 62.0, netPnl = 1438.0),
            createTrade("T3", grossPnl = -750.0, charges = 52.0, netPnl = -802.0, outcome = TradeOutcome.LOSS),
            createTrade("T4", grossPnl = 0.0, charges = 45.0, netPnl = -45.0, outcome = TradeOutcome.AMBIGUOUS)
        )

        val metrics = repo.computeAccuracyMetrics(trades)
        val report = ReconciliationAuditor.auditLedgerAndMetrics(trades, metrics)

        assertTrue(report.isFullyReconciled)
        assertEquals(0.0, report.netPnlDiscrepancy, 0.01)
        assertEquals(4, report.totalTradesAudited)
        assertEquals(1, report.ambiguousTradesCount)
        assertTrue(report.tradeAudits.all { it.isReconciled })
        assertTrue(report.auditSummary.contains("PERFECT RECONCILIATION"))
    }

    @Test
    fun testReconciliationReportDetectsArithmeticDiscrepancy() {
        val repo = SignalRepositoryImpl()

        val trades = listOf(
            createTrade("T1", grossPnl = 1000.0, charges = 55.0, netPnl = 945.0),
            // T2 has erroneous net P&L: 1500 - 62 = 1438, but netPnl is artificially set to 1500.0
            createTrade("T2", grossPnl = 1500.0, charges = 62.0, netPnl = 1500.0)
        )

        val metrics = repo.computeAccuracyMetrics(trades)
        val report = ReconciliationAuditor.auditLedgerAndMetrics(trades, metrics)

        assertFalse("Auditor must detect math discrepancy", report.isFullyReconciled)
        assertTrue(report.netPnlDiscrepancy > 0.0)
        assertFalse(report.tradeAudits.find { it.tradeId == "T2" }!!.isReconciled)
        assertEquals(62.0, report.tradeAudits.find { it.tradeId == "T2" }!!.discrepancy, 0.01)
    }
}
