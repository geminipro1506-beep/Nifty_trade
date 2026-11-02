package com.cloudscope.moneyprinter.ui.viewmodel

import com.cloudscope.moneyprinter.data.model.*

enum class TerminalNavTab(val title: String) {
    DASHBOARD("Dashboard"),
    SIGNALS("Live Signals"),
    OPTION_CHAIN("Option Chain"),
    ANALYSIS("Analysis"),
    SETTINGS("Settings")
}

data class MainUiState(
    val isDarkTheme: Boolean = true,
    val selectedTab: TerminalNavTab = TerminalNavTab.DASHBOARD,
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
    val autoRefreshEnabled: Boolean = true,
    val optionChainSearchQuery: String = "",
    val selectedExpiryDate: String = "07 NOV 2024",
    val tradeConfirmationSignal: TradingSignal? = null,
    val targetExecutionBroker: BrokerType? = null
)
