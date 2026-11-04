package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.HistoricalCandle
import com.cloudscope.moneyprinter.data.model.MarketQuote
import com.cloudscope.moneyprinter.data.model.NiftyFuturesQuote
import com.cloudscope.moneyprinter.domain.smartmoney.*
import org.junit.Assert.*
import org.junit.Test

class SmartMoneyDetectorTest {

    @Test
    fun testDetectUnusualVolume_FlagsSurgeOver1_5x() {
        val baseTime = 1730432700000L
        val candles = mutableListOf<HistoricalCandle>()
        // 19 normal candles of 100k volume
        for (i in 0 until 19) {
            candles.add(
                HistoricalCandle(
                    timestamp = baseTime + (i * 300000L),
                    open = 24800.0,
                    high = 24820.0,
                    low = 24790.0,
                    close = 24810.0,
                    volume = 100000L,
                    openInterest = 12000000L
                )
            )
        }
        // 20th candle with unusual 250k volume (2.5x)
        candles.add(
            HistoricalCandle(
                timestamp = baseTime + (19 * 300000L),
                open = 24810.0,
                high = 24850.0,
                low = 24805.0,
                close = 24845.0,
                volume = 250000L,
                openInterest = 12200000L
            )
        )

        val spotQuote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = 24845.0,
            change = 45.0,
            pChange = 0.18,
            open = 24800.0,
            high = 24850.0,
            low = 24790.0,
            close = 24800.0,
            vwap = 24820.0,
            volume = 250000L
        )

        val activity = SmartMoneyDetector.analyzeSmartMoney(
            spotQuote = spotQuote,
            futuresQuote = null,
            candles = candles,
            vwap = 24820.0,
            pdh = 24900.0,
            pdl = 24750.0,
            orbHigh = 24825.0,
            orbLow = 24790.0
        )

