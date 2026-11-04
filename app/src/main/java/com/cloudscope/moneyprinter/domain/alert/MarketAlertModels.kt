package com.cloudscope.moneyprinter.domain.alert

import com.cloudscope.moneyprinter.data.model.MarketDirection
import java.util.UUID

enum class AlertCategory(val displayName: String) {
    DIRECTION("Market Direction"),
    CONFIDENCE("AI Confidence"),
    TECHNICAL("Technical Indicators"),
    VOLATILITY("Volatility & VIX")
}

enum class MarketAlertType(val category: AlertCategory, val defaultEmoji: String, val label: String) {
    DIRECTION_BULLISH(AlertCategory.DIRECTION, "🟢", "Bullish Turn"),
    DIRECTION_BEARISH(AlertCategory.DIRECTION, "🔴", "Bearish Turn"),
    DIRECTION_SIDEWAYS(AlertCategory.DIRECTION, "🟡", "Sideways Consolidation"),
    CONFIDENCE_INCREASE(AlertCategory.CONFIDENCE, "📈", "Confidence Surge"),
    CONFIDENCE_DECREASE(AlertCategory.CONFIDENCE, "📉", "Confidence Drop"),
    VWAP_BREAKOUT(AlertCategory.TECHNICAL, "⚡", "VWAP Breakout"),
    VWAP_BREAKDOWN(AlertCategory.TECHNICAL, "⚠️", "VWAP Breakdown"),
    RSI_OVERBOUGHT(AlertCategory.TECHNICAL, "🔥", "RSI Overbought"),
    RSI_OVERSOLD(AlertCategory.TECHNICAL, "❄️", "RSI Oversold"),
    MACD_BULLISH_CROSS(AlertCategory.TECHNICAL, "📊", "MACD Bullish Cross"),
    MACD_BEARISH_CROSS(AlertCategory.TECHNICAL, "📊", "MACD Bearish Cross"),
    EMA_BULLISH_CROSS(AlertCategory.TECHNICAL, "📈", "EMA 9/21 Bullish Cross"),
    EMA_BEARISH_CROSS(AlertCategory.TECHNICAL, "📉", "EMA 9/21 Bearish Cross"),
    VOLUME_SURGE(AlertCategory.TECHNICAL, "🚀", "Volume Spike"),
    VIX_SPIKE(AlertCategory.VOLATILITY, "⚡", "VIX Surge")
}

data class MarketAlert(
    val id: String = UUID.randomUUID().toString(),
    val type: MarketAlertType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val previousDirection: MarketDirection? = null,
    val newDirection: MarketDirection? = null,
    val previousConfidence: Int? = null,
    val newConfidence: Int? = null,
    val spotPrice: Double,
    val isRead: Boolean = false
)

data class AlertSettings(
    val notificationsEnabled: Boolean = true,
    val bullishAlertsEnabled: Boolean = true,
    val bearishAlertsEnabled: Boolean = true,
    val sidewaysAlertsEnabled: Boolean = true,
    val confidenceAlertsEnabled: Boolean = true,
    val technicalAlertsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val minConfidenceChangeThreshold: Int = 10, // Default 10 percentage points
    val cooldownMinutes: Int = 3 // Minimum time interval between repeated alerts of same type
)
