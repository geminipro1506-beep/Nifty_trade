package com.cloudscope.moneyprinter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.data.model.ConnectionStatus
import com.cloudscope.moneyprinter.ui.components.*
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import com.cloudscope.moneyprinter.ui.viewmodel.MainViewModel

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
                        Text(
                            text = "CLOUDSCOPE MONEY PRINTER",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "NIFTY INTRADAY OPTIONS TERMINAL",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                actions = {
                    // Backtest Audit Button
                    IconButton(onClick = { viewModel.setBacktestDialogVisible(true) }) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Historical Audit",
                            tint = colors.textSecondary
                        )
                    }

                    // API Setup Button
                    IconButton(onClick = { viewModel.setConfigDialogVisible(true) }) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Broker Credentials",
                            tint = if (credentials.isConfigured) colors.bullish else colors.sideways
                        )
                    }

                    // Theme Toggle Button
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            imageVector = if (uiState.isDarkTheme) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                            contentDescription = "Toggle Theme",
                            tint = colors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                )
            )
        },
        containerColor = colors.background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Connection Status Bar
            ConnectionBar(
                state = uiState.connectionState,
                onRefreshClick = { viewModel.refreshMarketData() },
                onConfigureClick = { viewModel.setConfigDialogVisible(true) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Credentials setup banner if missing
            if (uiState.connectionState.status == ConnectionStatus.NEEDS_SETUP) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.card)
                        .border(1.dp, colors.sideways.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BROKER SETUP REQUIRED",
                                color = colors.sideways,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Connect Kite Connect credentials for live NIFTY ticks and option chains.",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.setConfigDialogVisible(true) },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.bullish),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Connect", color = colors.background, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Priority 1: Top Market Direction + AI Confidence Tile
            MarketSignalTile(
                quote = uiState.quote,
                signal = uiState.signal
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Priority 2 & 3: Current Trading Signal (Action, Entry, SL, T1, T2, R:R)
            TradeSignalCard(
                signal = uiState.signal
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Priority 4: Compact Market Indicators
            IndicatorsGrid(
                indicators = uiState.indicators
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Priority 5: Option Chain Summary
            OptionChainSummaryCard(
                optionChain = uiState.optionChain
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Priority 6: Support & Resistance
            SupportResistanceCard(
                levels = uiState.supportResistance,
                currentSpot = uiState.quote?.lastPrice
            )

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Dialogs
        if (uiState.showConfigDialog) {
            ApiConfigDialog(
                initialApiKey = credentials.apiKey,
                initialAccessToken = credentials.accessToken,
                onDismiss = { viewModel.setConfigDialogVisible(false) },
                onSave = { key, token -> viewModel.saveCredentials(key, token) },
                onClear = { viewModel.clearCredentials() },
                onTestConnection = { key, token -> viewModel.testBrokerConnection(key, token) },
                testResult = uiState.testConnectionResult
            )
        }

        if (uiState.showBacktestDialog) {
            BacktestDialog(
                result = uiState.backtestResult,
                onDismiss = { viewModel.setBacktestDialogVisible(false) },
                onRunBacktest = { viewModel.runBacktestSimulation() }
            )
        }
    }
}
