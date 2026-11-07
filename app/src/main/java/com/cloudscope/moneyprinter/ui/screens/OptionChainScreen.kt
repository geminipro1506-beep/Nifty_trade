package com.cloudscope.moneyprinter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.data.model.OptionChain
import com.cloudscope.moneyprinter.data.model.OptionContract
import com.cloudscope.moneyprinter.data.model.StrikeRow
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import com.cloudscope.moneyprinter.ui.viewmodel.MainUiState
import com.cloudscope.moneyprinter.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionChainScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val optionChain = uiState.optionChain
    val searchQuery = uiState.optionChainSearchQuery
    val selectedExpiry = uiState.selectedExpiryDate

    val indianFormat = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    var expandedStrike by remember { mutableStateOf<Double?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ==========================================
        // 1. TOP OVERVIEW SUMMARY CARD
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OPTION CHAIN INTELLIGENCE",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    val pcr = optionChain?.pcr ?: 1.05
                    val pcrColor = if (pcr >= 1.0) colors.bullish else colors.bearish
                    val pcrSentiment = if (pcr >= 1.0) "BULLISH BIAS" else "BEARISH BIAS"

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (pcr >= 1.0) colors.bullishBg else colors.bearishBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PCR ${String.format("%.2f", pcr)} • $pcrSentiment",
                            color = pcrColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryMetric(label = "ATM STRIKE", value = optionChain?.let { "${it.atmStrike.toInt()}" } ?: "--", valueColor = colors.textPrimary)
                    SummaryMetric(label = "MAX PAIN", value = optionChain?.let { "₹${it.maxPainStrike.toInt()}" } ?: "--", valueColor = colors.sideways)
                    SummaryMetric(label = "CALL RESISTANCE", value = optionChain?.let { "${it.getEffectiveCallResistanceStrike().toInt()} CE" } ?: "--", valueColor = colors.bearish)
                    SummaryMetric(label = "PUT SUPPORT", value = optionChain?.let { "${it.getEffectivePutSupportStrike().toInt()} PE" } ?: "--", valueColor = colors.bullish)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ==========================================
        // 2. EXPIRY SELECTION CHIPS
        // ==========================================
        val expiries = optionChain?.expiryDates?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            ?: com.cloudscope.moneyprinter.domain.options.ExpiryCalculator.calculateUpcomingExpiries().allExpiries
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            expiries.forEach { exp ->
                val isSelected = exp == selectedExpiry
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedExpiry(exp) },
                    label = {
                        Text(
                            text = exp,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.bullish,
                        selectedLabelColor = colors.background,
                        containerColor = colors.cardElevated,
                        labelColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) colors.bullish else colors.border,
                        selectedBorderColor = colors.bullish,
                        enabled = true,
                        selected = isSelected
                    ),
                    modifier = Modifier.height(44.dp) // Touch target >= 44dp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ==========================================
        // 3. SEARCHABLE STRIKE PRICES
        // ==========================================
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setOptionChainSearch(it) },
            placeholder = {
                Text(
                    text = "Search strike price (e.g. 24800, 25000)...",
                    color = colors.textMuted,
                    fontSize = 15.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.textSecondary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.setOptionChainSearch("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = colors.textSecondary)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp), // Touch target > 48dp
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.card,
                unfocusedContainerColor = colors.card,
                focusedBorderColor = colors.bullish,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ==========================================
        // 4. OPTION CHAIN TABLE HEADERS
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.cardElevated)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CALLS (LTP / OI)",
                color = colors.bullish,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.3f)
            )

            Text(
                text = "STRIKE",
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Text(
                text = "PUTS (LTP / OI)",
                color = colors.bearish,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.3f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ==========================================
        // 5. STRIKE ROWS LIST
        // ==========================================
        val allRows = optionChain?.rows ?: emptyList()
        val filteredRows = if (searchQuery.isBlank()) {
            allRows
        } else {
            allRows.filter { it.strikePrice.toInt().toString().contains(searchQuery.trim()) }
        }

        val atmStrike = optionChain?.atmStrike ?: 24850.0

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredRows, key = { it.strikePrice }) { row ->
                val isAtm = row.strikePrice == atmStrike
                val isCallItm = row.strikePrice < atmStrike
                val isPutItm = row.strikePrice > atmStrike
                val isExpanded = expandedStrike == row.strikePrice

                StrikeRowCard(
                    row = row,
                    isAtm = isAtm,
                    isCallItm = isCallItm,
                    isPutItm = isPutItm,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedStrike = if (isExpanded) null else row.strikePrice
                    }
                )
            }
        }
    }
}

