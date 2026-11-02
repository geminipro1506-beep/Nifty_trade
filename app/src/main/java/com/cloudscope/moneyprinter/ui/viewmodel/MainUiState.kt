package com.cloudscope.moneyprinter.ui.viewmodel

import com.cloudscope.moneyprinter.data.model.*

data class MainUiState(
    val isDarkTheme: Boolean = true,
    val quote: MarketQuote? = null,
    val signal: TradingSignal? = null,
    val indicators: TechnicalIndicators? = null,
    val optionChain: OptionChain? = null,
    val supportResistance: SupportResistanceLevels? = null,
    val connectionState: ConnectionState = ConnectionState(ConnectionStatus.LIVE),
    val backtestResult: BacktestResult? = null,
    val isRefreshing: Boolean = false,
    val showConfigDialog: Boolean = false,
    val showBacktestDialog: Boolean = false,
    val testConnectionResult: String? = null,
    val isMarketOpen: Boolean = true,
    val autoRefreshEnabled: Boolean = true
)
