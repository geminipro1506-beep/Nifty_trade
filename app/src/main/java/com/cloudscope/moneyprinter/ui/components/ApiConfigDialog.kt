package com.cloudscope.moneyprinter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cloudscope.moneyprinter.data.model.BrokerCredentials
import com.cloudscope.moneyprinter.data.model.BrokerType
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigDialog(
    initialCredentials: BrokerCredentials,
    onDismiss: () -> Unit,
    onSaveCredentials: (apiKey: String, accessToken: String, brokerType: BrokerType, clientId: String) -> Unit,
    onSaveBrokerType: (brokerType: BrokerType) -> Unit,
    onClear: () -> Unit,
    onTestConnection: (apiKey: String, accessToken: String, brokerType: BrokerType) -> Unit,
    testResult: String?
) {
    val colors = LocalTerminalColors.current
    var selectedBroker by remember { mutableStateOf(initialCredentials.brokerType) }
    var apiKey by remember { mutableStateOf(initialCredentials.apiKey) }
    var accessToken by remember { mutableStateOf(initialCredentials.accessToken) }
    var clientId by remember { mutableStateOf(initialCredentials.clientId) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BROKER & DEMAT SETUP",
                        color = colors.textPrimary,
                        fontSize = 19.sp, // Requirement: 20-24px
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.bullishBg)
                        .border(1.dp, colors.bullish.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "✓ DEMAT SETUP IS 100% OPTIONAL\nCloudscope Money Printer runs autonomously using real-time public NSE market feeds. You never need to connect a broker to view signals.",
                        color = colors.bullish,
                        fontSize = 14.5.sp, // Minimum 14-16px
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SELECT DATA FEED OR BROKER ROUTING:",
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Broker Selection Options (Touch target >= 48dp)
                BrokerType.values().forEach { broker ->
                    val isSelected = selectedBroker == broker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(52.dp) // Touch target >= 48dp
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.cardElevated else colors.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) colors.bullish else colors.border,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedBroker = broker }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedBroker = broker },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.bullish,
                                    unselectedColor = colors.textMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = broker.displayName,
                                color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode descriptions & inputs
                when (selectedBroker) {
                    BrokerType.NONE_STANDALONE -> {
                        Text(
                            text = "Uses free, unthrottled real-time National Stock Exchange (NSE) quotes and Black-Scholes Greeks. Perfect for live signal tracking without broker credentials.",
                            color = colors.textSecondary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                    BrokerType.GROWW -> {
                        Text(
                            text = "Groww is configured for 1-tap trade routing. When signals trigger, you can immediately open your Groww order window with pre-filled strike parameters.",
                            color = colors.textSecondary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                    BrokerType.ANGEL_ONE -> {
                        Text(
                            text = "Angel One SmartAPI credentials (optional):",
                            color = colors.textSecondary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("SmartAPI Key (Optional)", fontSize = 14.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = clientId,
                            onValueChange = { clientId = it },
                            label = { Text("Angel One Client ID (Optional)", fontSize = 14.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                    }
                    BrokerType.ZERODHA -> {
                        Text(
                            text = "Enter Zerodha Kite Connect credentials for tick streaming:",
                            color = colors.textSecondary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key", fontSize = 14.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = accessToken,
                            onValueChange = { accessToken = it },
                            label = { Text("Access Token", fontSize = 14.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                    }
                }

                if (testResult != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = testResult,
                        color = if (testResult.startsWith("Success")) colors.bullish else colors.bearish,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onTestConnection(apiKey, accessToken, selectedBroker) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("TEST FEED", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (selectedBroker == BrokerType.NONE_STANDALONE || selectedBroker == BrokerType.GROWW) {
                                onSaveBrokerType(selectedBroker)
                            } else {
                                onSaveCredentials(apiKey, accessToken, selectedBroker, clientId)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.3f).height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.bullish)
                    ) {
                        Text("SAVE & APPLY", color = colors.background, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        onClear()
                        selectedBroker = BrokerType.NONE_STANDALONE
                        apiKey = ""
                        accessToken = ""
                        clientId = ""
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally).height(44.dp)
                ) {
                    Text("Reset to Autonomous Mode", color = colors.bearish, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