@Composable
private fun StrikeRowCard(
    row: StrikeRow,
    isAtm: Boolean,
    isCallItm: Boolean,
    isPutItm: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val colors = LocalTerminalColors.current
    val call = row.callContract
    val put = row.putContract

    val borderModifier = if (isAtm) {
        Modifier.border(2.dp, colors.accentBlue, RoundedCornerShape(12.dp))
    } else {
        Modifier.border(1.dp, colors.border, RoundedCornerShape(12.dp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .then(borderModifier)
            .clickable { onToggleExpand() }
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call Side
                val callBg = if (isCallItm) colors.itmCallTint else Color.Transparent
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(callBg)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = call?.let { "₹${String.format("%.2f", it.lastPrice)}" } ?: "--",
                                color = colors.textPrimary,
                                fontSize = 17.sp, // Requirement: >= 16px body/price
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            if (isCallItm) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.bullishBg)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("ITM", color = colors.bullish, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = call?.let { "OI: ${formatOiInLakhs(it.openInterest)} • Sp: ₹${String.format("%.2f", it.askPrice - it.bidPrice)}" } ?: "--",
                            color = colors.textSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                }

                // Center Strike
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${row.strikePrice.toInt()}",
                            color = if (isAtm) colors.accentBlue else colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isAtm) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.accentBlue)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ATM", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Put Side
                val putBg = if (isPutItm) colors.itmPutTint else Color.Transparent
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(putBg)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isPutItm) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.bearishBg)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("ITM", color = colors.bearish, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = put?.let { "₹${String.format("%.2f", it.lastPrice)}" } ?: "--",
                                color = colors.textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = put?.let { "OI: ${formatOiInLakhs(it.openInterest)} • Sp: ₹${String.format("%.2f", it.askPrice - it.bidPrice)}" } ?: "--",
                            color = colors.textSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }

            // Expanded Greeks & Details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CALL GREEKS & VOLUME", color = colors.bullish, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Delta: ${call?.let { String.format("%.2f", it.delta) } ?: "--"}", color = colors.textSecondary, fontSize = 13.sp)
                            Text("IV: ${call?.let { String.format("%.1f%%", it.impliedVolatility) } ?: "--"}", color = colors.textSecondary, fontSize = 13.sp)
                            Text("Volume: ${call?.let { formatOiInLakhs(it.volume) } ?: "--"}", color = colors.textSecondary, fontSize = 13.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("PUT GREEKS & VOLUME", color = colors.bearish, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Delta: ${put?.let { String.format("%.2f", it.delta) } ?: "--"}", color = colors.textSecondary, fontSize = 13.sp)
                            Text("IV: ${put?.let { String.format("%.1f%%", it.impliedVolatility) } ?: "--"}", color = colors.textSecondary, fontSize = 13.sp)
                            Text("Volume: ${put?.let { formatOiInLakhs(it.volume) } ?: "--"}", color = colors.textSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, valueColor: Color) {
    val colors = LocalTerminalColors.current
    Column {
        Text(text = label, color = colors.textMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatOiInLakhs(oi: Long): String {
    val inLakhs = oi / 100000.0
    return String.format("%.1fL", inLakhs)
}
