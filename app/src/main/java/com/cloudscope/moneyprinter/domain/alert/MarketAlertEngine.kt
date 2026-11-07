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

    // Smart Money alert state tracking
    var lastSmartMoneyAction: com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalAction = com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalAction.NO_TRADE
        private set
    var lastSmartMoneyStrike: String = ""
        private set
    var lastSmartMoneyTime: Long = 0L
        private set

    // Normal Options alert state tracking
    var lastNormalAction: com.cloudscope.moneyprinter.domain.normaloptions.NormalSignalAction = com.cloudscope.moneyprinter.domain.normaloptions.NormalSignalAction.NO_TRADE
        private set
    var lastNormalStrike: Double = 0.0
        private set
    var lastNormalTime: Long = 0L
        private set

    // Signal conflict state tracking
    var lastConflictState: Boolean = false
        private set
    var lastConflictTime: Long = 0L
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
        lastSmartMoneyAction = com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalAction.NO_TRADE
        lastSmartMoneyStrike = ""
        lastSmartMoneyTime = 0L
        lastNormalAction = com.cloudscope.moneyprinter.domain.normaloptions.NormalSignalAction.NO_TRADE
        lastNormalStrike = 0.0
        lastNormalTime = 0L
        lastConflictState = false
        lastConflictTime = 0L
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

    /**
     * Evaluates smart money options signal changes and returns an in-app alert when a new setup forms
     * or the directional signal changes, while avoiding duplicate alerts.
     */
    fun evaluateSmartMoneySignal(
        smartMoneySignal: com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignal?,
        quote: MarketQuote,
        now: Long = System.currentTimeMillis()
    ): MarketAlert? {
        if (smartMoneySignal == null || smartMoneySignal.action == com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalAction.NO_TRADE) {
            if (lastSmartMoneyAction != com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalAction.NO_TRADE) {
                lastSmartMoneyAction = com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalAction.NO_TRADE
                lastSmartMoneyStrike = ""
            }
            return null
        }

        val action = smartMoneySignal.action
        val strike = smartMoneySignal.suggestedStrike
        val cooldownMillis = 10 * 60 * 1000L

        val isSignalChanged = action != lastSmartMoneyAction
        val isNewSetup = strike != lastSmartMoneyStrike
        val isCooldownExpired = (now - lastSmartMoneyTime) > cooldownMillis

        if (isSignalChanged || isNewSetup || isCooldownExpired) {
            lastSmartMoneyAction = action
            lastSmartMoneyStrike = strike
            lastSmartMoneyTime = now

            val isCe = action == com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalAction.BUY_CE
            val emoji = if (isCe) "🟢" else "🔴"
            val alertType = if (isCe) MarketAlertType.SMART_MONEY_BUY_CE else MarketAlertType.SMART_MONEY_BUY_PE

            return MarketAlert(
                type = alertType,
                title = "$emoji SMART MONEY: ${action.displayName} ($strike)",
                message = "${smartMoneySignal.shortReason} • Entry: ${smartMoneySignal.entryZone} • SL: ${smartMoneySignal.stopLoss} • T1: ${smartMoneySignal.target1} (${smartMoneySignal.confidence.displayName} Confidence)",
                timestamp = now,
                spotPrice = quote.lastPrice,
                newDirection = if (isCe) MarketDirection.BULLISH else MarketDirection.BEARISH,
                newConfidence = if (smartMoneySignal.confidence == com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneyConfidence.STRONG) 85 else 70
            )
        }
        return null
    }

    /**
     * Evaluates Normal Options Signal changes with duplicate suppression and cooldown.
     */
    fun evaluateNormalSignal(
        normalSignal: com.cloudscope.moneyprinter.domain.normaloptions.NormalOptionsSignal?,
        quote: MarketQuote,
        now: Long = System.currentTimeMillis()
    ): MarketAlert? {
        if (normalSignal == null || normalSignal.action == com.cloudscope.moneyprinter.domain.normaloptions.NormalSignalAction.NO_TRADE ||
            normalSignal.action == com.cloudscope.moneyprinter.domain.normaloptions.NormalSignalAction.WAITING_FOR_DATA
        ) {
            if (lastNormalAction != com.cloudscope.moneyprinter.domain.normaloptions.NormalSignalAction.NO_TRADE) {
                lastNormalAction = com.cloudscope.moneyprinter.domain.normaloptions.NormalSignalAction.NO_TRADE
                lastNormalStrike = 0.0
            }
            return null
        }

        val action = normalSignal.action
        val strike = normalSignal.suggestedStrike ?: 0.0
        val cooldownMillis = 5 * 60 * 1000L

        val isSignalChanged = action != lastNormalAction
        val isNewStrike = strike > 0.0 && strike != lastNormalStrike
        val isCooldownExpired = (now - lastNormalTime) > cooldownMillis

        if (isSignalChanged || isNewStrike || isCooldownExpired) {
            lastNormalAction = action
            lastNormalStrike = strike
            lastNormalTime = now

            val isCall = action == com.cloudscope.moneyprinter.domain.normaloptions.NormalSignalAction.BUY_CALL
            val emoji = if (isCall) "🟢" else "🔴"
            val alertType = if (isCall) MarketAlertType.NORMAL_BUY_CE else MarketAlertType.NORMAL_BUY_PE
            val strikeLabel = if (strike > 0.0) "${strike.toInt()} ${if (isCall) "CE" else "PE"}" else "ATM"

            return MarketAlert(
                type = alertType,
                title = "$emoji NORMAL OPTIONS: ${action.displayName} ($strikeLabel)",
                message = "${normalSignal.signalReason} • Entry: ₹${formatPrice(normalSignal.entryPrice)} • SL: ₹${formatPrice(normalSignal.stopLoss)} • T1: ₹${formatPrice(normalSignal.target1)} (${normalSignal.confidencePercent}% Confidence)",
                timestamp = now,
                spotPrice = quote.lastPrice,
                newDirection = if (isCall) MarketDirection.BULLISH else MarketDirection.BEARISH,
                newConfidence = normalSignal.confidencePercent
            )
        }
        return null
    }

    /**
     * Evaluates Signal Conflict between Normal and Smart Money engines.
     */
    fun evaluateSignalConflict(
        conflictState: com.cloudscope.moneyprinter.domain.signal.SignalConflictState,
        quote: MarketQuote,
        now: Long = System.currentTimeMillis()
    ): MarketAlert? {
        if (!conflictState.hasConflict) {
            lastConflictState = false
            return null
        }

        val cooldownMillis = 5 * 60 * 1000L
        if (!lastConflictState || (now - lastConflictTime) > cooldownMillis) {
            lastConflictState = true
            lastConflictTime = now

            return MarketAlert(
                type = MarketAlertType.SIGNAL_CONFLICT,
                title = "⚠️ SIGNAL CONFLICT DETECTED",
                message = conflictState.conflictReason,
                timestamp = now,
                spotPrice = quote.lastPrice
            )
        }
        return null
    }

    /**
     * Evaluates high-impact news items and generates an alert for market-moving events.
     */
    fun evaluateHighImpactNews(
        article: com.cloudscope.moneyprinter.domain.news.MarketNewsArticle,
        quote: MarketQuote,
        now: Long = System.currentTimeMillis()
    ): MarketAlert? {
        if (article.impact != com.cloudscope.moneyprinter.domain.news.NewsImpactLevel.HIGH) return null

        val isBullish = article.sentiment == com.cloudscope.moneyprinter.domain.news.NewsSentiment.POSITIVE
        val isBearish = article.sentiment == com.cloudscope.moneyprinter.domain.news.NewsSentiment.NEGATIVE
        if (!isBullish && !isBearish) return null

        val alertType = if (isBullish) MarketAlertType.BULLISH_NEWS else MarketAlertType.BEARISH_NEWS
        val cooldownMillis = 15 * 60 * 1000L
        val lastTime = lastAlertTimeByType[alertType] ?: 0L

        if ((now - lastTime) > cooldownMillis) {
            lastAlertTimeByType[alertType] = now
            val emoji = if (isBullish) "🟢" else "🔴"
            return MarketAlert(
                type = alertType,
                title = "$emoji ${alertType.label}: ${article.source}",
                message = article.displayHeadline,
                timestamp = now,
                spotPrice = quote.lastPrice
            )
        }
        return null
    }

    /**
     * Evaluates market data disconnection.
     */
    fun evaluateDisconnection(
        isDisconnected: Boolean,
        spotPrice: Double,
        now: Long = System.currentTimeMillis()
    ): MarketAlert? {
        if (!isDisconnected) return null

        val alertType = MarketAlertType.MARKET_DATA_DISCONNECTED
        val cooldownMillis = 5 * 60 * 1000L
        val lastTime = lastAlertTimeByType[alertType] ?: 0L

        if ((now - lastTime) > cooldownMillis) {
            lastAlertTimeByType[alertType] = now
            return MarketAlert(
                type = alertType,
                title = "🔴 Market Data Disconnected",
                message = "Live exchange stream interrupted. Checking network and reconnecting automatically.",
                timestamp = now,
                spotPrice = spotPrice
            )
        }
        return null
    }
}
