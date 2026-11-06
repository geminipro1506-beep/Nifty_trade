package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.indicator.IndicatorEngine
import com.cloudscope.moneyprinter.domain.normaloptions.NormalOptionsSignalEngine
import com.cloudscope.moneyprinter.domain.normaloptions.NormalSignalAction
import com.cloudscope.moneyprinter.domain.options.OptionsSelector
import com.cloudscope.moneyprinter.domain.smartmoney.OptionsConfirmationEngine
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneyDetector
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalAction
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalEngine
import org.junit.Assert.*
import org.junit.Test

class NormalOptionsSignalEngineTest {

    private fun createBullishCandles(count: Int = 45): List<HistoricalCandle> {
        val list = mutableListOf<HistoricalCandle>()
        var price = 24600.0
        val baseTime = 1730432700000L

        for (i in 0 until count) {
            val open = price
            val close = price + 15.0
            val high = close + 8.0
            val low = open - 4.0
            val volume = 60000L + (i * 2000L) // Expanding volume
            list.add(
                HistoricalCandle(
                    timestamp = baseTime + (i * 300000L),
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )
            price = close
        }
        return list
    }

    private fun createBearishCandles(count: Int = 45): List<HistoricalCandle> {
        val list = mutableListOf<HistoricalCandle>()
        var price = 25100.0
        val baseTime = 1730432700000L

        for (i in 0 until count) {
            val open = price
            val close = price - 15.0
            val high = open + 4.0
            val low = close - 8.0
            val volume = 60000L + (i * 2000L)
            list.add(
                HistoricalCandle(
                    timestamp = baseTime + (i * 300000L),
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )
            price = close
        }
        return list
    }

    private fun createSidewaysCandles(count: Int = 45): List<HistoricalCandle> {
        val list = mutableListOf<HistoricalCandle>()
        val basePrice = 24800.0
        val baseTime = 1730432700000L

        for (i in 0 until count) {
            val offset = if (i % 2 == 0) 4.0 else -4.0
            list.add(
                HistoricalCandle(
                    timestamp = baseTime + (i * 300000L),
                    open = basePrice,
                    high = basePrice + 8.0,
                    low = basePrice - 8.0,
                    close = basePrice + offset,
                    volume = 20000L
                )
            )
        }
        return list
    }

    private fun createMockOptionChain(atmStrike: Double): OptionChain {
        val callContract = OptionContract(
            symbol = "NIFTY24NOV${atmStrike.toInt()}CE",
            strikePrice = atmStrike,
            optionType = OptionType.CE,
            expiryDate = "07 NOV 2026",
            lastPrice = 145.0,
            change = 22.0,
            pChange = 17.8,
            openInterest = 2500000L,
            changeInOi = 450000L,
            volume = 1200000L,
            impliedVolatility = 13.5,
            delta = 0.52,
            gamma = 0.002,
            theta = -8.5,
            vega = 12.0,
            bidPrice = 144.5,
            askPrice = 145.0,
            bidQty = 1500,
            askQty = 1800
        )

        val putContract = OptionContract(
            symbol = "NIFTY24NOV${atmStrike.toInt()}PE",
            strikePrice = atmStrike,
            optionType = OptionType.PE,
            expiryDate = "07 NOV 2026",
            lastPrice = 132.0,
            change = -18.0,
            pChange = -12.0,
            openInterest = 2800000L,
            changeInOi = 600000L,
            volume = 1400000L,
            impliedVolatility = 14.0,
            delta = -0.48,
            gamma = 0.002,
            theta = -8.0,
            vega = 11.5,
            bidPrice = 131.5,
            askPrice = 132.0,
            bidQty = 2000,
            askQty = 2200
        )

        return OptionChain(
            spotPrice = atmStrike,
            atmStrike = atmStrike,
            pcr = 1.15,
            maxPainStrike = atmStrike,
            totalCallOi = 10000000L,
            totalPutOi = 11500000L,
            totalCallOiChange = 500000L,
            totalPutOiChange = 900000L,
            rows = listOf(StrikeRow(atmStrike, callContract, putContract)),
            expiryDates = listOf("07 NOV 2026")
        )
    }

    @Test
    fun testBuyCallSignalGeneratedOnFullBullishConfluence() {
        val candles = createBullishCandles(30)
        val lastCandle = candles.last()
        val spotPrice = lastCandle.close

        val quote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = spotPrice,
            change = 210.0,
            pChange = 0.85,
            high = spotPrice + 10.0,
            low = 24600.0,
            open = 24610.0,
            close = 24600.0,
            vwap = spotPrice - 60.0,
            volume = 95000L,
            timestamp = System.currentTimeMillis()
        )

        val indicators = TechnicalIndicators(
            rsi14 = 66.0,
            ema20 = spotPrice - 40.0,
            ema50 = spotPrice - 90.0,
            ema200 = spotPrice - 200.0,
            vwap = spotPrice - 60.0,
            macdLine = 18.0,
            signalLine = 12.0,
            macdHistogram = 6.0,
            atr14 = 28.0,
            pdh = 24700.0,
            pdl = 24500.0,
            pdc = 24600.0,
            orbHigh = 24650.0,
            orbLow = 24580.0,
            orbState = OrbState.BREAKOUT_UP,
            pcr = 1.20,
            vix = 14.0,
            marketDirection = MarketDirection.BULLISH,
            ema9 = spotPrice - 15.0
        )

        val atmStrike = OptionsSelector.roundToStrikeStep(spotPrice)
        val optionChain = createMockOptionChain(atmStrike)

        val signal = NormalOptionsSignalEngine.generateSignal(
            quote = quote,
            candles = candles,
            indicators = indicators,
            optionChain = optionChain
        )

        assertEquals(NormalSignalAction.BUY_CALL, signal.action)
        assertEquals(OptionType.CE, signal.optionType)
        assertTrue("Confidence should be >= 70%", signal.confidencePercent >= 70)
        assertEquals(atmStrike, signal.suggestedStrike ?: 0.0, 0.01)
        assertEquals("07 NOV 2026", signal.expiryDate)
        assertEquals(145.0, signal.entryPrice, 0.01) // Live premium from mock chain
        assertTrue("Stop loss must be lower than entry", signal.stopLoss < signal.entryPrice)
        assertTrue("Target 1 must be higher than entry", signal.target1 > signal.entryPrice)
        assertTrue("Target 2 must be higher than Target 1", signal.target2 > signal.target1)
        assertTrue("Risk-to-reward ratio formatted", signal.riskRewardRatio.startsWith("1 :"))
        assertTrue("Reason mentions VWAP and EMA", signal.signalReason.contains("EMA") && signal.signalReason.contains("VWAP"))
    }

    @Test
    fun testBuyPutSignalGeneratedOnFullBearishConfluence() {
        val candles = createBearishCandles(30)
        val lastCandle = candles.last()
        val spotPrice = lastCandle.close

        val quote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = spotPrice,
            change = -230.0,
            pChange = -0.92,
            high = 25110.0,
            low = spotPrice - 10.0,
            open = 25090.0,
            close = 25100.0,
            vwap = spotPrice + 60.0,
            volume = 95000L,
            timestamp = System.currentTimeMillis()
        )

        val indicators = TechnicalIndicators(
            rsi14 = 34.0,
            ema20 = spotPrice + 40.0,
            ema50 = spotPrice + 90.0,
            ema200 = spotPrice + 200.0,
            vwap = spotPrice + 60.0,
            macdLine = -18.0,
            signalLine = -10.0,
            macdHistogram = -8.0,
            atr14 = 28.0,
            pdh = 25150.0,
            pdl = 24950.0,
            pdc = 25100.0,
            orbHigh = 25080.0,
            orbLow = 25020.0,
            orbState = OrbState.BREAKOUT_DOWN,
            pcr = 0.78,
            vix = 16.5,
            marketDirection = MarketDirection.BEARISH,
            ema9 = spotPrice + 15.0
        )

        val atmStrike = OptionsSelector.roundToStrikeStep(spotPrice)
        val optionChain = createMockOptionChain(atmStrike)

        val signal = NormalOptionsSignalEngine.generateSignal(
            quote = quote,
            candles = candles,
            indicators = indicators,
            optionChain = optionChain
        )

        assertEquals(NormalSignalAction.BUY_PUT, signal.action)
        assertEquals(OptionType.PE, signal.optionType)
        assertTrue("Confidence should be >= 70%", signal.confidencePercent >= 70)
        assertEquals(atmStrike, signal.suggestedStrike ?: 0.0, 0.01)
        assertEquals("07 NOV 2026", signal.expiryDate)
        assertEquals(132.0, signal.entryPrice, 0.01) // Live PE premium
        assertTrue("Stop loss must be lower than entry", signal.stopLoss < signal.entryPrice)
        assertTrue("Target 1 must be higher than entry", signal.target1 > signal.entryPrice)
        assertTrue("Target 2 must be higher than Target 1", signal.target2 > signal.target1)
        assertTrue("Risk-to-reward ratio formatted", signal.riskRewardRatio.startsWith("1 :"))
    }

