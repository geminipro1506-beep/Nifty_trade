package com.cloudscope.moneyprinter.data.local

import android.content.Context
import android.content.SharedPreferences
import com.cloudscope.moneyprinter.domain.alert.AlertSettings
import com.cloudscope.moneyprinter.domain.alert.MarketAlert
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlertPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAlertSettings(): AlertSettings {
        return AlertSettings(
            notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true),
            bullishAlertsEnabled = prefs.getBoolean(KEY_BULLISH_ENABLED, true),
            bearishAlertsEnabled = prefs.getBoolean(KEY_BEARISH_ENABLED, true),
            sidewaysAlertsEnabled = prefs.getBoolean(KEY_SIDEWAYS_ENABLED, true),
            confidenceAlertsEnabled = prefs.getBoolean(KEY_CONFIDENCE_ENABLED, true),
            technicalAlertsEnabled = prefs.getBoolean(KEY_TECHNICAL_ENABLED, true),
            soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true),
            minConfidenceChangeThreshold = prefs.getInt(KEY_CONFIDENCE_THRESHOLD, 10),
            cooldownMinutes = prefs.getInt(KEY_COOLDOWN_MINUTES, 3)
        )
    }

    fun saveAlertSettings(settings: AlertSettings) {
        prefs.edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, settings.notificationsEnabled)
            .putBoolean(KEY_BULLISH_ENABLED, settings.bullishAlertsEnabled)
            .putBoolean(KEY_BEARISH_ENABLED, settings.bearishAlertsEnabled)
            .putBoolean(KEY_SIDEWAYS_ENABLED, settings.sidewaysAlertsEnabled)
            .putBoolean(KEY_CONFIDENCE_ENABLED, settings.confidenceAlertsEnabled)
            .putBoolean(KEY_TECHNICAL_ENABLED, settings.technicalAlertsEnabled)
            .putBoolean(KEY_SOUND_ENABLED, settings.soundEnabled)
            .putInt(KEY_CONFIDENCE_THRESHOLD, settings.minConfidenceChangeThreshold)
            .putInt(KEY_COOLDOWN_MINUTES, settings.cooldownMinutes)
            .apply()
    }

    fun getAlertHistory(): List<MarketAlert> {
        val json = prefs.getString(KEY_ALERT_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MarketAlert>>() {}.type
            gson.fromJson<List<MarketAlert>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAlertHistory(alerts: List<MarketAlert>) {
        val trimmed = alerts.take(MAX_HISTORY_ITEMS)
        val json = gson.toJson(trimmed)
        prefs.edit().putString(KEY_ALERT_HISTORY, json).apply()
    }

    fun isBackgroundMonitoringEnabled(): Boolean {
        return prefs.getBoolean(KEY_BG_MONITORING_ENABLED, false)
    }

    fun setBackgroundMonitoringEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BG_MONITORING_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "nifty_market_alert_prefs"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_BULLISH_ENABLED = "bullish_enabled"
        private const val KEY_BEARISH_ENABLED = "bearish_enabled"
        private const val KEY_SIDEWAYS_ENABLED = "sideways_enabled"
        private const val KEY_CONFIDENCE_ENABLED = "confidence_enabled"
        private const val KEY_TECHNICAL_ENABLED = "technical_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_CONFIDENCE_THRESHOLD = "confidence_threshold"
        private const val KEY_COOLDOWN_MINUTES = "cooldown_minutes"
        private const val KEY_ALERT_HISTORY = "alert_history"
        private const val KEY_BG_MONITORING_ENABLED = "bg_monitoring_enabled"
        private const val MAX_HISTORY_ITEMS = 100
    }
}
