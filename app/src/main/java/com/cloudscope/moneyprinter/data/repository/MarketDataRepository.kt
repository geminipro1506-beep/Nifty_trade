package com.cloudscope.moneyprinter.data.repository

import com.cloudscope.moneyprinter.data.model.*
import kotlinx.coroutines.flow.StateFlow

interface MarketDataRepository {
    val connectionState: StateFlow<ConnectionState>
    val latestQuote: StateFlow<MarketQuote?>
    val latestOptionChain: StateFlow<OptionChain?>
    val historicalCandles: StateFlow<List<HistoricalCandle>>
    val latestVix: StateFlow<Double>

    suspend fun refreshMarketData(): Result<Unit>
    suspend fun testConnection(apiKey: String, accessToken: String): Result<Boolean>
    fun saveCredentials(apiKey: String, accessToken: String)
    fun getCredentials(): BrokerCredentials
    fun clearCredentials()
}
