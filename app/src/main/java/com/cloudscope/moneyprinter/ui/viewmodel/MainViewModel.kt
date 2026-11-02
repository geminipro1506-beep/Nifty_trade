package com.cloudscope.moneyprinter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudscope.moneyprinter.data.local.SecurePreferences
import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.data.repository.MarketDataRepository
import com.cloudscope.moneyprinter.domain.backtest.BacktestEngine
import com.cloudscope.moneyprinter.domain.indicator.IndicatorEngine
import com.cloudscope.moneyprinter.domain.risk.RiskManager
import com.cloudscope.moneyprinter.domain.signal.SignalEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: MarketDataRepository,
    private val securePreferences: SecurePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(
            isDarkTheme = securePreferences.isDarkThemeEnabled(),
            autoRefreshEnabled = securePreferences.isAutoRefreshEnabled()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        // Collect repository states
        viewModelScope.launch {
            repository.connectionState.collect { conn ->
                _uiState.update { it.copy(connectionState = conn) }
            }
        }

        viewModelScope.launch {
            repository.latestQuote.collect { quote ->
                updateMarketAnalysis(quote, repository.latestOptionChain.value)
            }
        }

        viewModelScope.launch {
            repository.latestOptionChain.collect { chain ->
                updateMarketAnalysis(repository.latestQuote.value, chain)
            }
        }

        // Run default out-of-sample backtest simulation
        runBacktestSimulation()

        // Start polling loop
        startAutoRefresh()

        // Initial fetch
        refreshMarketData()
    }

    fun toggleTheme() {
        val newTheme = !_uiState.value.isDarkTheme
        securePreferences.setDarkThemeEnabled(newTheme)
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
            repository.refreshMarketData()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun saveCredentials(
        apiKey: String,
        accessToken: String,
        brokerType: BrokerType = BrokerType.ZERODHA,
        clientId: String = ""
    ) {
        repository.saveCredentials(apiKey, accessToken, brokerType, clientId)
        refreshMarketData()
    }

    fun saveBrokerType(brokerType: BrokerType) {
        repository.saveBrokerType(brokerType)
        refreshMarketData()
    }

    fun clearCredentials() {
        repository.clearCredentials()
        refreshMarketData()
    }

    fun testBrokerConnection(
        apiKey: String,
        accessToken: String,
        brokerType: BrokerType = BrokerType.ZERODHA
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(testConnectionResult = "Validating ${brokerType.displayName}...") }
            val result = repository.testConnection(apiKey, accessToken, brokerType)
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
            val candles = repository.historicalCandles.value
            val result = BacktestEngine.runBacktest(candles)
            _uiState.update { it.copy(backtestResult = result) }
        }
    }

    private fun startAutoRefresh() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(5000) // 5 second polling interval
                if (_uiState.value.autoRefreshEnabled) {
                    repository.refreshMarketData()
                }
            }
        }
    }

    private fun updateMarketAnalysis(quote: MarketQuote?, optionChain: OptionChain?) {
        val candles = repository.historicalCandles.value
        val isMarketHours = RiskManager.isWithinTradingHours()

        if (quote == null || candles.isEmpty()) {
            _uiState.update {
                it.copy(
                    quote = null,
                    signal = TradingSignal.noTrade("Awaiting live market data."),
                    indicators = null,
                    supportResistance = null,
                    isMarketOpen = isMarketHours
                )
            }
            return
        }

        val pdh = quote.high
        val pdl = quote.low
        val pdc = quote.close
        val pcr = optionChain?.pcr ?: 1.05
        val vix = repository.latestVix.value

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

        // Generate signal
        val signal = SignalEngine.generateSignal(
            quote = quote,
            indicators = indicators,
            optionChain = optionChain,
            candleCount = candles.size,
            isMarketHours = isMarketHours
        )

        _uiState.update {
            it.copy(
                quote = quote,
                signal = signal,
                indicators = indicators,
                optionChain = optionChain,
                supportResistance = srLevels,
                isMarketOpen = isMarketHours
            )
        }
    }

    fun getBrokerCredentials(): BrokerCredentials = repository.getCredentials()

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
