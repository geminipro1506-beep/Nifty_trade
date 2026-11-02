package com.cloudscope.moneyprinter.data.model

enum class BrokerType(val displayName: String) {
    NONE_STANDALONE("Public NSE Feed (Standalone)"),
    GROWW("Groww (Execution Broker)"),
    ANGEL_ONE("Angel One (SmartAPI)"),
    ZERODHA("Zerodha Kite Connect")
}

enum class ConnectionStatus {
    LIVE,
    DELAYED,
    DATA_UNAVAILABLE
}

data class ConnectionState(
    val status: ConnectionStatus,
    val lastUpdated: Long = System.currentTimeMillis(),
    val message: String = "",
    val brokerType: BrokerType = BrokerType.NONE_STANDALONE,
    val brokerName: String = brokerType.displayName
)

data class BrokerCredentials(
    val brokerType: BrokerType = BrokerType.NONE_STANDALONE,
    val apiKey: String = "",
    val accessToken: String = "",
    val clientId: String = "",
    val isConfigured: Boolean = false
)
