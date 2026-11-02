package com.cloudscope.moneyprinter.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cloudscope.moneyprinter.data.model.BrokerType
import com.cloudscope.moneyprinter.data.model.TradingSignal
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors

@Composable
fun OrderConfirmationDialog(
    signal: TradingSignal,
    broker: BrokerType,
    onDismiss: () -> Unit
) {
    val colors = LocalTerminalColors.current
    val context = LocalContext.current
    var acknowledgedRisk by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Risk Warning",
                            tint = colors.sideways,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ORDER CONFIRMATION",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Trade Summary Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.cardElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = signal.contractSymbol,
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Broker Target: ${broker.displayName}",
                            color = colors.bullish,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("ENTRY PRICE", color = colors.textMuted, fontSize = 13.sp)
                                Text("₹${String.format("%.2f", signal.entryPrice)}", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("STOP LOSS", color = colors.textMuted, fontSize = 13.sp)
                                Text("₹${String.format("%.2f", signal.stopLoss)}", color = colors.bearish, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("TARGET 1", color = colors.textMuted, fontSize = 13.sp)
                                Text("₹${String.format("%.2f", signal.target1)}", color = colors.bullish, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = colors.border, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Quantity (1 Lot):", color = colors.textSecondary, fontSize = 15.sp)
                            Text("${signal.lotSize} Shares", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Capital Required:", color = colors.textSecondary, fontSize = 15.sp)
                            Text("₹${String.format("%.2f", signal.capitalRequired)}", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Taxes & Charges:", color = colors.textSecondary, fontSize = 15.sp)
                            Text("₹${String.format("%.2f", signal.estimatedCharges)}", color = colors.sideways, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Risk acknowledgment checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { acknowledgedRisk = !acknowledgedRisk }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = acknowledgedRisk,
                        onCheckedChange = { acknowledgedRisk = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.bullish,
                            uncheckedColor = colors.textMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I acknowledge this is an intraday options order. Trading options carries risk of total capital loss.",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Text("CANCEL", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            executeOrderIntent(context, broker)
                            onDismiss()
                        },
                        enabled = acknowledgedRisk,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.bullish,
                            disabledContainerColor = colors.cardElevated
                        )
                    ) {
                        Text(
                            text = "EXECUTE IN ${broker.displayName.uppercase()}",
                            color = if (acknowledgedRisk) colors.background else colors.textMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun executeOrderIntent(context: Context, broker: BrokerType) {
    try {
        val (packageName, webUrl) = when (broker) {
            BrokerType.GROWW -> Pair("com.nextbillion.groww", "https://groww.in/options-trading")
            BrokerType.ANGEL_ONE -> Pair("com.msf.angelmobile", "https://trade.angelone.in/")
            BrokerType.ZERODHA -> Pair("com.zerodha.kite3", "https://kite.zerodha.com/")
            BrokerType.NONE_STANDALONE -> Pair("com.nextbillion.groww", "https://groww.in/options-trading")
        }

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
            context.startActivity(browserIntent)
        }
    } catch (e: Exception) {
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://groww.in/options-trading"))
        context.startActivity(fallback)
    }
}
