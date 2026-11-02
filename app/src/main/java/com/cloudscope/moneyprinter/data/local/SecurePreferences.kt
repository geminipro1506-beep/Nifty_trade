package com.cloudscope.moneyprinter.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.cloudscope.moneyprinter.data.model.BrokerCredentials
import com.cloudscope.moneyprinter.data.model.BrokerType

open class SecurePreferences(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "cs_secure_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for environments where Android KeyStore hardware backing is limited
            context.getSharedPreferences("cs_trading_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_BROKER_TYPE = "broker_type"
        private const val KEY_API_KEY = "broker_api_key"
        private const val KEY_ACCESS_TOKEN = "broker_access_token"
        private const val KEY_CLIENT_ID = "broker_client_id"
        private const val KEY_DARK_THEME = "ui_dark_theme_enabled"
        private const val KEY_AUTO_REFRESH = "auto_refresh_enabled"
    }

    open fun saveCredentials(
        apiKey: String,
        accessToken: String,
        brokerType: BrokerType = BrokerType.ZERODHA,
        clientId: String = ""
    ) {
        prefs.edit()
            .putString(KEY_BROKER_TYPE, brokerType.name)
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_ACCESS_TOKEN, accessToken.trim())
            .putString(KEY_CLIENT_ID, clientId.trim())
            .apply()
    }

    open fun saveBrokerType(brokerType: BrokerType) {
        prefs.edit().putString(KEY_BROKER_TYPE, brokerType.name).apply()
    }

    open fun getCredentials(): BrokerCredentials {
        val brokerTypeName = prefs.getString(KEY_BROKER_TYPE, BrokerType.NONE_STANDALONE.name) ?: BrokerType.NONE_STANDALONE.name
        val brokerType = try {
            BrokerType.valueOf(brokerTypeName)
        } catch (e: Exception) {
            BrokerType.NONE_STANDALONE
        }
        val apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        val token = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        val clientId = prefs.getString(KEY_CLIENT_ID, "") ?: ""

        val isConfigured = when (brokerType) {
            BrokerType.NONE_STANDALONE -> false
            BrokerType.GROWW -> true // Groww is active as execution broker
            BrokerType.ANGEL_ONE -> apiKey.isNotBlank() || token.isNotBlank()
            BrokerType.ZERODHA -> apiKey.isNotBlank() && token.isNotBlank()
        }

        return BrokerCredentials(
            brokerType = brokerType,
            apiKey = apiKey,
            accessToken = token,
            clientId = clientId,
            isConfigured = isConfigured
        )
    }

    open fun clearCredentials() {
        prefs.edit()
            .remove(KEY_BROKER_TYPE)
            .remove(KEY_API_KEY)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_CLIENT_ID)
            .apply()
    }

    open fun isDarkThemeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_THEME, true) // Default to terminal dark theme
    }

    open fun setDarkThemeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
    }

    open fun isAutoRefreshEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_REFRESH, true)
    }

    open fun setAutoRefreshEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REFRESH, enabled).apply()
    }
}
