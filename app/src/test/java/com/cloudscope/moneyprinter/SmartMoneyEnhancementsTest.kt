package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.HistoricalCandle
import com.cloudscope.moneyprinter.data.model.MarketQuote
import com.cloudscope.moneyprinter.domain.smartmoney.*
import org.junit.Assert.*
import org.junit.Test

class SmartMoneyEnhancementsTest {

    private fun createCandlesWithBullishChoch(): List<HistoricalCandle> {
        val candles = mutableListOf<HistoricalCandle>()
        val baseTime = 1730955600000L

        // Prior downtrend: lower highs and lower lows
        candles.add(HistoricalCandle(baseTime, 24900.0, 24910.0, 24850.0, 24860.0, 50000L))
        candles.add(HistoricalCandle(baseTime + 300000L, 24860.0, 24880.0, 24800.0, 24810.0, 60000L)) // Lower Low
        candles.add(HistoricalCandle(baseTime + 600000L, 24810.0, 24840.0, 24790.0, 24820.0, 55000L)) // Swing High at 24840
        candles.add(HistoricalCandle(baseTime + 900000L, 24820.0, 24830.0, 24770.0, 24780.0, 70000L)) // Lower Low at 24770
        // Impulsive breakout above 24840 (CHoCH)
        candles.add(HistoricalCandle(baseTime + 1200000L, 24780.0, 24870.0, 24780.0, 24865.0, 120000L)) // Closes above 24840!

        return candles
    }

    @Test
    fun testBullishChochDetected() {
        val candles = createCandlesWithBullishChoch()
        val last = candles.last()
        val quote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = last.close,
            change = 30.0,
            pChange = 0.12,
            open = 24780.0,
            high = 24870.0,
            low = 24770.0,
            close = 24835.0,
            vwap = 24810.0,
            volume = 800000L
        )

        val activity = SmartMoneyDetector.analyzeSmartMoney(
            spotQuote = quote,
            futuresQuote = null,
            candles = candles,
            vwap = 24810.0,
            pdh = 24920.0,
            pdl = 24750.0,
            orbHigh = 24850.0,
            orbLow = 24800.0
        )

        assertEquals(ChochState.BULLISH_CHOCH, activity.chochState)
        assertEquals(MarketStructureState.BULLISH_STRUCTURE, activity.marketStructure)
        assertTrue(activity.summary.contains("Potential Smart Money Activity"))
        assertTrue(activity.summary.contains("Bullish CHoCH"))
    }
}
