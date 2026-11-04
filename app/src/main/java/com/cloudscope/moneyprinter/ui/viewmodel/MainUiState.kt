package com.cloudscope.moneyprinter.ui.viewmodel

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.alert.AlertSettings
import com.cloudscope.moneyprinter.domain.alert.MarketAlert

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
    val strategyCohortMetrics: List<StrategyCohortMetric> = emptyList(),
    val reconciliationReport: ReconciliationReport? = null,
    val paperAccount: PaperTradingAccount = PaperTradingAccount(),
    val selectedAccuracyPeriod: AccuracyPeriod = AccuracyPeriod.LAST_30_DAYS,
    val selectedStrategyFilter: StrategyFilter = StrategyFilter.ALL,
    val selectedOptionFilter: OptionFilter = OptionFilter.ALL,
    val paperTradingOnlyFilter: Boolean = false,
    val notificationMessage: String? = null,

    // Signal Audit Details Modal
    val selectedDetailSignal: HistoricalSignalRecord? = null,
    val isSignalDetailLoading: Boolean = false,
    val signalDetailError: String? = null,

    // Real-Time Notification & Alert System
    val alerts: List<MarketAlert> = emptyList(),
    val unreadAlertCount: Int = 0,
    val activeInAppAlert: MarketAlert? = null,
    val alertSettings: AlertSettings = AlertSettings(),
    val showAlertsPanel: Boolean = false,
    val isBackgroundMonitoringActive: Boolean = false
)
