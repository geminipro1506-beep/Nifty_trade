package com.cloudscope.moneyprinter.ui.viewmodel

import com.cloudscope.moneyprinter.data.model.*

enum class TerminalNavTab(val title: String) {
    DASHBOARD("Dashboard"),
    SIGNALS("Live Signals"),
    OPTION_CHAIN("Option Chain"),
    PERFORMANCE("Performance"),
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
    val selectedExpiryDate: String = "Upcoming Weekly",
    val tradeConfirmationSignal: TradingSignal? = null,
    val targetExecutionBroker: BrokerType? = null,

    // Accuracy & Historical Tracking
    val accuracyMetrics: AccuracyMetrics? = null,
    val historicalSignals: List<HistoricalSignalRecord> = emptyList(),
    val strategyInsights: List<StrategyImprovementInsight> = emptyList(),
    val strategyComparisons: List<StrategyPerformanceComparison> = emptyList(),
    val paperAccount: PaperTradingAccount = PaperTradingAccount(),
    val selectedAccuracyPeriod: AccuracyPeriod = AccuracyPeriod.LAST_30_DAYS,
    val selectedStrategyFilter: StrategyFilter = StrategyFilter.ALL,
    val selectedOptionFilter: OptionFilter = OptionFilter.ALL,
    val paperTradingOnlyFilter: Boolean = false,
    val notificationMessage: String? = null
)
