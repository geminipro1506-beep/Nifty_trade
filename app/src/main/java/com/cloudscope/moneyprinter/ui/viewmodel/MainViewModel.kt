package com.cloudscope.moneyprinter.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudscope.moneyprinter.data.local.AlertPreferences
import com.cloudscope.moneyprinter.data.local.SecurePreferences
import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.alert.AlertSettings
import com.cloudscope.moneyprinter.domain.alert.AndroidNotificationDispatcher
import com.cloudscope.moneyprinter.domain.alert.MarketAlert
import com.cloudscope.moneyprinter.domain.alert.MarketAlertEngine
import com.cloudscope.moneyprinter.domain.alert.MarketAlertType
import com.cloudscope.moneyprinter.domain.backtest.BacktestEngine
import com.cloudscope.moneyprinter.domain.indicator.IndicatorEngine
import com.cloudscope.moneyprinter.domain.paper.PaperTradingManager
import com.cloudscope.moneyprinter.domain.risk.RiskManager
import com.cloudscope.moneyprinter.domain.signal.ChargesCalculator
import com.cloudscope.moneyprinter.domain.signal.SignalEngine
import com.cloudscope.moneyprinter.domain.signal.SignalOutcomeTracker
import com.cloudscope.moneyprinter.domain.strategy.StrategyImprovementEngine
import com.cloudscope.moneyprinter.data.repository.MarketDataRepository
import com.cloudscope.moneyprinter.data.repository.SignalRepository
import com.cloudscope.moneyprinter.domain.normaloptions.*
import com.cloudscope.moneyprinter.domain.smartmoney.*
import com.cloudscope.moneyprinter.service.MarketMonitoringService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainViewModel(
    private val repository: MarketDataRepository? = null,
    private val securePreferences: SecurePreferences? = null,
    private val signalRepository: SignalRepository? = null,
    private val alertPreferences: AlertPreferences? = null,
    private val notificationDispatcher: AndroidNotificationDispatcher? = null
) : ViewModel() {

    private val initialAlertSettings = alertPreferences?.getAlertSettings() ?: AlertSettings()
    private val initialAlerts = alertPreferences?.getAlertHistory() ?: emptyList()
    private val initialBgMonitoring = alertPreferences?.isBackgroundMonitoringEnabled() ?: false

    private val _uiState = MutableStateFlow(
        MainUiState(
            isDarkTheme = securePreferences?.isDarkThemeEnabled() ?: true,
            autoRefreshEnabled = securePreferences?.isAutoRefreshEnabled() ?: true,
            alertSettings = initialAlertSettings,
            alerts = initialAlerts,
            unreadAlertCount = initialAlerts.count { !it.isRead },
            isBackgroundMonitoringActive = initialBgMonitoring
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var inAppAlertDismissJob: Job? = null
    private val alertEngine = MarketAlertEngine()
    private val paperManager: PaperTradingManager? = signalRepository?.let { PaperTradingManager(it) }
    private val marketNewsService = com.cloudscope.moneyprinter.domain.news.MarketNewsService()
    private var newsJob: Job? = null

    // Signal generation cooldown tracking (15 min cooldown for identical strike)
    private var lastRecordedSignalTime: Long = 0L
    private var lastRecordedStrike: Double = 0.0
    private var lastRecordedAction: SignalAction = SignalAction.NO_TRADE

    init {
        // Collect repository states
        repository?.let { repo ->
            viewModelScope.launch {
                repo.connectionState.collect { conn ->
                    _uiState.update { it.copy(connectionState = conn) }
                }
            }

            viewModelScope.launch {
                repo.latestQuote.collect { quote ->
                    updateMarketAnalysis(quote, repo.latestOptionChain.value)
                }
            }

            viewModelScope.launch {
                repo.latestOptionChain.collect { chain ->
                    updateMarketAnalysis(repo.latestQuote.value, chain)
                }
            }

            viewModelScope.launch {
                repo.latestFuturesQuote.collect { fut ->
                    _uiState.update { it.copy(futuresQuote = fut) }
                }
            }
        }

        // Collect signals from repository if available
        signalRepository?.let { repo ->
            viewModelScope.launch {
                repo.signalsFlow.collect { allSignals ->
                    refreshAccuracyViews(allSignals)
                }
            }
        }

        // Collect paper trading account state
        paperManager?.let { paper ->
            viewModelScope.launch {
                paper.accountState.collect { acct ->
                    _uiState.update { it.copy(paperAccount = acct) }
                }
            }
        }

        // Run default out-of-sample backtest simulation
        runBacktestSimulation()

        // Start polling loop only if repository is present and autoRefresh is enabled
        if (repository != null && _uiState.value.autoRefreshEnabled) {
            startAutoRefresh()
        }

        // Initial fetch
        refreshMarketData()
        refreshAccuracyData()
        startNewsMonitoring()
        viewModelScope.launch {
            paperManager?.refreshAccountState()
        }
    }

    fun selectTab(tab: TerminalNavTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setOptionChainSearch(query: String) {
        _uiState.update { it.copy(optionChainSearchQuery = query) }
    }

    fun setSelectedExpiry(expiry: String) {
        _uiState.update { it.copy(selectedExpiryDate = expiry) }
    }

    fun setAccuracyPeriod(period: AccuracyPeriod) {
        _uiState.update { it.copy(selectedAccuracyPeriod = period) }
        refreshAccuracyData()
    }

    fun setStrategyFilter(filter: StrategyFilter) {
        _uiState.update { it.copy(selectedStrategyFilter = filter) }
        refreshAccuracyData()
    }

    fun setOptionFilter(filter: OptionFilter) {
        _uiState.update { it.copy(selectedOptionFilter = filter) }
        refreshAccuracyData()
    }

    fun togglePaperTradingOnlyFilter() {
        _uiState.update { it.copy(paperTradingOnlyFilter = !it.paperTradingOnlyFilter) }
        refreshAccuracyData()
    }

    fun executePaperTrade(signal: TradingSignal) {
        val indicators = _uiState.value.indicators ?: return
        viewModelScope.launch {
            val record = paperManager?.executePaperTrade(signal, indicators)
            if (record != null) {
                _uiState.update {
                    it.copy(notificationMessage = "Paper Trade Placed: 1 Lot ${record.contractSymbol} at ₹${record.entryPrice}")
                }
            } else {
                _uiState.update {
                    it.copy(notificationMessage = "Paper Trade Failed: Insufficient virtual margin")
                }
            }
        }
    }

    fun closePaperTrade(record: HistoricalSignalRecord, reason: ExitReason = ExitReason.TARGET_1) {
        val chain = _uiState.value.optionChain ?: return
        val contract = chain.rows.find { it.strikePrice == record.strikePrice }?.let {
            if (record.optionType == OptionType.CE) it.callContract else it.putContract
        }
        val exitPrice = contract?.lastPrice ?: record.entryPrice

        viewModelScope.launch {
            paperManager?.closePaperPosition(record, exitPrice, reason)
            _uiState.update {
                it.copy(notificationMessage = "Paper Position Closed: ${record.contractSymbol} via $reason")
            }
        }
    }

    fun resetPaperAccount() {
        viewModelScope.launch {
            paperManager?.resetAccount()
            _uiState.update { it.copy(notificationMessage = "Paper Portfolio reset to ₹1,00,000 capital") }
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    fun requestTradeExecution(signal: TradingSignal, broker: BrokerType) {
        _uiState.update {
            it.copy(
                tradeConfirmationSignal = signal,
                targetExecutionBroker = broker
            )
        }
    }

    fun dismissTradeExecution() {
        _uiState.update {
            it.copy(
                tradeConfirmationSignal = null,
                targetExecutionBroker = null
            )
        }
    }

    fun toggleAutoRefresh() {
        val next = !_uiState.value.autoRefreshEnabled
        securePreferences?.setAutoRefreshEnabled(next)
        _uiState.update { it.copy(autoRefreshEnabled = next) }
    }

    fun toggleTheme() {
        val newTheme = !_uiState.value.isDarkTheme
        securePreferences?.setDarkThemeEnabled(newTheme)
        _uiState.update { it.copy(isDarkTheme = newTheme) }
    }

    fun setConfigDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showConfigDialog = visible, testConnectionResult = null) }
    }

    fun setBacktestDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showBacktestDialog = visible) }
    }

    fun refreshMarketData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            repository?.refreshMarketData()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun saveCredentials(
        apiKey: String,
        accessToken: String,
        brokerType: BrokerType = BrokerType.ZERODHA,
        clientId: String = ""
    ) {
        repository?.saveCredentials(apiKey, accessToken, brokerType, clientId)
        refreshMarketData()
    }

    fun saveBrokerType(brokerType: BrokerType) {
        repository?.saveBrokerType(brokerType)
        refreshMarketData()
    }

    fun clearCredentials() {
        repository?.clearCredentials()
        refreshMarketData()
    }

    fun testBrokerConnection(
        apiKey: String,
        accessToken: String,
        brokerType: BrokerType = BrokerType.ZERODHA
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(testConnectionResult = "Validating ${brokerType.displayName}...") }
            val result = repository?.testConnection(apiKey, accessToken, brokerType)
                ?: Result.failure(Exception("Repository unavailable"))
            if (result.isSuccess) {
                val successMsg = when (brokerType) {
                    BrokerType.NONE_STANDALONE -> "Success: Standalone Mode Active (Zero Broker Setup Required)"
                    BrokerType.GROWW -> "Success: Groww selected as Execution Broker"
                    BrokerType.ANGEL_ONE -> "Success: Angel One SmartAPI ready"
                    BrokerType.ZERODHA -> "Success: Connected to Zerodha Kite Connect"
                }
                _uiState.update { it.copy(testConnectionResult = successMsg) }
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Connection failed"
                _uiState.update { it.copy(testConnectionResult = "Failed: $error") }
            }
        }
    }

    fun runBacktestSimulation() {
        viewModelScope.launch {
            val candles = repository?.historicalCandles?.value ?: emptyList()
            val result = BacktestEngine.runBacktest(candles)
            _uiState.update { it.copy(backtestResult = result) }
        }
    }

    private fun startAutoRefresh() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            if (_uiState.value.autoRefreshEnabled) {
                repository?.refreshMarketData()
            }
            while (isActive) {
                delay(1000) // 1-second interval for real-time responsiveness
                if (_uiState.value.autoRefreshEnabled) {
                    repository?.refreshMarketData()
                }
            }
        }
    }

    private fun startNewsMonitoring() {
        newsJob?.cancel()
        newsJob = viewModelScope.launch {
            while (isActive) {
                runCatching {
                    val articles = marketNewsService.getLatestMarketNews()
                    if (articles.isNotEmpty()) {
                        _uiState.update { it.copy(marketNews = articles) }
                        val quote = _uiState.value.quote
                            ?: MarketQuote(symbol = "NIFTY 50", lastPrice = 24850.0, change = 0.0, pChange = 0.0, open = 24800.0, high = 24900.0, low = 24750.0, close = 24800.0)
                        val highImpact = articles.firstOrNull { it.impact == com.cloudscope.moneyprinter.domain.news.NewsImpactLevel.HIGH }
                        if (highImpact != null) {
                            val alert = alertEngine.evaluateHighImpactNews(highImpact, quote)
                            if (alert != null) {
                                handleTriggeredAlerts(listOf(alert))
                            }
                        }
                    }
                }.onFailure {
                    // Safe isolated news handling
                }
                delay(60000L)
            }
        }
    }

    private fun updateMarketAnalysis(quote: MarketQuote?, optionChain: OptionChain?) {
        val candles = repository?.historicalCandles?.value ?: emptyList()
        val isMarketHours = RiskManager.isWithinTradingHours()

        if (quote == null || candles.isEmpty()) {
            _uiState.update {
                it.copy(
                    quote = null,
                    signal = TradingSignal.noTrade("Awaiting live market data."),
                    normalOptionsSignal = NormalOptionsSignal.waitingForData(),
                    smartMoneyActivity = null,
                    smartMoneySignal = null,
                    indicators = null,
                    supportResistance = null,
                    isMarketOpen = isMarketHours
                )
            }
            return
        }

        // Validate Market Quote
        val quoteValidation = com.cloudscope.moneyprinter.data.validator.MarketDataValidator.validateQuote(quote, _uiState.value.quote)
        val latencyMs = quoteValidation.latencyMs
        val isStale = quoteValidation.isStale

        val pdh = quote.high
        val pdl = quote.low
        val pdc = quote.close
        val pcr = optionChain?.pcr ?: 1.05
        val vix = repository?.latestVix?.value ?: 15.0

        // Compute technical indicators
        val indicators = IndicatorEngine.evaluateIndicators(
            candles = candles,
            pdh = pdh,
            pdl = pdl,
            pdc = pdc,
            pcr = pcr,
            vix = vix
        )

        // Compute Support & Resistance levels
        val srLevels = IndicatorEngine.calculateSupportResistance(pdh, pdl, pdc)

        // Compute Smart Money Activity on Spot & Futures
        val futures = repository?.latestFuturesQuote?.value ?: _uiState.value.futuresQuote
        val smartMoneyActivity = SmartMoneyDetector.analyzeSmartMoney(
            spotQuote = quote,
            futuresQuote = futures,
            candles = candles,
            vwap = indicators.vwap,
            pdh = pdh,
            pdl = pdl,
            orbHigh = indicators.orbHigh,
            orbLow = indicators.orbLow
        )

        // Compute Option Chain institutional confirmation
        val optionsConfirmation = OptionsConfirmationEngine.confirmOptions(
            spotPrice = quote.lastPrice,
            optionChain = optionChain
        )

        // Generate Smart Money Options Signal (SMART MONEY BUY CE / SMART MONEY BUY PE / NO ACTIVITY)
        val smartMoneySignal = SmartMoneySignalEngine.generateSignal(
            spotQuote = quote,
            smartMoneyActivity = smartMoneyActivity,
            optionsDetails = optionsConfirmation,
            optionChain = optionChain
        )

        // Evaluate Smart Money Signal Alerts (in-app notification, duplicate suppression, setup change)
        evaluateSmartMoneySignalAlert(smartMoneySignal, quote)

        // Generate Normal Options Signal (independent technical analysis & price action engine)
        val normalOptionsSignal = NormalOptionsSignalEngine.generateSignal(
            quote = quote,
            candles = candles,
            indicators = indicators,
            optionChain = optionChain,
            supportResistance = srLevels
        )

        // Evaluate Normal Options Signal Alerts
        val normalAlert = alertEngine.evaluateNormalSignal(normalOptionsSignal, quote)
        if (normalAlert != null) {
            handleTriggeredAlerts(listOf(normalAlert))
        }

        // Detect Signal Conflict between Normal & Smart Money engines
        val conflictState = com.cloudscope.moneyprinter.domain.signal.SignalConflictEngine.detectConflict(normalOptionsSignal, smartMoneySignal)
        val conflictAlert = alertEngine.evaluateSignalConflict(conflictState, quote)
        if (conflictAlert != null) {
            handleTriggeredAlerts(listOf(conflictAlert))
        }

        // Generate signal
        val rawSignal = SignalEngine.generateSignal(
            quote = quote,
            indicators = indicators,
            optionChain = optionChain,
            candleCount = candles.size,
            isMarketHours = isMarketHours
        )

        // Enrich signal with empirical historical performance
        val enrichedSignal = enrichSignalWithHistoricalStats(rawSignal)

        // Evaluate Signal Outcome Tracking on existing open trades
        trackOpenSignalOutcomes(quote, optionChain)

        // Evaluate Cooldown & Auto-record new signals
        recordSignalIfQualified(enrichedSignal, indicators)

        // Evaluate Market Condition Alerts
        val triggeredAlerts = alertEngine.evaluate(
            quote = quote,
            indicators = indicators,
            signal = enrichedSignal,
            candles = candles,
            settings = _uiState.value.alertSettings
        )
        if (triggeredAlerts.isNotEmpty()) {
            handleTriggeredAlerts(triggeredAlerts)
        }

        _uiState.update {
            it.copy(
                quote = quote,
                futuresQuote = futures,
                signal = enrichedSignal,
                normalOptionsSignal = normalOptionsSignal,
                smartMoneyActivity = smartMoneyActivity,
                smartMoneySignal = smartMoneySignal,
                signalConflictState = conflictState,
                feedLatencyMs = latencyMs,
                isDataStale = isStale,
                validationReason = quoteValidation.reason,
                indicators = indicators,
                optionChain = optionChain,
                supportResistance = srLevels,
                isMarketOpen = isMarketHours
            )
        }
    }

    private fun enrichSignalWithHistoricalStats(signal: TradingSignal): TradingSignal {
        val repo = signalRepository ?: return signal
        val allSignals = repo.signalsFlow.value
        val executed = allSignals.filter { it.entryMet && it.status == SignalLifecycleStatus.CLOSED }

        val comparable = executed.filter { it.strategyType == signal.strategyType }
        val sampleSize = comparable.size
        if (sampleSize == 0) {
            return signal.copy(
                comparableTradesCount = 0,
                historicalStrategyWinRate = 0.0,
                historicalProfitFactor = 0.0,
                hasSufficientHistoricalSample = false
            )
        }

        val wins = comparable.count { it.outcome == TradeOutcome.WIN }
        val winRate = (wins.toDouble() / sampleSize) * 100.0
        val grossProfit = comparable.filter { it.outcome == TradeOutcome.WIN }.sumOf { kotlin.math.max(0.0, it.grossPnl) }
        val grossLoss = comparable.filter { it.outcome == TradeOutcome.LOSS }.sumOf { abs(it.grossPnl) }
        val pf = if (grossLoss > 0.0) grossProfit / grossLoss else if (grossProfit > 0) 9.99 else 0.0

        return signal.copy(
            comparableTradesCount = sampleSize,
            historicalStrategyWinRate = Math.round(winRate * 10.0) / 10.0,
            historicalProfitFactor = Math.round(pf * 100.0) / 100.0,
            hasSufficientHistoricalSample = sampleSize >= 5
        )
    }

    private fun recordSignalIfQualified(signal: TradingSignal, indicators: TechnicalIndicators) {
        val repo = signalRepository ?: return

        // Only record active BUY_CE or BUY_PE setups with >= 60% confidence
        if (signal.action != SignalAction.BUY_CE && signal.action != SignalAction.BUY_PE) return
        if (signal.confidencePercent < 60) return

        val now = System.currentTimeMillis()
        val cooldownMillis = 15 * 60 * 1000L // 15 minutes cooldown

        val isCooldownActive = (now - lastRecordedSignalTime) < cooldownMillis &&
                lastRecordedStrike == signal.recommendedStrike &&
                lastRecordedAction == signal.action

        if (!isCooldownActive) {
            lastRecordedSignalTime = now
            lastRecordedStrike = signal.recommendedStrike
            lastRecordedAction = signal.action

            val record = SignalOutcomeTracker.createRecordFromSignal(signal, indicators, isPaperTrade = false)
            viewModelScope.launch {
                repo.recordGeneratedSignal(record)
            }
        }
    }

    private fun trackOpenSignalOutcomes(quote: MarketQuote, optionChain: OptionChain?) {
        val repo = signalRepository ?: return
        val chain = optionChain ?: return

        viewModelScope.launch {
            val openSignals = repo.getOpenSignals()
            for (signal in openSignals) {
                // Find matching option quote
                val row = chain.rows.find { it.strikePrice == signal.strikePrice }
                val optContract = if (row != null) {
                    if (signal.optionType == OptionType.CE) row.callContract else row.putContract
                } else null

                val ltp = optContract?.lastPrice ?: signal.entryPrice
                val high = ltp
                val low = ltp

                val evaluated = SignalOutcomeTracker.evaluateSignal(
                    record = signal,
                    currentOptionLtp = ltp,
                    candleHigh = high,
                    candleLow = low,
                    currentSpotPrice = quote.lastPrice,
                    currentTimestamp = System.currentTimeMillis()
                )

                if (evaluated != signal) {
                    repo.updateSignalLifecycle(evaluated)
                    if (signal.isPaperTrade) {
                        paperManager?.refreshAccountState()
                    }
                }
            }
        }
    }

    private fun refreshAccuracyData() {
        val repo = signalRepository ?: return
        viewModelScope.launch {
            val fromTimestamp = when (_uiState.value.selectedAccuracyPeriod) {
                AccuracyPeriod.TODAY -> System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
                AccuracyPeriod.LAST_7_DAYS -> System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                AccuracyPeriod.LAST_30_DAYS -> System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                AccuracyPeriod.LAST_90_DAYS -> System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
                AccuracyPeriod.ALL_TIME -> 0L
            }

            val filtered = repo.getSignalsFiltered(
                fromTimestamp = fromTimestamp,
                optionFilter = _uiState.value.selectedOptionFilter,
                strategyFilter = _uiState.value.selectedStrategyFilter,
                paperTradesOnly = if (_uiState.value.paperTradingOnlyFilter) true else null
            )

            refreshAccuracyViews(filtered)
        }
    }

    private fun refreshAccuracyViews(signals: List<HistoricalSignalRecord>) {
        val repo = signalRepository ?: return
        val metrics = repo.computeAccuracyMetrics(signals)
        val insights = StrategyImprovementEngine.generateInsights(signals)
        val comparisons = StrategyImprovementEngine.compareStrategyVersions(signals)
        val cohorts = StrategyImprovementEngine.evaluateStrategyCohorts(signals)
        val auditReport = com.cloudscope.moneyprinter.domain.audit.ReconciliationAuditor.auditLedgerAndMetrics(signals, metrics)

        _uiState.update {
            it.copy(
                historicalSignals = signals,
                accuracyMetrics = metrics,
                strategyInsights = insights,
                strategyComparisons = comparisons,
                strategyCohortMetrics = cohorts,
                reconciliationReport = auditReport
            )
        }
    }

    fun selectSignalDetail(signal: HistoricalSignalRecord?) {
        _uiState.update {
            it.copy(
                selectedDetailSignal = signal,
                isSignalDetailLoading = false,
                signalDetailError = null
            )
        }
    }

    fun selectSignalDetailById(signalId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSignalDetailLoading = true, signalDetailError = null) }
            try {
                val record = _uiState.value.historicalSignals.find { it.id == signalId }
                    ?: signalRepository?.getSignalById(signalId)
                if (record != null) {
                    _uiState.update {
                        it.copy(
                            selectedDetailSignal = record,
                            isSignalDetailLoading = false,
                            signalDetailError = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            selectedDetailSignal = null,
                            isSignalDetailLoading = false,
                            signalDetailError = "Signal record #$signalId was not found in audit database."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        selectedDetailSignal = null,
                        isSignalDetailLoading = false,
                        signalDetailError = "Failed to load signal #$signalId: ${e.message}"
                    )
                }
            }
        }
    }

    fun dismissSignalDetail() {
        _uiState.update {
            it.copy(
                selectedDetailSignal = null,
                isSignalDetailLoading = false,
                signalDetailError = null
            )
        }
    }

    fun retrySignalDetail(signalId: String) {
        selectSignalDetailById(signalId)
    }

    fun getBrokerCredentials(): BrokerCredentials = repository?.getCredentials() ?: BrokerCredentials()

    // ----------------------------------------------------
    // Smart Money Options Alert Evaluation
    // ----------------------------------------------------
    private fun evaluateSmartMoneySignalAlert(
        smartMoneySignal: SmartMoneySignal,
        quote: MarketQuote
    ) {
        val alert = alertEngine.evaluateSmartMoneySignal(smartMoneySignal, quote)
        if (alert != null) {
            handleTriggeredAlerts(listOf(alert))
        }
    }

    // ----------------------------------------------------
    // Real-Time Notification & Alert Controls
    // ----------------------------------------------------
    private fun handleTriggeredAlerts(newAlerts: List<MarketAlert>) {
        val settings = _uiState.value.alertSettings
        for (alert in newAlerts) {
            notificationDispatcher?.dispatchAlert(alert, settings)
        }

        val currentList = _uiState.value.alerts.toMutableList()
        currentList.addAll(0, newAlerts)
        val trimmed = currentList.take(100)
        val unread = trimmed.count { !it.isRead }
        val latest = newAlerts.lastOrNull()

        _uiState.update {
            it.copy(
                alerts = trimmed,
                unreadAlertCount = unread,
                activeInAppAlert = latest
            )
        }

        alertPreferences?.saveAlertHistory(trimmed)

        inAppAlertDismissJob?.cancel()
        inAppAlertDismissJob = viewModelScope.launch {
            delay(6000L) // Auto-dismiss banner after 6 seconds
            _uiState.update { it.copy(activeInAppAlert = null) }
        }
    }

    fun setAlertsPanelVisible(visible: Boolean) {
        _uiState.update { it.copy(showAlertsPanel = visible) }
    }

    fun dismissInAppAlert() {
        inAppAlertDismissJob?.cancel()
        _uiState.update { it.copy(activeInAppAlert = null) }
    }

    fun markAllAlertsAsRead() {
        val updated = _uiState.value.alerts.map { it.copy(isRead = true) }
        _uiState.update {
            it.copy(
                alerts = updated,
                unreadAlertCount = 0
            )
        }
        alertPreferences?.saveAlertHistory(updated)
    }

    fun clearAlertHistory() {
        _uiState.update {
            it.copy(
                alerts = emptyList(),
                unreadAlertCount = 0
            )
        }
        alertPreferences?.saveAlertHistory(emptyList())
    }

    fun updateAlertSettings(settings: AlertSettings) {
        alertPreferences?.saveAlertSettings(settings)
        _uiState.update { it.copy(alertSettings = settings) }
    }

    fun toggleBackgroundMonitoring(context: Context) {
        val current = _uiState.value.isBackgroundMonitoringActive
        val next = !current
        alertPreferences?.setBackgroundMonitoringEnabled(next)
        _uiState.update { it.copy(isBackgroundMonitoringActive = next) }
        if (next) {
            MarketMonitoringService.startService(context)
        } else {
            MarketMonitoringService.stopService(context)
        }
    }

    fun triggerTestAlert() {
        val currentPrice = _uiState.value.quote?.lastPrice ?: 24850.50
        val testAlert = MarketAlert(
            type = MarketAlertType.DIRECTION_BULLISH,
            title = "🟢 Test Alert: NIFTY 50 Shifted BULLISH",
            message = "Dynamic AI directional confidence +18% on confirmed VWAP breakout above ₹${String.format(java.util.Locale.US, "%.2f", currentPrice)}.",
            spotPrice = currentPrice,
            previousDirection = MarketDirection.SIDEWAYS,
            newDirection = MarketDirection.BULLISH,
            previousConfidence = 52,
            newConfidence = 76
        )
        handleTriggeredAlerts(listOf(testAlert))
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        inAppAlertDismissJob?.cancel()
    }
}
