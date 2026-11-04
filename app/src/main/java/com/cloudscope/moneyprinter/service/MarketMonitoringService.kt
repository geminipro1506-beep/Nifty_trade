package com.cloudscope.moneyprinter.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.cloudscope.moneyprinter.data.local.AlertPreferences
import com.cloudscope.moneyprinter.data.local.SecurePreferences
import com.cloudscope.moneyprinter.data.repository.MarketDataRepositoryImpl
import com.cloudscope.moneyprinter.domain.alert.AndroidNotificationDispatcher
import com.cloudscope.moneyprinter.domain.alert.MarketAlertEngine
import com.cloudscope.moneyprinter.domain.indicator.IndicatorEngine
import com.cloudscope.moneyprinter.domain.risk.RiskManager
import com.cloudscope.moneyprinter.domain.signal.SignalEngine
import kotlinx.coroutines.*

class MarketMonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var dispatcher: AndroidNotificationDispatcher
    private lateinit var alertPreferences: AlertPreferences
    private lateinit var repository: MarketDataRepositoryImpl
    private val alertEngine = MarketAlertEngine()
    private var monitoringJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        dispatcher = AndroidNotificationDispatcher(this)
        alertPreferences = AlertPreferences(this)
        val securePreferences = SecurePreferences(this)
        repository = MarketDataRepositoryImpl(securePreferences)
        Log.i(TAG, "MarketMonitoringService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceInternal()
        startMonitoring()
        return START_STICKY
    }

    private fun startForegroundServiceInternal() {
        val notification = dispatcher.buildMonitoringNotification("Monitoring NIFTY 50 live feed in background")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    AndroidNotificationDispatcher.MONITORING_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(AndroidNotificationDispatcher.MONITORING_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}")
        }
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            Log.i(TAG, "Background market monitoring loop started")
            while (isActive) {
                try {
                    val settings = alertPreferences.getAlertSettings()
                    if (settings.notificationsEnabled) {
                        repository.refreshMarketData()
                        val quote = repository.latestQuote.value
                        val candles = repository.historicalCandles.value
                        val optionChain = repository.latestOptionChain.value

                        if (quote != null && candles.isNotEmpty()) {
                            val pdh = quote.high
                            val pdl = quote.low
                            val pdc = quote.close
                            val pcr = optionChain?.pcr ?: 1.05
                            val vix = repository.latestVix.value

                            val indicators = IndicatorEngine.evaluateIndicators(
                                candles = candles,
                                pdh = pdh,
                                pdl = pdl,
                                pdc = pdc,
                                pcr = pcr,
                                vix = vix
                            )

                            val signal = SignalEngine.generateSignal(
                                quote = quote,
                                indicators = indicators,
                                optionChain = optionChain,
                                candleCount = candles.size,
                                isMarketHours = RiskManager.isWithinTradingHours()
                            )

                            val triggered = alertEngine.evaluate(
                                quote = quote,
                                indicators = indicators,
                                signal = signal,
                                candles = candles,
                                settings = settings
                            )

                            if (triggered.isNotEmpty()) {
                                val currentHistory = alertPreferences.getAlertHistory().toMutableList()
                                for (alert in triggered) {
                                    dispatcher.dispatchAlert(alert, settings)
                                    currentHistory.add(0, alert)
                                }
                                alertPreferences.saveAlertHistory(currentHistory)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error in background evaluation cycle: ${e.message}")
                }
                delay(12000L) // Poll every 12 seconds in background to conserve battery while staying responsive
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        alertPreferences.setBackgroundMonitoringEnabled(false)
        Log.i(TAG, "Background market monitoring stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        alertEngine.resetState()
        Log.i(TAG, "MarketMonitoringService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "MarketMonitoringService"
        const val ACTION_START = "com.cloudscope.moneyprinter.ACTION_START_MONITORING"
        const val ACTION_STOP = "com.cloudscope.moneyprinter.ACTION_STOP_MONITORING"

        fun startService(context: Context) {
            val intent = Intent(context, MarketMonitoringService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MarketMonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
