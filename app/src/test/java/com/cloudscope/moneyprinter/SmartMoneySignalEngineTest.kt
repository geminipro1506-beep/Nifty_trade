package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.MarketQuote
import com.cloudscope.moneyprinter.data.model.OptionChain
import com.cloudscope.moneyprinter.data.model.OptionContract
import com.cloudscope.moneyprinter.data.model.OptionType
import com.cloudscope.moneyprinter.data.model.StrikeRow
import com.cloudscope.moneyprinter.domain.smartmoney.*
import org.junit.Assert.*
import org.junit.Test

class SmartMoneySignalEngineTest {

    private fun createOptionChain(
        spot: Double,
        atmStrike: Double,
        ceLtp: Double,
        ceChange: Double,
        ceOiChange: Long,
        peLtp: Double,
        peChange: Double,
        peOiChange: Long,
        pcr: Double
    ): OptionChain {
        val ceContract = OptionContract(
            symbol = "NIFTY ${atmStrike.toInt()} CE",
            strikePrice = atmStrike,
            optionType = OptionType.CE,
            expiryDate = "07 NOV 2024",
            lastPrice = ceLtp,
            change = ceChange,
            pChange = 5.0,
            openInterest = 2500000L,
            changeInOi = ceOiChange,
            volume = 800000L,
            impliedVolatility = 14.0,
            delta = 0.52,
            gamma = 0.002,
            theta = -8.5,
            vega = 12.0,
            bidPrice = ceLtp - 0.50,
            askPrice = ceLtp + 0.50,
            bidQty = 1800,
            askQty = 1800
        )

        val peContract = OptionContract(
            symbol = "NIFTY ${atmStrike.toInt()} PE",
            strikePrice = atmStrike,
            optionType = OptionType.PE,
            expiryDate = "07 NOV 2024",
            lastPrice = peLtp,
            change = peChange,
            pChange = -4.5,
            openInterest = 3200000L,
            changeInOi = peOiChange,
            volume = 750000L,
            impliedVolatility = 14.5,
            delta = -0.48,
            gamma = 0.002,
            theta = -8.2,
            vega = 11.8,
            bidPrice = peLtp - 0.50,
            askPrice = peLtp + 0.50,
            bidQty = 1600,
            askQty = 1600
        )

        return OptionChain(
            spotPrice = spot,
            atmStrike = atmStrike,
            pcr = pcr,
            maxPainStrike = atmStrike,
            totalCallOi = 10000000L,
            totalPutOi = (10000000L * pcr).toLong(),
            totalCallOiChange = ceOiChange,
            totalPutOiChange = peOiChange,
            rows = listOf(StrikeRow(atmStrike, ceContract, peContract))
        )
    }

    @Test
    fun testGenerateSignal_BuyCeOnStrongBullishConfirmation() {
        val spotQuote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = 24860.0,
            change = 60.0,
            pChange = 0.24,
            open = 24800.0,
            high = 24870.0,
            low = 24790.0,
            close = 24800.0,
            vwap = 24825.0,
            volume = 450000L
        )

        val activity = SmartMoneyActivity(
            spotPrice = 24860.0,
            spotVolume = 450000L,
            isUnusualVolume = true,
            volumeRatio = 1.8,
            vwap = 24825.0,
            vwapState = VwapRelationship.RECLAIM,
            liquiditySweep = LiquiditySweepState.NONE,
            marketStructure = MarketStructureState.BULLISH_STRUCTURE,
            futuresPrice = 24898.0,
            futuresOi = 13500000L,
            futuresOiChange = 250000L,
            futuresOiChangePercent = 1.9,
            priceOiRegime = PriceOiRegime.LONG_BUILDUP,
            summary = "Long Buildup • Bullish Structure • Unusual Vol"
        )

        // Option Chain: Put writing (pe change -10, pe Oi +400k) with PCR 1.25
        val chain = createOptionChain(
            spot = 24860.0,
            atmStrike = 24850.0,
            ceLtp = 145.0,
            ceChange = 12.0,
            ceOiChange = 50000L,
            peLtp = 110.0,
            peChange = -10.0,
            peOiChange = 400000L, // Put writing
            pcr = 1.25
        )

        val optionsDetails = OptionsConfirmationEngine.confirmOptions(24860.0, chain)
        val signal = SmartMoneySignalEngine.generateSignal(spotQuote, activity, optionsDetails, chain)

