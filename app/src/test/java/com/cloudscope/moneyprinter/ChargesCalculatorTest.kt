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
}
