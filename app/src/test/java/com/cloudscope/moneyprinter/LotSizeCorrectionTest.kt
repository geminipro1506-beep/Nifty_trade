package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.options.ContractSpecificationService
import com.cloudscope.moneyprinter.domain.risk.RiskManager
import com.cloudscope.moneyprinter.domain.signal.ChargesCalculator
import com.cloudscope.moneyprinter.domain.signal.SignalOutcomeTracker
import org.junit.Assert.*
import org.junit.Test

class LotSizeCorrectionTest {

    @Test
    fun testNiftyAuthorizedLotSizeSpecification() {
        // NSE Authorized NIFTY 50 Lot Size is 65
        assertEquals(65, ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE)
        assertEquals(65, ContractSpecificationService.getLotSize("NIFTY"))

        val spec = ContractSpecificationService.getContractSpecification("NIFTY")
        assertEquals("NIFTY", spec.symbol)
        assertEquals(65, spec.lotSize)
        assertEquals(1800, spec.freezeQuantity)
        assertEquals(50.0, spec.strikeInterval, 0.01)
    }

    @Test
    fun testLotMultiplesCalculation() {
        // 1 lot = 65
        assertEquals(65, ContractSpecificationService.calculateQuantity(1))
        // 2 lots = 130
        assertEquals(130, ContractSpecificationService.calculateQuantity(2))
        // 3 lots = 195
        assertEquals(195, ContractSpecificationService.calculateQuantity(3))
        // 5 lots = 325
        assertEquals(325, ContractSpecificationService.calculateQuantity(5))

        // Quantity validation
        assertTrue(ContractSpecificationService.isValidQuantity(65))
        assertTrue(ContractSpecificationService.isValidQuantity(130))
        assertTrue(ContractSpecificationService.isValidQuantity(195))
        assertTrue(ContractSpecificationService.isValidQuantity(325))

        // Non-multiples must be invalid
        assertFalse(ContractSpecificationService.isValidQuantity(25))
        assertFalse(ContractSpecificationService.isValidQuantity(50))
        assertFalse(ContractSpecificationService.isValidQuantity(100))
    }

    @Test
    fun testRiskManagerUsesCentralizedLotSize() {
        assertEquals(65, RiskManager.NIFTY_LOT_SIZE)

        val capital = 200000.0 // ₹2,00,000 capital
        val signal = TradingSignal(
            action = SignalAction.BUY_CE,
            underlyingSpot = 24850.0,
            recommendedStrike = 24850.0,
            optionType = OptionType.CE,
            contractSymbol = "NIFTY 24850 CE",
            entryPrice = 120.0,
            stopLoss = 100.0, // ₹20 SL
            target1 = 150.0,
            target2 = 170.0,
            riskRewardRatio = 1.5,
            confidencePercent = 80,
            confidenceRating = ConfidenceRating.HIGH,
            status = SignalStatus.ACTIVE,
            marketDirection = MarketDirection.BULLISH,
            summaryFactors = "Breakout",
            confirmationReasons = emptyList(),
            lotSize = 65
        )

        val sizing = RiskManager.calculatePositionSize(capital, signal, riskPercent = 2.0)
        assertTrue(sizing.isApproved)
        assertEquals(sizing.lots * 65, sizing.totalQuantity)
        assertTrue(sizing.totalQuantity % 65 == 0)
    }

    @Test
    fun testChargesCalculationWithLotSize65() {
        val entryPrice = 140.0
        val exitPrice = 182.0 // +42 points
        val lots = 1
        val lotSize = 65

        val (gross, net) = ChargesCalculator.calculateNetPnl(entryPrice, exitPrice, lots, lotSize)
        val charges = ChargesCalculator.calculate(entryPrice, exitPrice, lots, lotSize)

        // Gross = (182.0 - 140.0) * 65 = 42 * 65 = ₹2730.0
        assertEquals(2730.0, gross, 0.01)

        // Brokerage = ₹40 flat
        assertEquals(40.0, charges.brokerage, 0.01)

        // Slippage = 0.5 * 65 * 2 = ₹65.0
        assertEquals(65.0, charges.slippageCost, 0.01)

        // Reconciled: Gross - Charges = Net
        assertEquals(gross - charges.totalCharges, net, 0.05)
    }

    @Test
    fun testHistoricalSignalRecordTotalQuantity() {
        val record1 = HistoricalSignalRecord(
            id = "REC-01",
            timestamp = System.currentTimeMillis(),
            spotPrice = 24800.0,
            marketDirection = MarketDirection.BULLISH,
            action = SignalAction.BUY_CE,
            contractSymbol = "NIFTY 24800 CE",
            strikePrice = 24800.0,
            optionType = OptionType.CE,
            expiryDate = "Current Weekly",
            premiumAtSignal = 130.0,
            entryPrice = 130.0,
            stopLoss = 100.0,
            target1 = 170.0,
            target2 = 190.0,
            target3 = 220.0,
            confidenceScore = 80,
            rsi14 = 60.0,
            vwap = 24780.0,
            ema9 = 24805.0,
            ema20 = 24790.0,
            ema50 = 24770.0,
            pcr = 1.2,
            vix = 14.0,
            reasons = listOf("Confluence"),
            invalidationConditions = "Drop below VWAP",
            lots = 1,
            lotSize = 65
        )
        assertEquals(65, record1.totalQuantity)

        val record3 = record1.copy(lots = 3)
        assertEquals(195, record3.totalQuantity)

        val record5 = record1.copy(lots = 5)
        assertEquals(325, record5.totalQuantity)
    }
}
