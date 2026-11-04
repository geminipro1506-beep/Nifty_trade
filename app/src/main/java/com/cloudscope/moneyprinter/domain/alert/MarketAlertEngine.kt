package com.cloudscope.moneyprinter.domain.alert

import com.cloudscope.moneyprinter.data.model.*
import java.util.Locale
import kotlin.math.abs

class MarketAlertEngine {

    // Persistent tracking of last observed market state
    var lastDirection: MarketDirection? = null
        private set
    var lastConfidence: Int? = null
        private set
    var lastPrice: Double? = null
        private set
    var lastVwap: Double? = null
        private set
    var lastRsi: Double? = null
        private set
    var lastMacdHist: Double? = null
        private set
    var lastEma9: Double? = null
        private set
    var lastEma20: Double? = null
        private set
    var lastVix: Double? = null
        private set

    // Cooldown tracking per alert type (timestamp in ms)
    private val lastAlertTimeByType = mutableMapOf<MarketAlertType, Long>()

    fun resetState() {
        lastDirection = null
        lastConfidence = null
        lastPrice = null
        lastVwap = null
        lastRsi = null
        lastMacdHist = null
        lastEma9 = null
        lastEma20 = null
        lastVix = null
        lastAlertTimeByType.clear()
    }

    /**
     * Evaluates live market conditions against previous state and returns newly triggered alerts.
     */
    fun evaluate(
        quote: MarketQuote,
        indicators: TechnicalIndicators,
        signal: TradingSignal?,
        candles: List<HistoricalCandle> = emptyList(),
        settings: AlertSettings = AlertSettings(),
        now: Long = System.currentTimeMillis()
    ): List<MarketAlert> {
        if (!settings.notificationsEnabled) {
            // Notifications master toggle is off
            updateState(quote, indicators, signal)
            return emptyList()
        }

        // Connection/staleness safety: ignore quotes older than 5 minutes
        if (quote.timestamp > 0 && (now - quote.timestamp) > 300000L) {
            return emptyList()
        }

        val triggeredAlerts = mutableListOf<MarketAlert>()

        // 1. Resolve Current Direction & Dynamic Confidence
        val currentDirection = indicators.marketDirection
        val currentConfidence = signal?.confidencePercent ?: when (currentDirection) {
            MarketDirection.BULLISH, MarketDirection.BEARISH -> 70
            MarketDirection.SIDEWAYS -> 45
        }

        // ----------------------------------------------------
        // 2. DIRECTION TRANSITIONS
        // ----------------------------------------------------
        val prevDir = lastDirection
        if (prevDir != null && currentDirection != prevDir) {
            when (currentDirection) {
                MarketDirection.BULLISH -> {
                    if (settings.bullishAlertsEnabled && !isCooldownActive(MarketAlertType.DIRECTION_BULLISH, now, settings.cooldownMinutes)) {
                        triggeredAlerts.add(
                            MarketAlert(
                                type = MarketAlertType.DIRECTION_BULLISH,
                                title = "🟢 NIFTY 50 changed to BULLISH",
                                message = "Market direction shifted from ${prevDir.name} to BULLISH at ₹${formatPrice(quote.lastPrice)}. Strong buying momentum dominant.",
                                timestamp = now,
                                previousDirection = prevDir,
                                newDirection = currentDirection,
                                previousConfidence = lastConfidence,
                                newConfidence = currentConfidence,
                                spotPrice = quote.lastPrice
                            )
                        )
                        lastAlertTimeByType[MarketAlertType.DIRECTION_BULLISH] = now
                    }
                }
                MarketDirection.BEARISH -> {
                    if (settings.bearishAlertsEnabled && !isCooldownActive(MarketAlertType.DIRECTION_BEARISH, now, settings.cooldownMinutes)) {
                        triggeredAlerts.add(
                            MarketAlert(
                                type = MarketAlertType.DIRECTION_BEARISH,
                                title = "🔴 NIFTY 50 changed to BEARISH",
                                message = "Market direction shifted from ${prevDir.name} to BEARISH at ₹${formatPrice(quote.lastPrice)}. Selling pressure dominant.",
                                timestamp = now,
                                previousDirection = prevDir,
                                newDirection = currentDirection,
                                previousConfidence = lastConfidence,
                                newConfidence = currentConfidence,
                                spotPrice = quote.lastPrice
                            )
                        )
                        lastAlertTimeByType[MarketAlertType.DIRECTION_BEARISH] = now
                    }
                }
                MarketDirection.SIDEWAYS -> {
                    if (settings.sidewaysAlertsEnabled && !isCooldownActive(MarketAlertType.DIRECTION_SIDEWAYS, now, settings.cooldownMinutes)) {
                        triggeredAlerts.add(
                            MarketAlert(
                                type = MarketAlertType.DIRECTION_SIDEWAYS,
                                title = "🟡 NIFTY 50 entered SIDEWAYS market",
                                message = "NIFTY entered consolidation between VWAP (₹${formatPrice(indicators.vwap)}) and key moving averages.",
                                timestamp = now,
                                previousDirection = prevDir,
                                newDirection = currentDirection,
                                previousConfidence = lastConfidence,
                                newConfidence = currentConfidence,
                                spotPrice = quote.lastPrice
                            )
                        )
                        lastAlertTimeByType[MarketAlertType.DIRECTION_SIDEWAYS] = now
                    }
                }
            }
        }

        // ----------------------------------------------------
        // 3. AI DIRECTIONAL CONFIDENCE SHIFTS
        // ----------------------------------------------------
        val prevConf = lastConfidence
        if (prevConf != null && settings.confidenceAlertsEnabled) {
            val delta = currentConfidence - prevConf
            if (delta >= settings.minConfidenceChangeThreshold && !isCooldownActive(MarketAlertType.CONFIDENCE_INCREASE, now, settings.cooldownMinutes)) {
                triggeredAlerts.add(
                    MarketAlert(
                        type = MarketAlertType.CONFIDENCE_INCREASE,
                        title = "📈 AI Confidence increased from $prevConf% to $currentConfidence%",
                        message = "Directional conviction strengthened by +$delta% on confirmed momentum alignment at ₹${formatPrice(quote.lastPrice)}.",
                        timestamp = now,
                        previousDirection = prevDir,
                        newDirection = currentDirection,
                        previousConfidence = prevConf,
                        newConfidence = currentConfidence,
                        spotPrice = quote.lastPrice
                    )
                )
                lastAlertTimeByType[MarketAlertType.CONFIDENCE_INCREASE] = now
            } else if (delta <= -settings.minConfidenceChangeThreshold && !isCooldownActive(MarketAlertType.CONFIDENCE_DECREASE, now, settings.cooldownMinutes)) {
                triggeredAlerts.add(
                    MarketAlert(
                        type = MarketAlertType.CONFIDENCE_DECREASE,
                        title = "📉 AI Confidence dropped from $prevConf% to $currentConfidence%",
                        message = "Directional conviction deteriorated by $delta% as key indicator confluence broke at ₹${formatPrice(quote.lastPrice)}.",
                        timestamp = now,
                        previousDirection = prevDir,
                        newDirection = currentDirection,
                        previousConfidence = prevConf,
                        newConfidence = currentConfidence,
                        spotPrice = quote.lastPrice
                    )
                )
                lastAlertTimeByType[MarketAlertType.CONFIDENCE_DECREASE] = now
            }
        }

        // ----------------------------------------------------
        // 4. TECHNICAL INDICATOR ALERTS
        // ----------------------------------------------------
        if (settings.technicalAlertsEnabled) {
            val pPrice = lastPrice
            val pVwap = lastVwap

            // 4a. VWAP Breakout / Breakdown
            if (pPrice != null && pVwap != null && indicators.vwap > 0.0) {
                if (pPrice <= pVwap && quote.lastPrice > indicators.vwap && !isCooldownActive(MarketAlertType.VWAP_BREAKOUT, now, settings.cooldownMinutes)) {
                    triggeredAlerts.add(
                        MarketAlert(
                            type = MarketAlertType.VWAP_BREAKOUT,
                            title = "⚡ NIFTY crossed above VWAP",
                            message = "NIFTY spot (₹${formatPrice(quote.lastPrice)}) broke above intraday VWAP (₹${formatPrice(indicators.vwap)}). Bullish continuation setup active.",
                            timestamp = now,
                            previousDirection = prevDir,
                            newDirection = currentDirection,
                            spotPrice = quote.lastPrice
                        )
                    )
                    lastAlertTimeByType[MarketAlertType.VWAP_BREAKOUT] = now
                } else if (pPrice >= pVwap && quote.lastPrice < indicators.vwap && !isCooldownActive(MarketAlertType.VWAP_BREAKDOWN, now, settings.cooldownMinutes)) {
                    triggeredAlerts.add(
                        MarketAlert(
                            type = MarketAlertType.VWAP_BREAKDOWN,
                            title = "⚠️ Strong selling pressure detected",
                            message = "NIFTY spot (₹${formatPrice(quote.lastPrice)}) dropped below intraday VWAP (₹${formatPrice(indicators.vwap)}). Bearish breakdown active.",
                            timestamp = now,
                            previousDirection = prevDir,
                            newDirection = currentDirection,
                            spotPrice = quote.lastPrice
                        )
                    )
                    lastAlertTimeByType[MarketAlertType.VWAP_BREAKDOWN] = now
                }
            }

            // 4b. RSI Overbought / Oversold
            val pRsi = lastRsi
            if (pRsi != null) {
                if (pRsi < 70.0 && indicators.rsi14 >= 70.0 && !isCooldownActive(MarketAlertType.RSI_OVERBOUGHT, now, settings.cooldownMinutes)) {
                    triggeredAlerts.add(
                        MarketAlert(
                            type = MarketAlertType.RSI_OVERBOUGHT,
                            title = "🔥 RSI Overbought (RSI ${indicators.rsi14.toInt()})",
                            message = "NIFTY RSI reached ${String.format(Locale.US, "%.1f", indicators.rsi14)}. Momentum extended; caution on chasing highs.",
                            timestamp = now,
                            spotPrice = quote.lastPrice
                        )
                    )
                    lastAlertTimeByType[MarketAlertType.RSI_OVERBOUGHT] = now
                } else if (pRsi > 30.0 && indicators.rsi14 <= 30.0 && !isCooldownActive(MarketAlertType.RSI_OVERSOLD, now, settings.cooldownMinutes)) {
                    triggeredAlerts.add(
                        MarketAlert(
                            type = MarketAlertType.RSI_OVERSOLD,
                            title = "❄️ RSI Oversold (RSI ${indicators.rsi14.toInt()})",
                            message = "NIFTY RSI dropped to ${String.format(Locale.US, "%.1f", indicators.rsi14)}. Heavily oversold; potential mean reversion bounce.",
                            timestamp = now,
                            spotPrice = quote.lastPrice
                        )
                    )
                    lastAlertTimeByType[MarketAlertType.RSI_OVERSOLD] = now
                }
            }

            // 4c. MACD Crossover
            val pMacdHist = lastMacdHist
            if (pMacdHist != null) {
                if (pMacdHist <= 0.0 && indicators.macdHistogram > 0.0 && !isCooldownActive(MarketAlertType.MACD_BULLISH_CROSS, now, settings.cooldownMinutes)) {
                    triggeredAlerts.add(
                        MarketAlert(
                            type = MarketAlertType.MACD_BULLISH_CROSS,
                            title = "📊 MACD Bullish Crossover",
                            message = "MACD histogram turned positive at ₹${formatPrice(quote.lastPrice)}. Upward momentum expanding.",
                            timestamp = now,
                            spotPrice = quote.lastPrice
                        )
                    )
                    lastAlertTimeByType[MarketAlertType.MACD_BULLISH_CROSS] = now
                } else if (pMacdHist >= 0.0 && indicators.macdHistogram < 0.0 && !isCooldownActive(MarketAlertType.MACD_BEARISH_CROSS, now, settings.cooldownMinutes)) {
                    triggeredAlerts.add(
                        MarketAlert(
                            type = MarketAlertType.MACD_BEARISH_CROSS,
                            title = "📊 MACD Bearish Crossover",
                            message = "MACD histogram turned negative at ₹${formatPrice(quote.lastPrice)}. Downward momentum accelerating.",
                            timestamp = now,
                            spotPrice = quote.lastPrice
                        )
                    )
                    lastAlertTimeByType[MarketAlertType.MACD_BEARISH_CROSS] = now
                }
            }

            // 4d. EMA 9 / 20 (21) Crossover
            val pEma9 = lastEma9
            val pEma20 = lastEma20
            if (pEma9 != null && pEma20 != null && indicators.ema9 > 0.0 && indicators.ema20 > 0.0) {
                if (pEma9 <= pEma20 && indicators.ema9 > indicators.ema20 && !isCooldownActive(MarketAlertType.EMA_BULLISH_CROSS, now, settings.cooldownMinutes)) {
                    triggeredAlerts.add(
                        MarketAlert(
                            type = MarketAlertType.EMA_BULLISH_CROSS,
                            title = "📈 EMA 9 crossed above EMA 21",
                            message = "Short-term 9 EMA (₹${formatPrice(indicators.ema9)}) crossed above 20/21 EMA (₹${formatPrice(indicators.ema20)}). Bullish continuation signal.",
                            timestamp = now,
                            spotPrice = quote.lastPrice
                        )
                    )
                    lastAlertTimeByType[MarketAlertType.EMA_BULLISH_CROSS] = now
                } else if (pEma9 >= pEma20 && indicators.ema9 < indicators.ema20 && !isCooldownActive(MarketAlertType.EMA_BEARISH_CROSS, now, settings.cooldownMinutes)) {
                    triggeredAlerts.add(
                        MarketAlert(
                            type = MarketAlertType.EMA_BEARISH_CROSS,
                            title = "📉 EMA 9 crossed below EMA 21",
                            message = "Short-term 9 EMA (₹${formatPrice(indicators.ema9)}) crossed below 20/21 EMA (₹${formatPrice(indicators.ema20)}). Bearish breakdown signal.",
                            timestamp = now,
                            spotPrice = quote.lastPrice
                        )
                    )
                    lastAlertTimeByType[MarketAlertType.EMA_BEARISH_CROSS] = now
                }
            }

            // 4e. Volume Surge (5m candle volume > 1.8x average)
            if (candles.size >= 5) {
                val recentCandles = candles.takeLast(10)
                val avgVol = recentCandles.dropLast(1).map { it.volume.toDouble() }.average()
                val currentVol = recentCandles.last().volume.toDouble()
                if (avgVol > 1000.0 && currentVol > 1.8 * avgVol && !isCooldownActive(MarketAlertType.VOLUME_SURGE, now, settings.cooldownMinutes)) {
                    triggeredAlerts.add(
                        MarketAlert(
                            type = MarketAlertType.VOLUME_SURGE,
                            title = "🚀 Unusual Volume Spike Detected",
                            message = "NIFTY 5-minute volume expanded to ${(currentVol / 1000).toInt()}k (${String.format(Locale.US, "%.1f", currentVol / avgVol)}x average). Institutional activity present.",
                            timestamp = now,
                            spotPrice = quote.lastPrice
                        )
                    )
                    lastAlertTimeByType[MarketAlertType.VOLUME_SURGE] = now
                }
            }

            // 4f. Significant India VIX Movement
            val pVix = lastVix
            if (pVix != null && indicators.vix > 0.0) {
                val vixDelta = indicators.vix - pVix
                if ((abs(vixDelta) >= 0.7 || (pVix < 20.0 && indicators.vix >= 20.0)) && !isCooldownActive(MarketAlertType.VIX_SPIKE, now, settings.cooldownMinutes)) {
                    val sign = if (vixDelta > 0) "+" else ""
                    triggeredAlerts.add(
                        MarketAlert(
                            type = MarketAlertType.VIX_SPIKE,
                            title = "⚡ Significant India VIX Movement",
                            message = "India VIX shifted by $sign${String.format(Locale.US, "%.2f", vixDelta)} to ${String.format(Locale.US, "%.2f", indicators.vix)}. Volatility regime changing.",
                            timestamp = now,
                            spotPrice = quote.lastPrice
                        )
                    )
                    lastAlertTimeByType[MarketAlertType.VIX_SPIKE] = now
                }
            }
        }

        // Always update baseline state for next evaluation
        updateState(quote, indicators, signal)

        return triggeredAlerts
    }

    private fun updateState(
        quote: MarketQuote,
        indicators: TechnicalIndicators,
        signal: TradingSignal?
    ) {
        lastDirection = indicators.marketDirection
        lastConfidence = signal?.confidencePercent ?: when (indicators.marketDirection) {
            MarketDirection.BULLISH, MarketDirection.BEARISH -> 70
            MarketDirection.SIDEWAYS -> 45
        }
        lastPrice = quote.lastPrice
        lastVwap = indicators.vwap
        lastRsi = indicators.rsi14
        lastMacdHist = indicators.macdHistogram
        lastEma9 = indicators.ema9
        lastEma20 = indicators.ema20
        lastVix = indicators.vix
    }

    private fun isCooldownActive(type: MarketAlertType, now: Long, cooldownMinutes: Int): Boolean {
        val lastTime = lastAlertTimeByType[type] ?: return false
        val cooldownMillis = cooldownMinutes * 60 * 1000L
        return (now - lastTime) < cooldownMillis
    }

    private fun formatPrice(price: Double): String {
        return String.format(Locale.US, "%.2f", price)
    }
}
