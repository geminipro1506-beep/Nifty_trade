package com.cloudscope.moneyprinter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            Log.e("MainActivity", "Safe fallback initializing MainViewModel", e)
            try {
                viewModel = MainViewModel()
            } catch (t: Throwable) {
                Log.e("MainActivity", "Critical fallback initializing empty MainViewModel", t)
                viewModel = MainViewModel()
            }
        }

        setContent {
            var composeCrashError by remember { mutableStateOf<Throwable?>(null) }

            if (composeCrashError != null) {
                // Graceful, terminal-themed recovery dashboard
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0B0E14))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "NIFTY 50 REALTIME TERMINAL",
                            color = Color(0xFF00E676),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Terminal Session Recovered",
                            color = Color(0xFFFF5252),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = composeCrashError?.localizedMessage ?: "Display recovered from unexpected state.",
                            color = Color(0xFF8B949E),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                composeCrashError = null
                                viewModel.refreshMarketData()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                        ) {
                            Text(
                                text = "RELOAD MARKET FEED",
                                color = Color(0xFF0B0E14),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                val uiState by viewModel.uiState.collectAsState()
                CloudScopeTheme(darkTheme = uiState.isDarkTheme) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