        assertTrue("Unusual volume should be flagged when volume >= 1.5x average", activity.isUnusualVolume)
        assertTrue("Volume ratio should be >= 1.5", activity.volumeRatio >= 1.5)
    }

    @Test
    fun testDetectVwapReclaim_WhenPriceDipsAndRecoversAboveVwap() {
        val candles = listOf(
            HistoricalCandle(1000L, 24820.0, 24830.0, 24790.0, 24800.0, 150000L), // dipped below VWAP (24810)
            HistoricalCandle(2000L, 24800.0, 24835.0, 24795.0, 24830.0, 200000L)  // reclaimed above VWAP
        )

        val spotQuote = MarketQuote(
            lastPrice = 24830.0,
            change = 30.0,
            pChange = 0.12,
            open = 24800.0,
            high = 24835.0,
            low = 24790.0,
            close = 24800.0,
            vwap = 24810.0,
            volume = 350000L
        )

        val activity = SmartMoneyDetector.analyzeSmartMoney(
            spotQuote = spotQuote,
            futuresQuote = null,
            candles = candles,
            vwap = 24810.0,
            pdh = 24900.0,
            pdl = 24750.0,
            orbHigh = 24825.0,
            orbLow = 24790.0
        )

        assertEquals(VwapRelationship.RECLAIM, activity.vwapState)
    }

    @Test
    fun testDetectVwapRejection_WhenPriceProbesAboveAndFails() {
        val candles = listOf(
            HistoricalCandle(1000L, 24800.0, 24825.0, 24795.0, 24820.0, 150000L), // probed above VWAP (24815)
            HistoricalCandle(2000L, 24820.0, 24822.0, 24780.0, 24785.0, 210000L)  // rejected below VWAP
        )

        val spotQuote = MarketQuote(
            lastPrice = 24785.0,
            change = -25.0,
            pChange = -0.10,
            open = 24810.0,
            high = 24825.0,
            low = 24780.0,
            close = 24810.0,
            vwap = 24815.0,
            volume = 360000L
        )

        val activity = SmartMoneyDetector.analyzeSmartMoney(
            spotQuote = spotQuote,
            futuresQuote = null,
            candles = candles,
            vwap = 24815.0,
            pdh = 24900.0,
            pdl = 24750.0,
            orbHigh = 24825.0,
            orbLow = 24790.0
        )

        assertEquals(VwapRelationship.REJECTION, activity.vwapState)
    }

    @Test
    fun testDetectLiquiditySweep_BullishStopHuntBelowSupport() {
        // PDL = 24750. Candle dips to 24730 (sweeping sell-side liquidity), then closes at 24755 with a 25pt lower wick out of 35pt range (71% wick)
        val candle = HistoricalCandle(
            timestamp = 1000L,
            open = 24760.0,
            high = 24765.0,
            low = 24730.0,
            close = 24755.0,
            volume = 280000L,
            openInterest = 12500000L
        )

        val spotQuote = MarketQuote(
            lastPrice = 24755.0,
            change = 5.0,
            pChange = 0.02,
            open = 24750.0,
            high = 24765.0,
            low = 24730.0,
            close = 24750.0,
            vwap = 24750.0,
            volume = 280000L
        )

        val activity = SmartMoneyDetector.analyzeSmartMoney(
            spotQuote = spotQuote,
            futuresQuote = null,
            candles = listOf(candle),
            vwap = 24750.0,
            pdh = 24850.0,
            pdl = 24750.0, // key support level swept
            orbHigh = 24800.0,
            orbLow = 24760.0
        )

        assertEquals(LiquiditySweepState.BULLISH_SWEEP, activity.liquiditySweep)
    }

    @Test
    fun testDetectMarketStructure_BullishBosAndHigherHighs() {
        val candles = listOf(
            HistoricalCandle(1000L, 24750.0, 24780.0, 24740.0, 24775.0, 100000L),
            HistoricalCandle(2000L, 24775.0, 24790.0, 24760.0, 24785.0, 110000L),
            HistoricalCandle(3000L, 24785.0, 24830.0, 24780.0, 24825.0, 180000L), // swing high 24830
            HistoricalCandle(4000L, 24825.0, 24828.0, 24805.0, 24815.0, 120000L), // swing low 24805
            HistoricalCandle(5000L, 24815.0, 24860.0, 24810.0, 24855.0, 220000L)  // broke above 24830
        )

        val spotQuote = MarketQuote(
            lastPrice = 24855.0,
            change = 80.0,
            pChange = 0.32,
            open = 24750.0,
            high = 24860.0,
            low = 24740.0,
            close = 24775.0,
            vwap = 24810.0,
            volume = 730000L
        )

        val activity = SmartMoneyDetector.analyzeSmartMoney(
            spotQuote = spotQuote,
            futuresQuote = null,
            candles = candles,
            vwap = 24810.0,
            pdh = 24800.0,
            pdl = 24700.0,
            orbHigh = 24790.0,
            orbLow = 24740.0
        )

        assertEquals(MarketStructureState.BULLISH_STRUCTURE, activity.marketStructure)
    }

    @Test
    fun testPriceOiRegime_InstitutionalBuildupMatrix() {
        val spotQuote = MarketQuote(
            lastPrice = 24880.0,
            change = 60.0,
            pChange = 0.24,
            open = 24820.0,
            high = 24890.0,
            low = 24810.0,
            close = 24820.0,
            vwap = 24840.0,
            volume = 500000L
        )

        // Case 1: Long Buildup (Price UP + Futures OI UP)
        val futLongBuildup = NiftyFuturesQuote(
            lastPrice = 24920.0,
            change = 65.0,
            pChange = 0.26,
            openInterest = 13500000L,
            changeInOi = 250000L,
            volume = 800000L,
            vwap = 24880.0
        )
        val act1 = SmartMoneyDetector.analyzeSmartMoney(spotQuote, futLongBuildup, emptyList(), 24840.0, 24900.0, 24750.0, 24850.0, 24800.0)
        assertEquals(PriceOiRegime.LONG_BUILDUP, act1.priceOiRegime)

        // Case 2: Short Covering (Price UP + Futures OI DOWN)
        val futShortCovering = NiftyFuturesQuote(
            lastPrice = 24920.0,
            change = 65.0,
            pChange = 0.26,
            openInterest = 12000000L,
            changeInOi = -180000L,
            volume = 800000L,
            vwap = 24880.0
        )
        val act2 = SmartMoneyDetector.analyzeSmartMoney(spotQuote, futShortCovering, emptyList(), 24840.0, 24900.0, 24750.0, 24850.0, 24800.0)
        assertEquals(PriceOiRegime.SHORT_COVERING, act2.priceOiRegime)

        // Case 3: Short Buildup (Price DOWN + Futures OI UP)
        val spotBear = spotQuote.copy(lastPrice = 24750.0, change = -70.0)
        val futShortBuildup = NiftyFuturesQuote(
            lastPrice = 24785.0,
            change = -75.0,
            pChange = -0.30,
            openInterest = 13800000L,
            changeInOi = 320000L,
            volume = 900000L,
            vwap = 24810.0
        )
        val act3 = SmartMoneyDetector.analyzeSmartMoney(spotBear, futShortBuildup, emptyList(), 24810.0, 24900.0, 24750.0, 24850.0, 24800.0)
        assertEquals(PriceOiRegime.SHORT_BUILDUP, act3.priceOiRegime)

        // Case 4: Long Unwinding (Price DOWN + Futures OI DOWN)
        val futLongUnwinding = NiftyFuturesQuote(
            lastPrice = 24785.0,
            change = -75.0,
            pChange = -0.30,
            openInterest = 11500000L,
            changeInOi = -220000L,
            volume = 750000L,
            vwap = 24810.0
        )
        val act4 = SmartMoneyDetector.analyzeSmartMoney(spotBear, futLongUnwinding, emptyList(), 24810.0, 24900.0, 24750.0, 24850.0, 24800.0)
        assertEquals(PriceOiRegime.LONG_UNWINDING, act4.priceOiRegime)
    }
}
