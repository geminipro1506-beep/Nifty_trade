package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.risk.RiskManager
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class RiskManagerTest {

    @Test
    fun testPositionSizing_TwoPercentRisk_WithFriction() {
        val capital = 100000.0 // ₹1,00,000 capital
        val signal = TradingSignal(
            action = SignalAction.BUY_CE,
            underlyingSpot = 24850.0,
            recommendedStrike = 24850.0,
            optionType = OptionType.CE,
            contractSymbol = "NIFTY 24850 CE",
            entryPrice = 120.0,
            stopLoss = 95.0, // ₹25 market risk per contract
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
        // Risk per lot = (₹25 SL + 1.0pt slippage) * 25 + charges (~₹52) ≈ ₹702
        // Allowed lots = floor(2000 / 702) = 2 lots (50 qty)
        val sizing = RiskManager.calculatePositionSize(capital, signal, riskPercent = 2.0)

        assertEquals(2, sizing.lots)
        assertEquals(50, sizing.totalQuantity)
        assertTrue(sizing.isApproved)
        assertTrue(sizing.riskPercentOfCapital <= 2.0)
    }

    @Test
    fun testPositionSizing_RejectsTradeWhenSingleLotBreachesRiskLimit() {
        val capital = 10000.0 // small ₹10,000 account
        val signal = TradingSignal(
            action = SignalAction.BUY_CE,
            underlyingSpot = 24850.0,
            recommendedStrike = 24850.0,
            optionType = OptionType.CE,
            contractSymbol = "NIFTY 24850 CE",
            entryPrice = 150.0,
            stopLoss = 100.0, // ₹50 SL distance! 50 * 25 = ₹1250 loss + friction
            target1 = 200.0,
            target2 = 230.0,
            riskRewardRatio = 1.0,
            confidencePercent = 95, // Even with 95% AI confidence!
            confidenceRating = ConfidenceRating.HIGH,
            status = SignalStatus.ACTIVE,
            marketDirection = MarketDirection.BULLISH,
            summaryFactors = "High confidence breakout",
            confirmationReasons = emptyList()
        )

        // Max 2% of ₹10,000 = ₹200 risk budget
        // Single lot risk is > ₹1250, which grossly exceeds ₹200
        val sizing = RiskManager.calculatePositionSize(capital, signal, riskPercent = 2.0)

        assertEquals(0, sizing.lots)
        assertEquals(0, sizing.totalQuantity)
        assertFalse(sizing.isApproved)
        assertNotNull(sizing.riskRejectionReason)
    }

    @Test
    fun testConfidenceScoreNeverOverridesRiskCeiling() {
        val capital = 100000.0
        val evalLowConf = RiskManager.calculateInstitutionalPositionSize(
            actualCapital = capital,
            entryPrice = 120.0,
            stopLoss = 95.0,
            confidenceScore = 55
        )
        val evalHighConf = RiskManager.calculateInstitutionalPositionSize(
            actualCapital = capital,
            entryPrice = 120.0,
            stopLoss = 95.0,
            confidenceScore = 99
        )

        // High AI confidence (99%) must NEVER increase allowed lots beyond risk budget
        assertEquals(evalLowConf.approvedLots, evalHighConf.approvedLots)
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
