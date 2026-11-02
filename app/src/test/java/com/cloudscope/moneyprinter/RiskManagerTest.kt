package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.risk.RiskManager
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class RiskManagerTest {

    @Test
    fun testPositionSizing_TwoPercentRisk() {
        val capital = 100000.0 // ₹1,00,000 capital
        val signal = TradingSignal(
            action = SignalAction.BUY_CE,
            underlyingSpot = 24850.0,
            recommendedStrike = 24850.0,
            optionType = OptionType.CE,
            contractSymbol = "NIFTY 24850 CE",
            entryPrice = 120.0,
            stopLoss = 95.0, // ₹25 risk per contract
            target1 = 155.0,
            target2 = 180.0,
            riskRewardRatio = 1.4,
            confidencePercent = 78,
            confidenceRating = ConfidenceRating.HIGH,
            status = SignalStatus.ACTIVE,
            marketDirection = MarketDirection.BULLISH,
            summaryFactors = "Test",
            confirmationReasons = emptyList()
        )

        // 2% of ₹1,00,000 = ₹2,000 max risk
        // Risk per lot = ₹25 * 25 (lot size) = ₹625
        // Allowed lots = floor(2000 / 625) = 3 lots (75 qty)
        val sizing = RiskManager.calculatePositionSize(capital, signal, riskPercent = 2.0)

        assertEquals(3, sizing.lots)
        assertEquals(75, sizing.totalQuantity)
        assertEquals(1875.0, sizing.maxRiskAmount, 0.01) // 75 * ₹25 = ₹1875 <= ₹2000
        assertTrue(sizing.riskPercentOfCapital <= 2.0)
    }

    @Test
    fun testAutoSquareOffTriggerAt1515IST() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        cal.set(Calendar.HOUR_OF_DAY, 15)
        cal.set(Calendar.MINUTE, 16)
        assertTrue("At 15:16 IST, auto square-off must be active", RiskManager.isAutoSquareOffTime(cal.timeInMillis))

        cal.set(Calendar.HOUR_OF_DAY, 11)
        cal.set(Calendar.MINUTE, 30)
        assertFalse("At 11:30 IST, auto square-off must not be active", RiskManager.isAutoSquareOffTime(cal.timeInMillis))
    }
}
