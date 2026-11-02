package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.local.HistoricalSampleData
import com.cloudscope.moneyprinter.data.model.BacktestConfig
import com.cloudscope.moneyprinter.domain.backtest.BacktestEngine
import org.junit.Assert.*
import org.junit.Test

class BacktestEngineTest {

    @Test
    fun testBacktestExecutionOnHistoricalCandles() {
        val candles = HistoricalSampleData.sampleNiftyCandles
        assertTrue("Historical sample candles should have at least 30 candles", candles.size >= 30)

        val config = BacktestConfig(
            initialCapital = 100000.0,
            brokeragePerOrder = 20.0,
            slippagePoints = 0.5,
            minConfidenceThreshold = 62
        )

        val result = BacktestEngine.runBacktest(candles, config)

        assertNotNull(result)
        assertTrue("Backtest must be walk-forward validated", result.walkForwardValidated)
        assertTrue("Total taxes and charges should be recorded", result.totalChargesAndTaxes >= 0.0)
        assertTrue("Max drawdown must be non-negative", result.maxDrawdownAmount >= 0.0)
        assertTrue("Profit factor should be non-negative", result.profitFactor >= 0.0)
        assertNotNull("Parameter stability report should be present", result.parameterStabilityReport)
    }

    @Test
    fun testParameterStabilityReportVerification() {
        val candles = HistoricalSampleData.sampleNiftyCandles
        val stabilityReport = BacktestEngine.testParameterStability(candles)

        assertNotNull(stabilityReport)
        assertTrue(stabilityReport.parameterResults.isNotEmpty())
        assertTrue("Stability report notes should be present", stabilityReport.auditNotes.isNotEmpty())
    }

    @Test
    fun testBacktestWithEmptyData_HandlesGracefully() {
        val result = BacktestEngine.runBacktest(emptyList())
        assertEquals(0, result.totalTrades)
        assertEquals(0.0, result.netProfit, 0.001)
        assertFalse(result.walkForwardValidated)
    }
}