    @Test
    fun testNoTradeWhenMarketIsSideways() {
        val candles = createSidewaysCandles(30)
        val spotPrice = 24800.0

        val quote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = spotPrice,
            change = 5.0,
            pChange = 0.02,
            high = 24810.0,
            low = 24790.0,
            open = 24800.0,
            close = 24795.0,
            vwap = 24800.5,
            volume = 20000L,
            timestamp = System.currentTimeMillis()
        )

        val indicators = TechnicalIndicators(
            rsi14 = 50.2,
            ema20 = 24801.0,
            ema50 = 24802.0,
            ema200 = 24790.0,
            vwap = 24800.5,
            macdLine = 0.5,
            signalLine = 0.4,
            macdHistogram = 0.1,
            atr14 = 14.0,
            pdh = 24850.0,
            pdl = 24750.0,
            pdc = 24795.0,
            orbHigh = 24815.0,
            orbLow = 24785.0,
            orbState = OrbState.INSIDE_RANGE,
            pcr = 1.02,
            vix = 13.0,
            marketDirection = MarketDirection.SIDEWAYS,
            ema9 = 24800.8
        )

        val signal = NormalOptionsSignalEngine.generateSignal(
            quote = quote,
            candles = candles,
            indicators = indicators,
            optionChain = null
        )

