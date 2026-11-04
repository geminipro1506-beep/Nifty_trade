package com.cloudscope.moneyprinter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cloudscope.moneyprinter.data.local.AlertPreferences
import com.cloudscope.moneyprinter.data.local.SecurePreferences
import com.cloudscope.moneyprinter.data.local.db.SignalDatabaseHelper
import com.cloudscope.moneyprinter.data.repository.MarketDataRepositoryImpl
import com.cloudscope.moneyprinter.data.repository.SignalRepositoryImpl
import com.cloudscope.moneyprinter.domain.alert.AndroidNotificationDispatcher
import com.cloudscope.moneyprinter.ui.screens.HomeScreen
import com.cloudscope.moneyprinter.ui.theme.CloudScopeTheme
import com.cloudscope.moneyprinter.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val securePreferences = SecurePreferences(applicationContext)
            val alertPreferences = AlertPreferences(applicationContext)
            val notificationDispatcher = AndroidNotificationDispatcher(applicationContext)
            val repository = MarketDataRepositoryImpl(securePreferences)
            val dbHelper = SignalDatabaseHelper.getInstance(applicationContext)
            val signalRepository = SignalRepositoryImpl(dbHelper)
            viewModel = MainViewModel(
                repository = repository,
                securePreferences = securePreferences,
                signalRepository = signalRepository,
                alertPreferences = alertPreferences,
                notificationDispatcher = notificationDispatcher
            )
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Safe fallback initializing MainViewModel", e)
            viewModel = MainViewModel()
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            CloudScopeTheme(darkTheme = uiState.isDarkTheme) {
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
