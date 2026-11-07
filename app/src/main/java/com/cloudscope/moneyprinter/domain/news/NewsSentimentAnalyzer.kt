package com.cloudscope.moneyprinter.domain.news

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object NewsSentimentAnalyzer {

    val istTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    /**
     * Contextual sentiment analysis tailored specifically for Indian Equities and NIFTY 50.
     * Evaluates economic context, constituent weightings, and macroeconomic implications.
     */
    fun analyzeSentiment(
        headline: String,
        summary: String = ""
    ): Pair<NewsSentiment, NewsImpactLevel> {
        val text = "$headline $summary".lowercase(Locale.ENGLISH)

        // 1. High-Impact Macroeconomic Drivers for NIFTY

        // Crude Oil Context (India imports ~85% of crude; lower prices benefit NIFTY, higher prices hurt)
        if (text.contains("crude") || text.contains("brent") || text.contains("oil prices")) {
            val isSpike = text.contains("surge") || text.contains("spike") || text.contains("jump") ||
                    text.contains("rise") || text.contains("rising") || text.contains("rises") ||
                    text.contains("climbs") || text.contains("higher") ||
                    text.contains("supply cut") || text.contains("opec cut")
            val isDrop = text.contains("slump") || text.contains("drop") || text.contains("fall") ||
                    text.contains("slide") || text.contains("tumble") || text.contains("cool") ||
                    text.contains("decline") || text.contains("lower")

            if (isSpike && !isDrop) {
                return Pair(NewsSentiment.NEGATIVE, NewsImpactLevel.HIGH)
            } else if (isDrop && !isSpike) {
                return Pair(NewsSentiment.POSITIVE, NewsImpactLevel.HIGH)
            }
        }

        // RBI / Monetary Policy Context
        if (text.contains("rbi") || text.contains("reserve bank") || text.contains("repo rate") || text.contains("mpc")) {
            val isDovish = text.contains("unchanged") || text.contains("rate cut") || text.contains("cut rate") ||
                    text.contains("supportive") || text.contains("dovish") || text.contains("liquidity support") ||
                    text.contains("pause")
            val isHawkish = text.contains("rate hike") || text.contains("hikes") || text.contains("tightening") ||
                    text.contains("inflation worry") || text.contains("curb inflation") || text.contains("hawkish")

            if (isDovish && !isHawkish) {
                return Pair(NewsSentiment.POSITIVE, NewsImpactLevel.HIGH)
            } else if (isHawkish && !isDovish) {
                return Pair(NewsSentiment.NEGATIVE, NewsImpactLevel.HIGH)
            }
        }

        // FII / DII Institutional Flows
        if (text.contains("fii") || text.contains("foreign institutional") || text.contains("fpi")) {
            val isBuying = text.contains("net buyer") || text.contains("inflow") || text.contains("pumps") ||
                    text.contains("invests") || text.contains("pours") || text.contains("buying") || text.contains("bought")
            val isSelling = text.contains("net seller") || text.contains("outflow") || text.contains("pulls out") ||
                    text.contains("dump") || text.contains("selling") || text.contains("offload") || text.contains("sold")

            if (isBuying && !isSelling) {
                return Pair(NewsSentiment.POSITIVE, NewsImpactLevel.HIGH)
            } else if (isSelling && !isBuying) {
                return Pair(NewsSentiment.NEGATIVE, NewsImpactLevel.HIGH)
            }
        }

        // Inflation (CPI/WPI)
        if (text.contains("inflation") || text.contains("cpi") || text.contains("retail inflation")) {
            val isCooling = text.contains("cools") || text.contains("eases") || text.contains("drops") ||
                    text.contains("slows") || text.contains("below expectation") || text.contains("moderates")
            val isRising = text.contains("spikes") || text.contains("accelerates") || text.contains("surges") ||
                    text.contains("jumps") || text.contains("above expectation") || text.contains("heats up")

            if (isCooling && !isRising) {
                return Pair(NewsSentiment.POSITIVE, NewsImpactLevel.HIGH)
            } else if (isRising && !isCooling) {
                return Pair(NewsSentiment.NEGATIVE, NewsImpactLevel.HIGH)
            }
        }

        // Rupee / Forex (USD/INR)
        if (text.contains("rupee") || text.contains("usd/inr") || text.contains("usdinr")) {
            val isStronger = text.contains("gains") || text.contains("appreciates") || text.contains("strengthens") ||
                    text.contains("rebounds") || text.contains("rallies")
            val isWeaker = text.contains("record low") || text.contains("slumps") || text.contains("depreciates") ||
                    text.contains("weakens") || text.contains("falls below")

            if (isStronger && !isWeaker) {
                return Pair(NewsSentiment.POSITIVE, NewsImpactLevel.MEDIUM)
            } else if (isWeaker && !isStronger) {
                return Pair(NewsSentiment.NEGATIVE, NewsImpactLevel.MEDIUM)
            }
        }

        // Global Markets (US / Wall Street / Asia)
        if (text.contains("wall st") || text.contains("wall street") || text.contains("nasdaq") ||
            text.contains("s&p") || text.contains("dow jones") || text.contains("asian markets") || text.contains("nikkei")
        ) {
            val isRally = text.contains("rally") || text.contains("surge") || text.contains("jumps") ||
                    text.contains("gains") || text.contains("advances") || text.contains("soars")
            val isPlunge = text.contains("plunges") || text.contains("slips") || text.contains("tumbles") ||
                    text.contains("drops") || text.contains("selloff") || text.contains("sinks")

            if (isRally && !isPlunge) {
                return Pair(NewsSentiment.POSITIVE, NewsImpactLevel.HIGH)
            } else if (isPlunge && !isRally) {
                return Pair(NewsSentiment.NEGATIVE, NewsImpactLevel.HIGH)
            }
        }

        // NIFTY & Major Index Context
        if (text.contains("nifty") || text.contains("sensex") || text.contains("dalal street") || text.contains("indian equities")) {
            val isBullish = text.contains("record high") || text.contains("all-time high") || text.contains("crosses") ||
                    text.contains("rallies") || text.contains("extends gains") || text.contains("bulls") || text.contains("rebounds")
            val isBearish = text.contains("drags") || text.contains("bloodbath") || text.contains("slips") ||
                    text.contains("plummets") || text.contains("snaps streak") || text.contains("bears take control")

            if (isBullish && !isBearish) {
                return Pair(NewsSentiment.POSITIVE, NewsImpactLevel.HIGH)
            } else if (isBearish && !isBullish) {
                return Pair(NewsSentiment.NEGATIVE, NewsImpactLevel.HIGH)
            }
        }

        // Key NIFTY 50 Heavyweights (Reliance, HDFC Bank, ICICI Bank, TCS, Infosys)
        val heavyweights = listOf("reliance", "hdfc bank", "icici bank", "tcs", "infosys", "bharti airtel", "itc", "l&t")
        if (heavyweights.any { text.contains(it) }) {
            val isGoodEarnings = text.contains("q2 profit jumps") || text.contains("q3 profit jumps") ||
                    text.contains("q4 profit jumps") || text.contains("beats estimates") || text.contains("strong profit") ||
                    text.contains("major deal win") || text.contains("bonus share")
            val isBadEarnings = text.contains("profit drops") || text.contains("misses estimates") ||
                    text.contains("penalty") || text.contains("probe") || text.contains("sebi notice") || text.contains("cut forecast")

            if (isGoodEarnings && !isBadEarnings) {
                return Pair(NewsSentiment.POSITIVE, NewsImpactLevel.MEDIUM)
            } else if (isBadEarnings && !isGoodEarnings) {
                return Pair(NewsSentiment.NEGATIVE, NewsImpactLevel.MEDIUM)
            }
        }

        // Default: If sentiment cannot be determined reliably, show NEUTRAL with LOW impact
        return Pair(NewsSentiment.NEUTRAL, NewsImpactLevel.LOW)
    }

    fun formatEpochToIst(epochMillis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a 'IST'", Locale.ENGLISH).apply {
            timeZone = istTimeZone
        }
        return sdf.format(Date(epochMillis))
    }
}