        assertEquals(NormalSignalAction.NO_TRADE, signal.action)
        assertEquals(0, signal.confidencePercent)
        assertTrue(signal.signalReason.contains("range") || signal.signalReason.contains("confluence") || signal.signalReason.contains("consolidat") || signal.signalReason.contains("Conflicting") || signal.signalReason.contains("edge"))
    }

    @Test
    fun testWaitingForDataWhenNullQuoteOrEmptyCandles() {
        val signalNull = NormalOptionsSignalEngine.generateSignal(
            quote = null,
            candles = emptyList(),
            indicators = null,
            optionChain = null
        )

        assertEquals(NormalSignalAction.WAITING_FOR_DATA, signalNull.action)
        assertEquals(0, signalNull.confidencePercent)
        assertTrue(signalNull.signalReason.contains("Awaiting") || signalNull.signalReason.contains("Waiting"))
    }

    @Test
    fun testRsiOrVwapAloneDoesNotTriggerBuyCallOrPut() {
        // High RSI (68.0) and price slightly above VWAP, but inside range, no breakout, and flat/bearish EMA
        val candles = createSidewaysCandles(30)
        val spotPrice = 24802.0

        val quote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = spotPrice,
            change = 10.0,
            pChange = 0.04,
            high = 24810.0,
            low = 24790.0,
            open = 24800.0,
            close = 24795.0,
            vwap = 24800.5,
            volume = 20000L,
            timestamp = System.currentTimeMillis()
        )

        val indicators = TechnicalIndicators(
            rsi14 = 68.0, // High RSI
            ema20 = 24820.0, // 20 EMA above price (bearish)
            ema50 = 24830.0,
            ema200 = 24850.0,
            vwap = 24795.0, // Price is above VWAP
            macdLine = -2.0,
            signalLine = -1.0,
            macdHistogram = -1.0, // Negative MACD
            atr14 = 15.0,
            pdh = 24900.0,
            pdl = 24700.0,
            pdc = 24795.0,
            orbHigh = 24850.0,
            orbLow = 24750.0,
            orbState = OrbState.INSIDE_RANGE, // No breakout!
            pcr = 1.05,
            vix = 14.0,
            marketDirection = MarketDirection.SIDEWAYS,
            ema9 = 24815.0 // 9 EMA below 20 EMA
        )

        val signal = NormalOptionsSignalEngine.generateSignal(
            quote = quote,
            candles = candles,
            indicators = indicators,
            optionChain = null
        )

        // Strict requirement: Never generate BUY CALL on RSI or VWAP alone!
        assertNotEquals(NormalSignalAction.BUY_CALL, signal.action)
    }

    @Test
    fun testIndependentEnginesCanProduceDifferentSignalsConcurrently() {
        // Normal Options Signal uses technical breakouts & EMA alignment -> BUY CALL
        // But Smart Money detects heavy Call writing at ATM strike (resistance) -> NO TRADE
        val candles = createBullishCandles(30)
        val spotPrice = candles.last().close

        val quote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = spotPrice,
            change = 180.0,
            pChange = 0.72,
            high = spotPrice + 10.0,
            low = 24600.0,
            open = 24610.0,
            close = 24600.0,
            vwap = spotPrice - 60.0,
            volume = 95000L,
            timestamp = System.currentTimeMillis()
        )

        val indicators = TechnicalIndicators(
            rsi14 = 64.0,
            ema20 = spotPrice - 30.0,
            ema50 = spotPrice - 70.0,
            ema200 = spotPrice - 180.0,
            vwap = spotPrice - 50.0,
            macdLine = 15.0,
            signalLine = 10.0,
            macdHistogram = 5.0,
            atr14 = 28.0,
            pdh = 24700.0,
            pdl = 24500.0,
            pdc = 24600.0,
            orbHigh = 24650.0,
            orbLow = 24580.0,
            orbState = OrbState.BREAKOUT_UP,
            pcr = 0.72, // Bearish low PCR (Call writing dominance)
            vix = 14.0,
            marketDirection = MarketDirection.BULLISH,
            ema9 = spotPrice - 10.0
        )

        // Option chain with heavy Call Short Buildup (Call writers defending ceiling)
        val atmStrike = OptionsSelector.roundToStrikeStep(spotPrice)
        val callContract = OptionContract(
            symbol = "NIFTY24NOV${atmStrike.toInt()}CE",
            strikePrice = atmStrike,
            optionType = OptionType.CE,
            expiryDate = "07 NOV 2026",
            lastPrice = 145.0,
            change = -5.0, // Price down with OI up = Short Buildup!
            pChange = -3.4,
            openInterest = 5500000L,
            changeInOi = 1200000L, // Massive call writing!
            volume = 2000000L,
            impliedVolatility = 14.0,
            delta = 0.50,
            gamma = 0.002,
            theta = -8.0,
            vega = 11.0,
            bidPrice = 144.5,
            askPrice = 145.0,
            bidQty = 2000,
            askQty = 2000
        )
        val chain = OptionChain(
            spotPrice = spotPrice,
            atmStrike = atmStrike,
            pcr = 0.72,
            maxPainStrike = atmStrike,
            totalCallOi = 15000000L,
            totalPutOi = 10800000L,
            totalCallOiChange = 2000000L,
            totalPutOiChange = 400000L,
            rows = listOf(StrikeRow(atmStrike, callContract, null)),
            expiryDates = listOf("07 NOV 2026")
        )

        // 1. Generate Normal Signal
        val normalSignal = NormalOptionsSignalEngine.generateSignal(
            quote = quote,
            candles = candles,
            indicators = indicators,
            optionChain = chain
        )

        // 2. Generate Smart Money Signal
        val smActivity = SmartMoneyDetector.analyzeSmartMoney(
            spotQuote = quote,
            futuresQuote = null,
            candles = candles,
            vwap = indicators.vwap,
            pdh = indicators.pdh,
            pdl = indicators.pdl,
            orbHigh = indicators.orbHigh,
            orbLow = indicators.orbLow
        )
        val smOptionsDetails = OptionsConfirmationEngine.confirmOptions(spotPrice, chain)
        val smSignal = SmartMoneySignalEngine.generateSignal(
            spotQuote = quote,
            smartMoneyActivity = smActivity,
            optionsDetails = smOptionsDetails,
            optionChain = chain
        )

        // Normal signal is BUY CALL because technical price action & indicators are bullish
        assertEquals(NormalSignalAction.BUY_CALL, normalSignal.action)

        // Smart Money signal is NO TRADE due to conflicting Call writing resistance
        assertEquals(SmartMoneySignalAction.NO_TRADE, smSignal.action)
        assertTrue(smSignal.shortReason.contains("Conflicting") || smSignal.shortReason.contains("Call Writing") || smSignal.shortReason.contains("PCR"))
    }

    @Test
    fun testSupertrendCalculationAccuracy() {
        val candles = createBullishCandles(30)
        val supertrend = IndicatorEngine.calculateSupertrend(candles, period = 10, multiplier = 3.0)

        assertTrue("Supertrend is bullish on uptrend", supertrend.isBullish)
        assertTrue("Supertrend value is below current close", supertrend.value < candles.last().close)
    }
}
