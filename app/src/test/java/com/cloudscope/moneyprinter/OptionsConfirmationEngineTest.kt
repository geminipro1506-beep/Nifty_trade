package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.OptionChain
import com.cloudscope.moneyprinter.data.model.OptionContract
import com.cloudscope.moneyprinter.data.model.OptionType
import com.cloudscope.moneyprinter.data.model.StrikeRow
import com.cloudscope.moneyprinter.domain.smartmoney.OptionBuildupType
import com.cloudscope.moneyprinter.domain.smartmoney.OptionsConfirmationEngine
import org.junit.Assert.*
import org.junit.Test

class OptionsConfirmationEngineTest {

    private fun createContract(
        symbol: String,
        strike: Double,
        type: OptionType,
        ltp: Double,
        change: Double,
        oi: Long,
        oiChange: Long,
        bid: Double = ltp - 0.25,
        ask: Double = ltp + 0.25,
        volume: Long = 500000L
    ) = OptionContract(
        symbol = symbol,
        strikePrice = strike,
        optionType = type,
        expiryDate = "07 NOV 2024",
        lastPrice = ltp,
        change = change,
        pChange = (change / (ltp - change)) * 100.0,
        openInterest = oi,
        changeInOi = oiChange,
        volume = volume,
        impliedVolatility = 14.0,
        delta = if (type == OptionType.CE) 0.50 else -0.50,
        gamma = 0.002,
        theta = -8.0,
        vega = 12.0,
        bidPrice = bid,
        askPrice = ask,
        bidQty = 1500,
        askQty = 1500
    )

    @Test
    fun testMissingOptionChain_ShowsConfirmationUnavailable() {
        val detailsNull = OptionsConfirmationEngine.confirmOptions(24850.0, null)
        assertFalse("Missing option chain must mark confirmation as unavailable", detailsNull.isAvailable)
        assertTrue("Reason must state option-chain confirmation unavailable", detailsNull.confirmationReason.contains("unavailable"))
        assertFalse(detailsNull.isBullishConfirmed)
        assertFalse(detailsNull.isBearishConfirmed)

        val emptyChain = OptionChain(24850.0, 24850.0, 1.0, 24850.0, 0L, 0L, 0L, 0L, emptyList())
        val detailsEmpty = OptionsConfirmationEngine.confirmOptions(24850.0, emptyChain)
        assertFalse(detailsEmpty.isAvailable)
    }

    @Test
    fun testOptionBuildupClassification_DistinguishesBuyingFromWriting() {
        // 1. Call Long Buildup: Price UP + OI UP
        val callLong = createContract("NIFTY 24850 CE", 24850.0, OptionType.CE, ltp = 145.0, change = 12.0, oi = 2000000L, oiChange = 150000L)
        assertEquals(OptionBuildupType.LONG_BUILDUP, OptionsConfirmationEngine.classifyContractBuildup(callLong))

        // 2. Call Short Buildup (Call Writing): Price DOWN + OI UP
        // Rule: High Call OI addition with price drop is CALL WRITING, NOT BUYING!
        val callWriting = createContract("NIFTY 24850 CE", 24850.0, OptionType.CE, ltp = 120.0, change = -15.0, oi = 3500000L, oiChange = 400000L)
        assertEquals("Price drop with high OI addition is Call Writing (Short Buildup)", OptionBuildupType.SHORT_BUILDUP, OptionsConfirmationEngine.classifyContractBuildup(callWriting))

        // 3. Call Short Covering: Price UP + OI DOWN
        val callCovering = createContract("NIFTY 24850 CE", 24850.0, OptionType.CE, ltp = 160.0, change = 18.0, oi = 1800000L, oiChange = -250000L)
        assertEquals(OptionBuildupType.SHORT_COVERING, OptionsConfirmationEngine.classifyContractBuildup(callCovering))

        // 4. Call Long Unwinding: Price DOWN + OI DOWN
        val callUnwinding = createContract("NIFTY 24850 CE", 24850.0, OptionType.CE, ltp = 110.0, change = -20.0, oi = 1500000L, oiChange = -180000L)
        assertEquals(OptionBuildupType.LONG_UNWINDING, OptionsConfirmationEngine.classifyContractBuildup(callUnwinding))

        // 5. Put Short Buildup (Put Writing): Price DOWN + OI UP
        // Rule: High Put OI addition with price drop is PUT WRITING (Bullish support floor)
        val putWriting = createContract("NIFTY 24850 PE", 24850.0, OptionType.PE, ltp = 115.0, change = -14.0, oi = 3800000L, oiChange = 450000L)
        assertEquals("Price drop with high Put OI addition is Put Writing (Short Buildup)", OptionBuildupType.SHORT_BUILDUP, OptionsConfirmationEngine.classifyContractBuildup(putWriting))

        // 6. Put Long Buildup: Price UP + OI UP (Bearish put buying / hedging)
        val putBuying = createContract("NIFTY 24850 PE", 24850.0, OptionType.PE, ltp = 155.0, change = 16.0, oi = 2200000L, oiChange = 200000L)
        assertEquals(OptionBuildupType.LONG_BUILDUP, OptionsConfirmationEngine.classifyContractBuildup(putBuying))
    }

