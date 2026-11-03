package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.domain.signal.ChargesCalculator
import org.junit.Assert.*
import org.junit.Test

class ChargesCalculatorTest {

    @Test
    fun testStatutoryChargesCalculation() {
        val charges = ChargesCalculator.calculate(
            buyPrice = 120.0,
            sellPrice = 160.0,
            lots = 1,
            lotSize = 25
        )

        // Brokerage should be ₹40 round-trip (₹20 buy + ₹20 sell)
        assertEquals(40.0, charges.brokerage, 0.01)

        // STT should be 0.1% on sell turnover: 25 * 160 * 0.001 = ₹4.0
        assertEquals(4.0, charges.stt, 0.01)

        // Slippage: 0.5 index points * 25 * 2 = ₹25.0
        assertEquals(25.0, charges.slippageCost, 0.01)

        // Total charges must be positive and greater than brokerage
        assertTrue(charges.totalCharges > 70.0)
    }

    @Test
    fun testNetPnlDeductsAllFriction() {
        val (gross, net) = ChargesCalculator.calculateNetPnl(
            entryPrice = 100.0,
            exitPrice = 140.0,
            lots = 1,
            lotSize = 25
        )

        // Gross = (140 - 100) * 25 = ₹1000
        assertEquals(1000.0, gross, 0.01)

        // Net should be less than gross by the exact total charges
        assertTrue(net < gross)
        assertTrue(net > 900.0)
    }

    @Test
    fun testLosingTradeReflectsCharges() {
        val (gross, net) = ChargesCalculator.calculateNetPnl(
            entryPrice = 120.0,
            exitPrice = 90.0,
            lots = 1,
            lotSize = 25
        )

        // Gross = (90 - 120) * 25 = -₹750
        assertEquals(-750.0, gross, 0.01)

        // Net loss should be worse than gross loss due to friction
        assertTrue(net < gross)
    }

    @Test
    fun testAuthorizedLotSize65Calculations() {
        // 1 Lot = 65 Qty
        val (gross1, _) = ChargesCalculator.calculateNetPnl(100.0, 110.0, lots = 1, lotSize = 65)
        assertEquals(650.0, gross1, 0.01)

        // 2 Lots = 130 Qty
        val (gross2, _) = ChargesCalculator.calculateNetPnl(100.0, 110.0, lots = 2, lotSize = 65)
        assertEquals(1300.0, gross2, 0.01)

        // 3 Lots = 195 Qty
        val (gross3, _) = ChargesCalculator.calculateNetPnl(100.0, 110.0, lots = 3, lotSize = 65)
        assertEquals(1950.0, gross3, 0.01)

        // 5 Lots = 325 Qty
        val (gross5, _) = ChargesCalculator.calculateNetPnl(100.0, 110.0, lots = 5, lotSize = 65)
        assertEquals(3250.0, gross5, 0.01)
    }

    @Test
    fun testDefaultLotSizeUsesAuthorized65() {
        val charges = ChargesCalculator.calculate(
            buyPrice = 100.0,
            sellPrice = 120.0,
            lots = 1
        )
        // Default lot size should be 65, so turnover is buy: 6500, sell: 7800
        // STT = 7800 * 0.001 = 7.8
        assertEquals(7.8, charges.stt, 0.01)
        // Slippage = 0.5 * 65 * 2 = 65.0
        assertEquals(65.0, charges.slippageCost, 0.01)
    }
}
