package com.cloudscope.moneyprinter.data.model

enum class ConnectionStatus {
    LIVE,
    DELAYED,
    DATA_UNAVAILABLE,
    NEEDS_SETUP
}

data class ConnectionState(
    val status: ConnectionStatus,
    val lastUpdated: Long = System.currentTimeMillis(),
    val message: String = "",
    val brokerName: String = "Zerodha Kite Connect"
)

data class BrokerCredentials(
    val apiKey: String = "",
    val accessToken: String = "",
    val isConfigured: Boolean = false
)
