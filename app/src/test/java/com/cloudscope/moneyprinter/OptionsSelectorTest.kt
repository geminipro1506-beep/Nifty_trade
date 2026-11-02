package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.options.BlackScholes
import com.cloudscope.moneyprinter.domain.options.OptionsSelector
import org.junit.Assert.*
import org.junit.Test

class OptionsSelectorTest {

    @Test
    fun testStrikeRoundingToNearest50() {
        assertEquals(24850.0, OptionsSelector.roundToStrikeStep(24862.0), 0.01)
        assertEquals(24850.0, OptionsSelector.roundToStrikeStep(24840.0), 0.01)
        assertEquals(24900.0, OptionsSelector.roundToStrikeStep(24885.0), 0.01)
    }

    @Test
    fun testTickSizeRoundingTo005() {
        assertEquals(125.05, OptionsSelector.roundToTickSize(125.04), 0.001)
        assertEquals(125.10, OptionsSelector.roundToTickSize(125.08), 0.001)
        assertEquals(125.00, OptionsSelector.roundToTickSize(125.01), 0.001)
    }

    @Test
    fun testBlackScholesDeltaCalculation() {
        // ATM European Call Delta should be approximately 0.50
        val callDelta = BlackScholes.calculateDelta(
            spot = 25000.0,
            strike = 25000.0,
            timeToExpiryYears = 7.0 / 365.0,
            volatility = 0.15,
            isCall = true
        )
        assertTrue("ATM Call Delta should be near 0.50 (got $callDelta)", callDelta in 0.48..0.55)

        // ATM European Put Delta should be approximately -0.50
        val putDelta = BlackScholes.calculateDelta(
            spot = 25000.0,
            strike = 25000.0,
            timeToExpiryYears = 7.0 / 365.0,
            volatility = 0.15,
            isCall = false
        )
        assertTrue("ATM Put Delta should be near -0.50 (got $putDelta)", putDelta in -0.55..-0.45)
    }

    @Test
    fun testContractSelection_BullishCallOption() {
        val spot = 24850.0
        val recommendation = OptionsSelector.selectOptimalContract(
            spotPrice = spot,
            direction = MarketDirection.BULLISH,
            optionChain = null,
            atr = 25.0
        )

        assertNotNull(recommendation)
        assertEquals(24850.0, recommendation!!.strikePrice, 0.01)
        assertEquals(OptionType.CE, recommendation.optionType)
        assertTrue("Stop loss must be lower than entry", recommendation.stopLoss < recommendation.entryPrice)
        assertTrue("Target 1 must be higher than entry", recommendation.target1 > recommendation.entryPrice)
        assertTrue("Target 2 must be higher than Target 1", recommendation.target2 > recommendation.target1)
        assertTrue("Risk/Reward should be at least 1:1.5", recommendation.riskRewardRatio >= 1.4)
    }

    @Test
    fun testWideSpreadRejection() {
        // Contract with spread > 2.5% should be rejected as illiquid
        val illiquidContract = OptionContract(
            symbol = "NIFTY 25000 CE",
            strikePrice = 25000.0,
            optionType = OptionType.CE,
            expiryDate = "Current Weekly",
            lastPrice = 100.0,
            change = 0.0,
            pChange = 0.0,
            openInterest = 50000L,
            changeInOi = 0L,
            volume = 1000L,
            impliedVolatility = 15.0,
            delta = 0.5,
            gamma = 0.002,
            theta = -8.0,
            vega = 12.0,
            bidPrice = 90.0,
            askPrice = 110.0, // ₹20 spread on ₹100 option (20% spread!)
            bidQty = 100,
            askQty = 100
        )

        val chain = OptionChain(
            spotPrice = 25000.0,
            atmStrike = 25000.0,
            pcr = 1.0,
            maxPainStrike = 25000.0,
            totalCallOi = 100000L,
            totalPutOi = 100000L,
            totalCallOiChange = 0L,
            totalPutOiChange = 0L,
            rows = listOf(StrikeRow(25000.0, illiquidContract, null))
        )

        val recommendation = OptionsSelector.selectOptimalContract(
            spotPrice = 25000.0,
            direction = MarketDirection.BULLISH,
            optionChain = chain,
            atr = 25.0
        )

        assertNull("Illiquid contract with wide spread should be rejected", recommendation)
    }
}
