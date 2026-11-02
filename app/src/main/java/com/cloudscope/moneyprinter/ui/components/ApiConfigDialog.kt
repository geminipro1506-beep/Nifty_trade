package com.cloudscope.moneyprinter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "BROKER & DEMAT SETUP (OPTIONAL)",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Optional notice banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.bullish.copy(alpha = 0.12f))
                        .border(1.dp, colors.bullish.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "✓ DEMAT SETUP IS 100% OPTIONAL\nCloudscope Money Printer streams live NIFTY & VIX market data autonomously. You do not need to link any broker account.",
                        color = colors.bullish,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SELECT YOUR BROKER OR DATA MODE:",
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Broker selection chips
                BrokerType.values().forEach { broker ->
                    val isSelected = selectedBroker == broker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) colors.card else colors.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) colors.bullish else colors.border,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedBroker = broker }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedBroker = broker },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.bullish,
                                unselectedColor = colors.textMuted
                            ),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = broker.displayName,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Specific broker inputs
                when (selectedBroker) {
                    BrokerType.NONE_STANDALONE -> {
                        Text(
                            text = "Uses free, unthrottled real-time National Stock Exchange (NSE) indices and synthetic Black-Scholes Greeks. Perfect for charting and signal tracking without any Demat accounts.",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                    BrokerType.GROWW -> {
                        Text(
                            text = "Groww is enabled as your preferred execution terminal. Trade signals in the app will display instant 1-tap routing into the Groww mobile app or web platform.",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                    BrokerType.ANGEL_ONE -> {
                        Text(
                            text = "Angel One SmartAPI credentials (optional):",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("SmartAPI Key (Optional)", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.bullish,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = clientId,
                            onValueChange = { clientId = it },
                            label = { Text("Angel One Client ID (Optional)", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.bullish,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    BrokerType.ZERODHA -> {
                        Text(
                            text = "Enter Kite Connect credentials for direct tick streaming:",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.bullish,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = accessToken,
                            onValueChange = { accessToken = it },
                            label = { Text("Access Token", fontSize = 11.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.bullish,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (testResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = testResult,
                        color = if (testResult.startsWith("Success")) colors.bullish else colors.bearish,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onTestConnection(apiKey, accessToken, selectedBroker)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Test", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
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
                        modifier = Modifier.weight(1.4f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.bullish,
                            contentColor = colors.background
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Save & Apply", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = {
                        onClear()
                        selectedBroker = BrokerType.NONE_STANDALONE
                        apiKey = ""
                        accessToken = ""
                        clientId = ""
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Reset to Autonomous Mode", color = colors.bearish, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
