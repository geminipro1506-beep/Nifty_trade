package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.MarketDirection
import com.cloudscope.moneyprinter.data.model.MarketQuote
import com.cloudscope.moneyprinter.data.model.OptionType
import com.cloudscope.moneyprinter.domain.alert.MarketAlertEngine
import com.cloudscope.moneyprinter.domain.alert.MarketAlertType
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneyConfidence
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignal
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalAction
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SmartMoneyAlertTest {

    private lateinit var alertEngine: MarketAlertEngine

    @Before
    fun setUp() {
        alertEngine = MarketAlertEngine()
    }

    private fun mockQuote(price: Double = 24860.0): MarketQuote {
        return MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = price,
            change = 60.0,
            pChange = 0.24,
            open = 24800.0,
            high = 24870.0,
            low = 24790.0,
            close = 24800.0,
            vwap = 24825.0,
            volume = 450000L
        )
    }

    private fun createSignal(
        action: SmartMoneySignalAction,
        strike: String = "24850 CE (ATM)",
        entry: String = "₹140.00 - ₹145.00",
        sl: String = "₹115.00",
        t1: String = "₹178.00",
        confidence: SmartMoneyConfidence = SmartMoneyConfidence.STRONG,
        reason: String = "Bullish BOS & VWAP Reclaim confirmed by Futures Long Buildup & Put Writing"
    ): SmartMoneySignal {
        return SmartMoneySignal(
            action = action,
            confidence = confidence,
            suggestedStrike = strike,
            strikePrice = 24850.0,
            optionType = if (action == SmartMoneySignalAction.BUY_CE) OptionType.CE else OptionType.PE,
            entryZone = entry,
            entryPrice = 142.5,
            stopLoss = sl,
            stopLossValue = 115.0,
            target1 = t1,
            target1Value = 178.0,
            target2 = "₹200.00",
            target2Value = 200.0,
            shortReason = reason
        )
    }

    @Test
    fun testAlertTrigger_WhenValidBuyCeSignalAppears() {
        val signal = createSignal(SmartMoneySignalAction.BUY_CE, strike = "24850 CE (ATM)")
        val quote = mockQuote(24860.0)

        val alert = alertEngine.evaluateSmartMoneySignal(signal, quote)

        assertNotNull("Alert must be triggered when a valid BUY CE signal appears", alert)
        assertEquals(MarketAlertType.DIRECTION_BULLISH, alert?.type)
        assertEquals(MarketDirection.BULLISH, alert?.newDirection)
        assertTrue("Title should include SMART MONEY and strike", alert?.title?.contains("SMART MONEY") == true)
        assertTrue("Title should mention BUY CE", alert?.title?.contains("BUY CE") == true)
        assertTrue("Message should include entry, SL and T1", alert?.message?.contains("Entry: ₹140.00 - ₹145.00") == true)
        assertTrue("Message should mention SL", alert?.message?.contains("SL: ₹115.00") == true)
        assertTrue("Message should mention T1", alert?.message?.contains("T1: ₹178.00") == true)
        assertEquals(24860.0, alert?.spotPrice ?: 0.0, 0.01)
    }

    @Test
    fun testAlertTrigger_WhenValidBuyPeSignalAppears() {
        val signal = createSignal(
            action = SmartMoneySignalAction.BUY_PE,
            strike = "24750 PE (ATM)",
            entry = "₹135.00 - ₹140.00",
            sl = "₹110.00",
            t1 = "₹172.00",
            confidence = SmartMoneyConfidence.STRONG,
            reason = "Bearish Structure & VWAP Rejection confirmed by Futures Short Buildup & Call Writing"
        )
        val quote = mockQuote(24740.0)

        val alert = alertEngine.evaluateSmartMoneySignal(signal, quote)

        assertNotNull("Alert must be triggered when a valid BUY PE signal appears", alert)
        assertEquals(MarketAlertType.DIRECTION_BEARISH, alert?.type)
        assertEquals(MarketDirection.BEARISH, alert?.newDirection)
        assertTrue(alert?.title?.contains("BUY PE") == true)
        assertTrue(alert?.message?.contains("Entry: ₹135.00 - ₹140.00") == true)
    }

    @Test
    fun testDuplicateAlertSuppression_DoesNotSpamSameSetup() {
        val signal = createSignal(SmartMoneySignalAction.BUY_CE, strike = "24850 CE (ATM)")
        val quote = mockQuote(24860.0)
        val now = 1000000L

        // First evaluation: triggers alert
        val firstAlert = alertEngine.evaluateSmartMoneySignal(signal, quote, now = now)
        assertNotNull("First evaluation must produce an alert", firstAlert)

        // Subsequent evaluation with the identical setup (same strike and action within cooldown)
        val duplicateAlert = alertEngine.evaluateSmartMoneySignal(signal, quote, now = now + 60000L) // 1 min later
        assertNull("Duplicate alert must be suppressed for identical setup", duplicateAlert)
    }

    @Test
    fun testAlertTrigger_WhenSignalChangesOrNewSetupForms() {
        val signal1 = createSignal(SmartMoneySignalAction.BUY_CE, strike = "24850 CE (ATM)")
        val quote = mockQuote(24860.0)
        val now = 1000000L

        val alert1 = alertEngine.evaluateSmartMoneySignal(signal1, quote, now = now)
        assertNotNull(alert1)

        // 1. Signal changes from BUY CE to BUY PE: immediately alerts!
        val signal2 = createSignal(SmartMoneySignalAction.BUY_PE, strike = "24850 PE (ATM)")
        val alert2 = alertEngine.evaluateSmartMoneySignal(signal2, quote, now = now + 30000L)
        assertNotNull("Must alert when directional signal changes to BUY PE", alert2)
        assertEquals(MarketAlertType.DIRECTION_BEARISH, alert2?.type)

        // 2. New setup forms (strike changes from 24850 PE to 24800 PE): immediately alerts!
        val signal3 = createSignal(SmartMoneySignalAction.BUY_PE, strike = "24800 PE (1-step ITM)")
        val alert3 = alertEngine.evaluateSmartMoneySignal(signal3, quote, now = now + 60000L)
        assertNotNull("Must alert when a new setup forms with a different strike", alert3)
        assertTrue(alert3?.title?.contains("24800 PE") == true)
    }

    @Test
    fun testNoTrade_ProducesNoAlertAndResetsState() {
        val signal1 = createSignal(SmartMoneySignalAction.BUY_CE, strike = "24850 CE (ATM)")
        val quote = mockQuote(24860.0)
        val now = 1000000L

        val alert1 = alertEngine.evaluateSmartMoneySignal(signal1, quote, now = now)
        assertNotNull(alert1)

        // Market shifts to NO TRADE
        val noTradeSignal = createSignal(SmartMoneySignalAction.NO_TRADE, strike = "—", reason = "Market rangebound")
        val noTradeAlert = alertEngine.evaluateSmartMoneySignal(noTradeSignal, quote, now = now + 30000L)
        assertNull("NO TRADE must not produce a directional alert", noTradeAlert)
        assertEquals(SmartMoneySignalAction.NO_TRADE, alertEngine.lastSmartMoneyAction)

        // When a new BUY CE setup appears again, it should trigger alert without being blocked
        val signalRebound = createSignal(SmartMoneySignalAction.BUY_CE, strike = "24850 CE (ATM)")
        val alertRebound = alertEngine.evaluateSmartMoneySignal(signalRebound, quote, now = now + 60000L)
        assertNotNull("Must trigger alert when new setup appears after NO TRADE", alertRebound)
    }
}