        assertEquals("Should generate BUY CE signal", SmartMoneySignalAction.BUY_CE, signal.action)
        assertEquals(SmartMoneyConfidence.STRONG, signal.confidence)
        assertTrue("Suggested strike should be ATM or 1-step ITM", signal.suggestedStrike.contains("24850 CE") || signal.suggestedStrike.contains("24800 CE"))
        assertTrue("Entry zone should be formatted with rupee sign", signal.entryZone.startsWith("₹"))
        assertTrue("Stop loss should be formatted", signal.stopLoss.startsWith("₹"))
        assertTrue("Target 1 should be formatted", signal.target1.startsWith("₹"))
        assertTrue("Target 2 should be formatted", signal.target2.startsWith("₹"))
        assertTrue("Target 1 should exceed Entry Price", signal.target1Value > signal.entryPrice)
        assertTrue("Target 2 should exceed Target 1", signal.target2Value > signal.target1Value)
        assertTrue("Entry price should exceed Stop Loss", signal.entryPrice > signal.stopLossValue)
        assertTrue("Short reason should mention smart money confirmation and Put writing", signal.shortReason.contains("Put Writing") || signal.shortReason.contains("Long Buildup"))
        assertTrue("Net Risk/Reward should be calculated", signal.netRiskRewardRatio > 0.0)
    }

    @Test
    fun testGenerateSignal_BuyPeOnStrongBearishConfirmation() {
        val spotQuote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = 24760.0,
            change = -70.0,
            pChange = -0.28,
            open = 24830.0,
            high = 24840.0,
            low = 24750.0,
            close = 24830.0,
            vwap = 24810.0,
            volume = 520000L
        )

        val activity = SmartMoneyActivity(
            spotPrice = 24760.0,
            spotVolume = 520000L,
            isUnusualVolume = true,
            volumeRatio = 2.1,
            vwap = 24810.0,
            vwapState = VwapRelationship.REJECTION,
            liquiditySweep = LiquiditySweepState.NONE,
            marketStructure = MarketStructureState.BEARISH_STRUCTURE,
            futuresPrice = 24795.0,
            futuresOi = 13800000L,
            futuresOiChange = 310000L,
            futuresOiChangePercent = 2.3,
            priceOiRegime = PriceOiRegime.SHORT_BUILDUP,
            summary = "Short Buildup • Bearish Structure • Unusual Vol"
        )

        // Option Chain: Call writing (ce change -14, ce Oi +450k) with PCR 0.80
        val chain = createOptionChain(
            spot = 24760.0,
            atmStrike = 24750.0,
            ceLtp = 95.0,
            ceChange = -14.0,
            ceOiChange = 450000L, // Call writing
            peLtp = 150.0,
            peChange = 15.0,
            peOiChange = 80000L,
            pcr = 0.80
        )

        val optionsDetails = OptionsConfirmationEngine.confirmOptions(24760.0, chain)
        val signal = SmartMoneySignalEngine.generateSignal(spotQuote, activity, optionsDetails, chain)

        assertEquals("Should generate BUY PE signal", SmartMoneySignalAction.BUY_PE, signal.action)
        assertEquals(SmartMoneyConfidence.STRONG, signal.confidence)
        assertTrue("Suggested strike should be ATM or 1-step ITM PE", signal.suggestedStrike.contains("24750 PE") || signal.suggestedStrike.contains("24800 PE"))
        assertTrue(signal.target1Value > signal.entryPrice)
        assertTrue(signal.entryPrice > signal.stopLossValue)
        assertTrue(signal.shortReason.contains("Call Writing") || signal.shortReason.contains("Short Buildup"))
    }

    @Test
    fun testConflictingSignals_TriggersNoTrade() {
        val spotQuote = MarketQuote(
            lastPrice = 24860.0,
            change = 40.0,
            pChange = 0.16,
            open = 24820.0,
            high = 24870.0,
            low = 24810.0,
            close = 24820.0,
            vwap = 24830.0,
            volume = 200000L
        )

        // Spot appears bullish above VWAP...
        val activity = SmartMoneyActivity(
            spotPrice = 24860.0,
            spotVolume = 200000L,
            isUnusualVolume = false,
            volumeRatio = 1.0,
            vwap = 24830.0,
            vwapState = VwapRelationship.ABOVE_VWAP,
            liquiditySweep = LiquiditySweepState.NONE,
            marketStructure = MarketStructureState.BULLISH_STRUCTURE,
            futuresPrice = 24895.0,
            futuresOi = 12500000L,
            futuresOiChange = 10000L,
            futuresOiChangePercent = 0.1,
            priceOiRegime = PriceOiRegime.LONG_BUILDUP,
            summary = "Long Buildup"
        )

        // ...BUT Option Chain reveals heavy Call Writing (resistance at ATM) and low PCR 0.75!
        val conflictingChain = createOptionChain(
            spot = 24860.0,
            atmStrike = 24850.0,
            ceLtp = 110.0,
            ceChange = -10.0,
            ceOiChange = 500000L, // Massive Call Writing resistance
            peLtp = 140.0,
            peChange = 8.0,
            peOiChange = 50000L,
            pcr = 0.75
        )

        val optionsDetails = OptionsConfirmationEngine.confirmOptions(24860.0, conflictingChain)
        val signal = SmartMoneySignalEngine.generateSignal(spotQuote, activity, optionsDetails, conflictingChain)

        assertEquals("Conflicting signals must produce NO TRADE", SmartMoneySignalAction.NO_TRADE, signal.action)
        assertTrue("Reason must explain the conflict", signal.shortReason.contains("Conflicting signals"))
        assertEquals("—", signal.suggestedStrike)
        assertEquals("—", signal.entryZone)
        assertEquals("—", signal.stopLoss)
    }

    @Test
    fun testDoNotGenerateSignalsOnVwapAlone_RuleEnforcement() {
        val spotQuote = MarketQuote(
            lastPrice = 24835.0,
            change = 10.0,
            pChange = 0.04,
            open = 24820.0,
            high = 24840.0,
            low = 24810.0,
            close = 24825.0,
            vwap = 24820.0, // spot is slightly above VWAP
            volume = 110000L
        )

        // Price is slightly above VWAP, but market structure is RANGEBOUND and Futures OI is NEUTRAL
        val activity = SmartMoneyActivity(
            spotPrice = 24835.0,
            spotVolume = 110000L,
            isUnusualVolume = false,
            volumeRatio = 0.9,
            vwap = 24820.0,
            vwapState = VwapRelationship.ABOVE_VWAP,
            liquiditySweep = LiquiditySweepState.NONE,
            marketStructure = MarketStructureState.RANGEBOUND,
            futuresPrice = 24870.0,
            futuresOi = 12500000L,
            futuresOiChange = 2000L,
            futuresOiChangePercent = 0.02,
            priceOiRegime = PriceOiRegime.NEUTRAL,
            summary = "Neutral"
        )

        val neutralChain = createOptionChain(
            spot = 24835.0,
            atmStrike = 24850.0,
            ceLtp = 130.0,
            ceChange = 1.0,
            ceOiChange = 10000L,
            peLtp = 120.0,
            peChange = -1.0,
            peOiChange = 15000L,
            pcr = 1.02
        )

        val optionsDetails = OptionsConfirmationEngine.confirmOptions(24835.0, neutralChain)
        val signal = SmartMoneySignalEngine.generateSignal(spotQuote, activity, optionsDetails, neutralChain)

        assertEquals("Must NOT generate BUY CE on VWAP alone", SmartMoneySignalAction.NO_TRADE, signal.action)
        assertTrue("Reason should mention rule or sideways market", signal.shortReason.contains("Sideways") || signal.shortReason.contains("Insufficient confirmation") || signal.shortReason.contains("VWAP"))
    }

    @Test
    fun testOptionChainUnavailable_ProducesNoTradeWithUnavailableIndicator() {
        val spotQuote = MarketQuote(
            lastPrice = 24860.0,
            change = 60.0,
            pChange = 0.24,
            open = 24800.0,
            high = 24870.0,
            low = 24790.0,
            close = 24800.0,
            vwap = 24825.0,
            volume = 450000L
        )

        val activity = SmartMoneyActivity(
            spotPrice = 24860.0,
            spotVolume = 450000L,
            isUnusualVolume = true,
            volumeRatio = 1.8,
            vwap = 24825.0,
            vwapState = VwapRelationship.RECLAIM,
            liquiditySweep = LiquiditySweepState.NONE,
            marketStructure = MarketStructureState.BULLISH_STRUCTURE,
            futuresPrice = 24898.0,
            futuresOi = 13500000L,
            futuresOiChange = 250000L,
            futuresOiChangePercent = 1.9,
            priceOiRegime = PriceOiRegime.LONG_BUILDUP,
            summary = "Long Buildup"
        )

        val unavailableOptions = OptionConfirmationDetails(
            isAvailable = false,
            pcr = 0.0,
            atmStrike = 0.0,
            atmCeBuildup = OptionBuildupType.NEUTRAL,
            atmPeBuildup = OptionBuildupType.NEUTRAL,
            atmCeVolume = 0L,
            atmPeVolume = 0L,
            atmCeChange = 0.0,
            atmPeChange = 0.0,
            atmCeBidAskSpread = 0.0,
            atmPeBidAskSpread = 0.0,
            isBidAskAcceptable = false,
            isBullishConfirmed = false,
            isBearishConfirmed = false,
            confirmationReason = "Option-chain confirmation unavailable"
        )

        val signal = SmartMoneySignalEngine.generateSignal(spotQuote, activity, unavailableOptions, null)

        assertEquals(SmartMoneySignalAction.NO_TRADE, signal.action)
        assertFalse("Option confirmation available must be false", signal.optionConfirmationAvailable)
        assertTrue("Reason must state option-chain confirmation unavailable", signal.shortReason.contains("unavailable"))
    }
}
