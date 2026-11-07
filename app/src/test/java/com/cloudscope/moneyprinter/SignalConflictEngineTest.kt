package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.OptionType
import com.cloudscope.moneyprinter.domain.normaloptions.NormalOptionsSignal
import com.cloudscope.moneyprinter.domain.normaloptions.NormalSignalAction
import com.cloudscope.moneyprinter.domain.signal.SignalConflictEngine
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneyConfidence
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignal
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalAction
import org.junit.Assert.*
import org.junit.Test

class SignalConflictEngineTest {

    @Test
    fun testConflictDetectedWhenNormalBuyCallAndSmartMoneyBuyPe() {
        val normalSignal = NormalOptionsSignal(
            action = NormalSignalAction.BUY_CALL,
            confidencePercent = 82,
            suggestedStrike = 24850.0,
            optionType = OptionType.CE,
            expiryDate = "10 NOV 2026",
            entryPrice = 145.0,
            stopLoss = 120.0,
            target1 = 180.0,
            target2 = 205.0,
            riskRewardRatio = "1 : 1.5",
            signalReason = "Bullish EMA crossover + Price > VWAP"
        )

        val smartMoneySignal = SmartMoneySignal(
            action = SmartMoneySignalAction.BUY_PE,
            confidence = SmartMoneyConfidence.STRONG,
            suggestedStrike = "24850 PE",
            strikePrice = 24850.0,
            optionType = OptionType.PE,
            expiryDate = "10 NOV 2026",
            entryZone = "₹130",
            entryPrice = 130.0,
            stopLoss = "₹105",
            target1 = "₹165",
            target2 = "₹190",
            shortReason = "Heavy institutional call writing + short buildup"
        )

        val conflict = SignalConflictEngine.detectConflict(normalSignal, smartMoneySignal)
        assertTrue(conflict.hasConflict)
        assertTrue(conflict.conflictReason.contains("SIGNAL CONFLICT"))
        assertTrue(conflict.conflictReason.contains("do not force a trade"))
    }

    @Test
    fun testConflictDetectedWhenNormalBuyPutAndSmartMoneyBuyCe() {
        val normalSignal = NormalOptionsSignal(
            action = NormalSignalAction.BUY_PUT,
            confidencePercent = 78,
            suggestedStrike = 24800.0,
            optionType = OptionType.PE,
            expiryDate = "10 NOV 2026",
            entryPrice = 130.0,
            stopLoss = 110.0,
            target1 = 160.0,
            target2 = 185.0,
            riskRewardRatio = "1 : 1.5",
            signalReason = "Bearish EMA crossover + Price < VWAP"
        )

        val smartMoneySignal = SmartMoneySignal(
            action = SmartMoneySignalAction.BUY_CE,
            confidence = SmartMoneyConfidence.STRONG,
            suggestedStrike = "24800 CE",
            strikePrice = 24800.0,
            optionType = OptionType.CE,
            expiryDate = "10 NOV 2026",
            entryZone = "₹140",
            entryPrice = 140.0,
            stopLoss = "₹115",
            target1 = "₹175",
            target2 = "₹200",
            shortReason = "Heavy institutional put writing floor + long accumulation"
        )

        val conflict = SignalConflictEngine.detectConflict(normalSignal, smartMoneySignal)
        assertTrue(conflict.hasConflict)
    }

    @Test
    fun testNoConflictWhenBothBullish() {
        val normalSignal = NormalOptionsSignal(
            action = NormalSignalAction.BUY_CALL,
            confidencePercent = 85,
            suggestedStrike = 24850.0,
            optionType = OptionType.CE,
            expiryDate = "10 NOV 2026",
            entryPrice = 145.0,
            stopLoss = 120.0,
            target1 = 180.0,
            target2 = 205.0,
            riskRewardRatio = "1 : 1.5",
            signalReason = "Bullish setup"
        )

        val smartMoneySignal = SmartMoneySignal(
            action = SmartMoneySignalAction.BUY_CE,
            confidence = SmartMoneyConfidence.STRONG,
            suggestedStrike = "24850 CE",
            strikePrice = 24850.0,
            optionType = OptionType.CE,
            expiryDate = "10 NOV 2026",
            entryZone = "₹145",
            entryPrice = 145.0,
            stopLoss = "₹120",
            target1 = "₹180",
            target2 = "₹205",
            shortReason = "Institutional accumulation"
        )

        val conflict = SignalConflictEngine.detectConflict(normalSignal, smartMoneySignal)
        assertFalse(conflict.hasConflict)
    }

    @Test
    fun testNoConflictWhenOneIsNoTrade() {
        val normalSignal = NormalOptionsSignal.noTrade("Awaiting confirmation")
        val smartMoneySignal = SmartMoneySignal(
            action = SmartMoneySignalAction.BUY_CE,
            confidence = SmartMoneyConfidence.STRONG,
            suggestedStrike = "24850 CE",
            strikePrice = 24850.0,
            optionType = OptionType.CE,
            expiryDate = "10 NOV 2026",
            entryZone = "₹145",
            entryPrice = 145.0,
            stopLoss = "₹120",
            target1 = "₹180",
            target2 = "₹205",
            shortReason = "Institutional accumulation"
        )

        val conflict = SignalConflictEngine.detectConflict(normalSignal, smartMoneySignal)
        assertFalse(conflict.hasConflict)
    }
}
