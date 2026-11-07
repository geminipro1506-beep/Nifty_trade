package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.domain.news.NewsImpactLevel
import com.cloudscope.moneyprinter.domain.news.NewsSentiment
import com.cloudscope.moneyprinter.domain.news.NewsSentimentAnalyzer
import org.junit.Assert.*
import org.junit.Test

class NewsSentimentAnalyzerTest {

    @Test
    fun testRbiUnchangedRateIsPositive() {
        val headline = "RBI keeps interest rates unchanged, supporting market sentiment"
        val (sentiment, impact) = NewsSentimentAnalyzer.analyzeSentiment(headline)
        assertEquals(NewsSentiment.POSITIVE, sentiment)
        assertEquals(NewsImpactLevel.HIGH, impact)
        assertEquals("🟢 +VE | $headline", "${sentiment.prefix}$headline")
    }

    @Test
    fun testCrudeOilRisingIsNegativeForIndia() {
        val headline = "Rising crude oil prices create pressure on Indian equities"
        val (sentiment, impact) = NewsSentimentAnalyzer.analyzeSentiment(headline)
        assertEquals(NewsSentiment.NEGATIVE, sentiment)
        assertEquals(NewsImpactLevel.HIGH, impact)
        assertEquals("🔴 -VE | $headline", "${sentiment.prefix}$headline")
    }

    @Test
    fun testCrudeOilTumblingIsPositiveForIndia() {
        val headline = "Brent crude oil prices slide toward $70 per barrel on supply surplus"
        val (sentiment, impact) = NewsSentimentAnalyzer.analyzeSentiment(headline)
        assertEquals(NewsSentiment.POSITIVE, sentiment)
        assertEquals(NewsImpactLevel.HIGH, impact)
    }

    @Test
    fun testFiiInflowsPositive() {
        val headline = "FIIs turn aggressive buyers with massive ₹3,500 crore inflow into banking stocks"
        val (sentiment, impact) = NewsSentimentAnalyzer.analyzeSentiment(headline)
        assertEquals(NewsSentiment.POSITIVE, sentiment)
        assertEquals(NewsImpactLevel.HIGH, impact)
    }

    @Test
    fun testFiiOutflowsNegative() {
        val headline = "FIIs dump ₹4,200 crore in equities amid global risk-off sentiment"
        val (sentiment, impact) = NewsSentimentAnalyzer.analyzeSentiment(headline)
        assertEquals(NewsSentiment.NEGATIVE, sentiment)
        assertEquals(NewsImpactLevel.HIGH, impact)
    }

    @Test
    fun testNarrowRangeIsNeutral() {
        val headline = "NIFTY trades in a narrow range ahead of economic data"
        val (sentiment, impact) = NewsSentimentAnalyzer.analyzeSentiment(headline)
        assertEquals(NewsSentiment.NEUTRAL, sentiment)
        assertEquals("⚪ NEUTRAL | $headline", "${sentiment.prefix}$headline")
    }

    @Test
    fun testFormatEpochToIst() {
        // 1730955600000L = 2024-11-07 10:30:00 IST
        val istString = NewsSentimentAnalyzer.formatEpochToIst(1730955600000L)
        assertTrue(istString.contains("IST"))
    }
}