    @Test
    fun testBullishOptionConfirmation_PcrAndPutWritingFloor() {
        val ceContract = createContract("NIFTY 24850 CE", 24850.0, OptionType.CE, ltp = 148.0, change = 10.0, oi = 2100000L, oiChange = 80000L)
        // Heavier Put writing: price drop on put with heavy OI addition
        val peContract = createContract("NIFTY 24850 PE", 24850.0, OptionType.PE, ltp = 112.0, change = -12.0, oi = 3400000L, oiChange = 380000L)

        val chain = OptionChain(
            spotPrice = 24850.0,
            atmStrike = 24850.0,
            pcr = 1.25, // Bullish PCR
            maxPainStrike = 24850.0,
            totalCallOi = 10000000L,
            totalPutOi = 12500000L,
            totalCallOiChange = 200000L,
            totalPutOiChange = 650000L, // Massive Put writing dominance
            rows = listOf(StrikeRow(24850.0, ceContract, peContract))
        )

        val details = OptionsConfirmationEngine.confirmOptions(24850.0, chain)
        assertTrue("Option confirmation should be available", details.isAvailable)
        assertTrue("Bullish confirmation should trigger with PCR 1.25 & Put writing", details.isBullishConfirmed)
        assertFalse("Bearish confirmation should not trigger", details.isBearishConfirmed)
        assertEquals(OptionBuildupType.SHORT_BUILDUP, details.atmPeBuildup) // Put writing
    }

    @Test
    fun testBearishOptionConfirmation_PcrAndCallWritingCeiling() {
        // Heavy Call writing: price drop on call with heavy OI addition
        val ceContract = createContract("NIFTY 24850 CE", 24850.0, OptionType.CE, ltp = 108.0, change = -16.0, oi = 4200000L, oiChange = 550000L)
        val peContract = createContract("NIFTY 24850 PE", 24850.0, OptionType.PE, ltp = 152.0, change = 14.0, oi = 1900000L, oiChange = 90000L)

        val chain = OptionChain(
            spotPrice = 24850.0,
            atmStrike = 24850.0,
            pcr = 0.78, // Bearish PCR
            maxPainStrike = 24850.0,
            totalCallOi = 14000000L,
            totalPutOi = 10920000L,
            totalCallOiChange = 800000L, // Massive Call writing dominance
            totalPutOiChange = 150000L,
            rows = listOf(StrikeRow(24850.0, ceContract, peContract))
        )

        val details = OptionsConfirmationEngine.confirmOptions(24850.0, chain)
        assertTrue(details.isAvailable)
        assertTrue("Bearish confirmation should trigger with low PCR & Call writing", details.isBearishConfirmed)
        assertFalse(details.isBullishConfirmed)
        assertEquals(OptionBuildupType.SHORT_BUILDUP, details.atmCeBuildup) // Call writing
    }

    @Test
    fun testBidAskSpreadLiquidityFilter_RejectsWideSpread() {
        // Wide illiquid spread: bid 120.0, ask 135.0 (spread ₹15 = > 10% of premium)
        val ceContract = createContract("NIFTY 24850 CE", 24850.0, OptionType.CE, ltp = 125.0, change = 8.0, oi = 1500000L, oiChange = 50000L, bid = 120.0, ask = 135.0)
        val peContract = createContract("NIFTY 24850 PE", 24850.0, OptionType.PE, ltp = 110.0, change = -8.0, oi = 1500000L, oiChange = 80000L)

        val chain = OptionChain(
            spotPrice = 24850.0,
            atmStrike = 24850.0,
            pcr = 1.15,
            maxPainStrike = 24850.0,
            totalCallOi = 10000000L,
            totalPutOi = 11500000L,
            totalCallOiChange = 100000L,
            totalPutOiChange = 300000L,
            rows = listOf(StrikeRow(24850.0, ceContract, peContract))
        )

        val details = OptionsConfirmationEngine.confirmOptions(24850.0, chain)
        assertFalse("Wide bid-ask spread must fail liquidity filter", details.isBidAskAcceptable)
        assertFalse("Cannot confirm bullish trade when bid-ask spread is wide/illiquid", details.isBullishConfirmed)
    }
}
