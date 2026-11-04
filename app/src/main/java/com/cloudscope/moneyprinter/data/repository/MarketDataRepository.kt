package com.cloudscope.moneyprinter.data.repository

import com.cloudscope.moneyprinter.data.model.*
import kotlinx.coroutines.flow.StateFlow

interface MarketDataRepository {
    val connectionState: StateFlow<ConnectionState>
    val latestQuote: StateFlow<MarketQuote?>
    val latestOptionChain: StateFlow<OptionChain?>
    val latestFuturesQuote: StateFlow<NiftyFuturesQuote?>
    val historicalCandles: StateFlow<List<HistoricalCandle>>
    val latestVix: StateFlow<Double>

    fun saveCredentials(apiKey: String, accessToken: String, brokerType: BrokerType = BrokerType.ZERODHA, clientId: String = "")
    fun saveBrokerType(brokerType: BrokerType)
    fun getCredentials(): BrokerCredentials
    fun clearCredentials()
    suspend fun testConnection(apiKey: String, accessToken: String, brokerType: BrokerType = BrokerType.ZERODHA): Result<Boolean>
    suspend fun refreshMarketData(): Result<Unit>
}
