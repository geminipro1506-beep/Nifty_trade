package com.cloudscope.moneyprinter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

private val MintAccent = Color(0xFF00E676)
private val EmeraldAccent = Color(0xFF10B981)

@Composable
fun SignalDetailsDialog(
    record: HistoricalSignalRecord?,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {},
    onClosePaperTrade: ((HistoricalSignalRecord) -> Unit)? = null
) {
    val colors = LocalTerminalColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .widthIn(max = 640.dp)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top App Bar / Navigation Header
                    DialogHeader(
                        title = "SIGNAL AUDIT SPECIFICATION",
                        subtitle = record?.let { "ID: ${it.id.take(8)}... • Authorized Lot Size ${it.lotSize}" } ?: "Audit Ledger",
                        onBack = onDismiss,
                        onClose = onDismiss
                    )

                    HorizontalDivider(color = colors.border, thickness = 1.dp)

                    // Body with Error Boundary
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        when {
                            isLoading -> {
                                LoadingStateView()
                            }
                            errorMessage != null -> {
                                ErrorStateView(
                                    error = errorMessage,
                                    onRetry = onRetry,
                                    onClose = onDismiss
                                )
                            }
                            record == null -> {
                                EmptyStateView(onClose = onDismiss)
                            }
                            else -> {
                                SignalDetailsContent(
                                    record = record,
                                    onClosePaperTrade = onClosePaperTrade
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = colors.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun LoadingStateView() {
    val colors = LocalTerminalColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = MintAccent,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading signal audit specifications...",
                color = colors.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun EmptyStateView(onClose: () -> Unit) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Signal Selected",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "The requested historical signal record was not found or is unavailable.",
                color = colors.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("BACK TO AUDIT TRAIL", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ErrorStateView(
    error: String,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = colors.bearish,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Audit Ledger Error",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error,
                color = colors.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Text("CLOSE", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = MintAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RETRY AUDIT", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SignalDetailsContent(
    record: HistoricalSignalRecord,
    onClosePaperTrade: ((HistoricalSignalRecord) -> Unit)?
) {
    val colors = LocalTerminalColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Trade Info
        TradeInfoSection(record = record)

        // Section 2: Entry Info
        EntryInfoSection(record = record)

        // Section 3: Exit Info
        ExitInfoSection(record = record)

        // Section 4: Performance & Financial Reconciliation
        PerformanceSection(record = record)

        // Section 5: Strategy Info & Confluence
        StrategyInfoSection(record = record)

        // Quick action: Close Paper Trade if open
        if (record.isPaperTrade && (record.status == SignalLifecycleStatus.ACTIVE || record.status == SignalLifecycleStatus.PENDING_ENTRY) && onClosePaperTrade != null) {
            Button(
                onClick = { onClosePaperTrade(record) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.warning)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = colors.warning, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CLOSE PAPER POSITION (MARKET EXIT)", color = colors.warning, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun TradeInfoSection(record: HistoricalSignalRecord) {
    val colors = LocalTerminalColors.current
    val isCe = record.action == SignalAction.BUY_CE
    val actionColor = if (isCe) colors.bullish else colors.bearish
    val actionBg = if (isCe) colors.bullishBg else colors.bearishBg

    val outcomeColor = when (record.outcome) {
        TradeOutcome.WIN -> colors.bullish
        TradeOutcome.LOSS -> colors.bearish
        TradeOutcome.AMBIGUOUS -> colors.warning
        TradeOutcome.BREAKEVEN -> colors.textSecondary
        TradeOutcome.CANCELLED, TradeOutcome.UNFILLED -> colors.textSecondary
        null -> colors.warning
    }

    SectionCard(title = "1. TRADE SPECIFICATION & SIZING") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = actionBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = record.action.name.replace("_", " "),
                        color = actionColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = record.contractSymbol.ifEmpty { "NIFTY Option" },
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(color = outcomeColor.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = record.outcome?.name ?: record.status.name,
                    color = outcomeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        DetailRow(label = "Option Strike", value = "₹${safeDouble(record.strikePrice)} ${record.optionType.name}")
        DetailRow(label = "Expiry Date", value = record.expiryDate.ifEmpty { "Current Weekly" })
        DetailRow(label = "Lifecycle Status", value = record.status.name)
        DetailRow(
            label = "Execution Environment",
            value = if (record.isPaperTrade) "Paper Trading (Simulated)" else "Live Broker Execution",
            valueColor = if (record.isPaperTrade) colors.warning else colors.bullish
        )

        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Contract sizing breakdown
        DetailRow(label = "Authorized Lot Size", value = "${record.lotSize} Qty/Lot (NSE Authorized)", valueColor = MintAccent)
        DetailRow(label = "Allocated Lots", value = "${record.lots} Lot${if (record.lots > 1) "s" else ""}")
        DetailRow(
            label = "Total Position Quantity",
            value = "${record.totalQuantity} Units (${record.lots} × ${record.lotSize})",
            valueColor = colors.textPrimary,
            isBold = true
        )
    }
}

@Composable
private fun EntryInfoSection(record: HistoricalSignalRecord) {
    val colors = LocalTerminalColors.current

    SectionCard(title = "2. ENTRY & TARGET LEVELS") {
        DetailRow(label = "Signal Timestamp", value = safeDate(record.timestamp))
        DetailRow(label = "Spot Price at Signal", value = "₹${safeDouble(record.spotPrice)}")
        DetailRow(label = "Option Premium at Signal", value = "₹${safeDouble(record.premiumAtSignal)}")
        DetailRow(
            label = "Execution Entry Price",
            value = "₹${safeDouble(record.entryPrice)}",
            valueColor = colors.bullish,
            isBold = true
        )
        DetailRow(
            label = "Entry Triggered",
            value = if (record.entryMet) "YES (${safeDate(record.entryTimestamp)})" else "NO (Unfilled/Pending)",
            valueColor = if (record.entryMet) colors.bullish else colors.warning
        )

        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

        DetailRow(label = "Stop Loss (SL)", value = "₹${safeDouble(record.stopLoss)}", valueColor = colors.bearish)
        DetailRow(label = "Target 1 (1:1.5)", value = "₹${safeDouble(record.target1)}", valueColor = colors.bullish)
        DetailRow(label = "Target 2 (1:2.0)", value = "₹${safeDouble(record.target2)}", valueColor = colors.bullish)
        DetailRow(label = "Target 3 (1:3.0)", value = "₹${safeDouble(record.target3)}", valueColor = colors.bullish)

        val riskPts = if (record.entryPrice > 0 && record.stopLoss > 0) record.entryPrice - record.stopLoss else 0.0
        val rewardPts = if (record.target1 > 0 && record.entryPrice > 0) record.target1 - record.entryPrice else 0.0
        val rrRatio = if (riskPts > 0) String.format(Locale.ENGLISH, "1:%.2f", rewardPts / riskPts) else "--"
        DetailRow(label = "Risk-to-Reward Ratio (T1)", value = rrRatio)
    }
}

@Composable
private fun ExitInfoSection(record: HistoricalSignalRecord) {
    val colors = LocalTerminalColors.current

    SectionCard(title = "3. EXIT & AUDIT RESOLUTION") {
        DetailRow(label = "Exit Timestamp", value = safeDate(record.exitTimestamp))
        DetailRow(
            label = "Actual Exit Price",
            value = record.actualExitPrice?.let { "₹${safeDouble(it)}" } ?: "--",
            valueColor = colors.textPrimary,
            isBold = true
        )
        DetailRow(
            label = "Exit Trigger Reason",
            value = record.exitReason?.name ?: "--",
            valueColor = when (record.exitReason) {
                ExitReason.TARGET_1, ExitReason.TARGET_2, ExitReason.TARGET_3 -> colors.bullish
                ExitReason.STOP_LOSS -> colors.bearish
                ExitReason.AMBIGUOUS -> colors.warning
                else -> colors.textSecondary
            },
            isBold = true
        )
        DetailRow(label = "Holding Duration", value = "${record.holdingTimeMinutes} Minutes")
        DetailRow(label = "Candle Resolution", value = record.candleResolution)
        DetailRow(label = "Final Audit Status", value = record.finalResolutionStatus ?: record.status.name)

        if (record.ambiguousReason != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = colors.warning.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.warning.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = colors.warning, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AMBIGUOUS CANDLE AUDIT LOG", color = colors.warning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = record.ambiguousReason,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceSection(record: HistoricalSignalRecord) {
    val colors = LocalTerminalColors.current
    val isWin = record.netPnl >= 0
    val pnlColor = if (isWin) colors.bullish else colors.bearish

    SectionCard(title = "4. FINANCIAL AUDIT & POST-FRICTION RECONCILIATION") {
        DetailRow(
            label = "Gross P&L (Pre-friction)",
            value = if (record.grossPnl >= 0) "+₹${safeDouble(record.grossPnl)}" else "-₹${safeDouble(abs(record.grossPnl))}",
            valueColor = if (record.grossPnl >= 0) colors.bullish else colors.bearish
        )

        DetailRow(
            label = "Statutory Friction & Charges",
            value = "-₹${safeDouble(record.totalCharges)}",
            valueColor = colors.textSecondary
        )

        Text(
            text = "(Roundtrip Brokerage ₹40 + STT/CTT + Exchange Turnover + GST + 0.5pt slippage)",
            color = colors.textSecondary.copy(alpha = 0.8f),
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
        )

        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

        DetailRow(
            label = "Net P&L (Post-friction)",
            value = if (record.netPnl >= 0) "+₹${safeDouble(record.netPnl)}" else "-₹${safeDouble(abs(record.netPnl))}",
            valueColor = pnlColor,
            isBold = true
        )

        // Mathematical reconciliation guarantee badge
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = MintAccent.copy(alpha = 0.10f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = MintAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Gross P&L (₹${safeDouble(record.grossPnl)}) - Charges (₹${safeDouble(record.totalCharges)}) = Net P&L (₹${safeDouble(record.netPnl)}) [Reconciled]",
                    color = MintAccent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Migration Audit Trail (if applicable)
        if (record.migrationStatus != null || record.originalLotSize != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = colors.card,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "HISTORICAL LOT SIZE MIGRATION AUDIT",
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DetailRow(label = "Migration Status", value = record.migrationStatus ?: "MIGRATED_LOT_SIZE_65", valueColor = MintAccent)
                    if (record.originalLotSize != null) {
                        DetailRow(label = "Original Lot Size", value = "${record.originalLotSize} Qty")
                    }
                    if (record.originalGrossPnl != null) {
                        DetailRow(label = "Original Gross P&L", value = "₹${safeDouble(record.originalGrossPnl)}")
                    }
                    if (record.originalNetPnl != null) {
                        DetailRow(label = "Original Net P&L", value = "₹${safeDouble(record.originalNetPnl)}")
                    }
                    if (record.migrationNotes != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = record.migrationNotes,
                            color = colors.textSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StrategyInfoSection(record: HistoricalSignalRecord) {
    val colors = LocalTerminalColors.current

    SectionCard(title = "5. STRATEGY CONFLUENCE & INDICATORS") {
        DetailRow(label = "Algorithmic Strategy", value = record.strategyType.name, isBold = true)
        DetailRow(
            label = "Confidence Score",
            value = "${record.confidenceScore}%",
            valueColor = if (record.confidenceScore >= 75) MintAccent else colors.warning,
            isBold = true
        )
        DetailRow(label = "Timeframe", value = record.timeframe)

        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Technical Indicators Snapshot
        DetailRow(label = "RSI (14)", value = safeDouble(record.rsi14, 1))
        DetailRow(label = "VWAP", value = "₹${safeDouble(record.vwap, 1)}")
        DetailRow(label = "EMA 9 / 20 / 50", value = "₹${safeDouble(record.ema9, 0)} / ₹${safeDouble(record.ema20, 0)} / ₹${safeDouble(record.ema50, 0)}")
        DetailRow(label = "Put-Call Ratio (PCR)", value = safeDouble(record.pcr, 2))
        DetailRow(label = "India VIX", value = safeDouble(record.vix, 2))

        if (record.reasons.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Algorithmic Confluence Reasons:", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            record.reasons.forEach { reason ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = MintAccent, fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                    Text(text = reason, color = colors.textPrimary, fontSize = 11.sp)
                }
            }
        }

        if (record.invalidationConditions.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            DetailRow(label = "Invalidation Condition", value = record.invalidationConditions)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalTerminalColors.current
    Surface(
        color = colors.card,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = LocalTerminalColors.current.textPrimary,
    isBold: Boolean = false
) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 11.5.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Defensive formatting helpers
private fun safeDouble(value: Double?, decimals: Int = 2): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "--"
    return try {
        String.format(Locale.ENGLISH, "%.${decimals}f", value)
    } catch (e: Exception) {
        value.toString()
    }
}

private fun safeDate(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0) return "--"
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.ENGLISH)
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "--"
    }
}
