package com.cloudscope.moneyprinter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cloudscope.moneyprinter.data.local.SecurePreferences
import com.cloudscope.moneyprinter.data.repository.MarketDataRepositoryImpl
import com.cloudscope.moneyprinter.ui.screens.HomeScreen
import com.cloudscope.moneyprinter.ui.theme.CloudScopeTheme
import com.cloudscope.moneyprinter.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val securePreferences = SecurePreferences(applicationContext)
        val repository = MarketDataRepositoryImpl(securePreferences)
        viewModel = MainViewModel(repository, securePreferences)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            CloudScopeTheme(darkTheme = uiState.isDarkTheme) {
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
