package com.cloudscope.moneyprinter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.data.model.BrokerType
import com.cloudscope.moneyprinter.data.model.ConnectionStatus
import com.cloudscope.moneyprinter.ui.components.ApiConfigDialog
import com.cloudscope.moneyprinter.ui.components.BacktestDialog
import com.cloudscope.moneyprinter.ui.components.BottomNavBar
import com.cloudscope.moneyprinter.ui.components.OrderConfirmationDialog
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import com.cloudscope.moneyprinter.ui.viewmodel.MainViewModel
import com.cloudscope.moneyprinter.ui.viewmodel.TerminalNavTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalTerminalColors.current
    val credentials = viewModel.getBrokerCredentials()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colors.bullish)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CLOUDSCOPE MONEY PRINTER",
                                color = colors.textPrimary,
                                fontSize = 17.sp, // Requirement: readable title, no tiny labels
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "NIFTY OPTIONS TERMINAL • ${credentials.brokerType.displayName.uppercase()}",
                            color = colors.textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    // Manual Refresh Button (Touch target >= 44dp)
                    IconButton(
                        onClick = { viewModel.refreshMarketData() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = if (uiState.isRefreshing) colors.bullish else colors.textSecondary
                        )
                    }

                    // Theme Toggle Button (Touch target >= 44dp)
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkTheme) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                            contentDescription = "Toggle Theme",
                            tint = colors.textSecondary
                        )
                    }

                    // Broker / Demat Setup Button (Touch target >= 44dp)
                    IconButton(
                        onClick = { viewModel.setConfigDialogVisible(true) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Broker Setup",
                            tint = if (credentials.isConfigured) colors.bullish else colors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface
                )
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        containerColor = colors.background,
        modifier = modifier
    ) { paddingValues ->
        // Responsive Container: Adapts smoothly to 390px mobile viewports up to large desktop/tablet screens
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 680.dp) // Centered max width on tablet & desktop
            ) {
                when (uiState.selectedTab) {
                    TerminalNavTab.DASHBOARD -> DashboardScreen(uiState = uiState, viewModel = viewModel)
                    TerminalNavTab.SIGNALS -> LiveSignalsScreen(uiState = uiState, viewModel = viewModel)
                    TerminalNavTab.OPTION_CHAIN -> OptionChainScreen(uiState = uiState, viewModel = viewModel)
                    TerminalNavTab.PERFORMANCE -> PerformanceScreen(uiState = uiState, viewModel = viewModel)
                    TerminalNavTab.ANALYSIS -> AnalysisScreen(uiState = uiState, viewModel = viewModel)
                    TerminalNavTab.SETTINGS -> SettingsScreen(uiState = uiState, viewModel = viewModel)
                }
            }
        }

        // ==========================================
        // DIALOGS & OVERLAYS
        // ==========================================
        // Order Execution Confirmation Dialog
        val confirmationSignal = uiState.tradeConfirmationSignal
        val targetBroker = uiState.targetExecutionBroker
        if (confirmationSignal != null && targetBroker != null) {
            OrderConfirmationDialog(
                signal = confirmationSignal,
                broker = targetBroker,
                onDismiss = { viewModel.dismissTradeExecution() }
            )
        }

        // Broker Credentials Config Dialog
        if (uiState.showConfigDialog) {
            ApiConfigDialog(
                initialCredentials = credentials,
                onDismiss = { viewModel.setConfigDialogVisible(false) },
                onSaveCredentials = { key, token, type, clientId ->
                    viewModel.saveCredentials(key, token, type, clientId)
                },
                onSaveBrokerType = { type ->
                    viewModel.saveBrokerType(type)
                },
                onClear = { viewModel.clearCredentials() },
                onTestConnection = { key, token, type ->
                    viewModel.testBrokerConnection(key, token, type)
                },
                testResult = uiState.testConnectionResult
            )
        }

        // Backtest Simulation Dialog
        if (uiState.showBacktestDialog) {
            BacktestDialog(
                result = uiState.backtestResult,
                onDismiss = { viewModel.setBacktestDialogVisible(false) },
                onRunBacktest = { viewModel.runBacktestSimulation() }
            )
        }
    }
}
