package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.data.model.MarketQuote
import com.cloudscope.moneyprinter.data.validator.MarketDataValidator
import org.junit.Assert.*
import org.junit.Test

class MarketDataValidatorTest {

    @Test
    fun testValidQuotePasses() {
        val quote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = 24850.0,
            change = 35.0,
            pChange = 0.14,
            open = 24820.0,
            high = 24890.0,
            low = 24800.0,
            close = 24815.0,
            timestamp = System.currentTimeMillis()
        )

        val result = MarketDataValidator.validateQuote(quote)
        assertTrue(result.isValid)
        assertFalse(result.isStale)
    }

    @Test
    fun testNegativePriceRejected() {
        val quote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = -100.0,
            change = 0.0,
            pChange = 0.0,
            open = 24820.0,
            high = 24890.0,
            low = 24800.0,
            close = 24815.0,
            timestamp = System.currentTimeMillis()
        )

        val result = MarketDataValidator.validateQuote(quote)
        assertFalse(result.isValid)
        assertTrue(result.reason.contains("Invalid spot price"))
    }

    @Test
    fun testAbnormalPriceJumpRejected() {
        val prevQuote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = 24850.0,
            change = 0.0,
            pChange = 0.0,
            open = 24820.0,
            high = 24890.0,
            low = 24800.0,
            close = 24815.0,
            timestamp = System.currentTimeMillis()
        )

        val jumpQuote = prevQuote.copy(
            lastPrice = 30000.0 // > 20% jump instantly
        )

        val result = MarketDataValidator.validateQuote(jumpQuote, prevQuote)
        assertFalse(result.isValid)
        assertTrue(result.reason.contains("Abnormal price jump"))
    }

    @Test
    fun testStaleTickRejected() {
        val now = System.currentTimeMillis()
        val staleQuote = MarketQuote(
            symbol = "NIFTY 50",
            lastPrice = 24850.0,
            change = 0.0,
            pChange = 0.0,
            open = 24820.0,
            high = 24890.0,
            low = 24800.0,
            close = 24815.0,
            timestamp = now - 300_000L // 5 mins old
        )

        val result = MarketDataValidator.validateQuote(staleQuote, currentTimestamp = now)
        assertFalse(result.isValid)
        assertTrue(result.isStale)
        assertTrue(result.reason.contains("Stale tick data"))
    }
}
