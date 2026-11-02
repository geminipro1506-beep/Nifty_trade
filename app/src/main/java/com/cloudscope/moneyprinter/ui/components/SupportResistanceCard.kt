package com.cloudscope.moneyprinter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.data.model.SupportResistanceLevels
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors

@Composable
fun SupportResistanceCard(
    levels: SupportResistanceLevels?,
    currentSpot: Double?,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SUPPORT & RESISTANCE LEVELS",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LevelItem(label = "R2", value = levels?.let { "₹${it.r2.toInt()}" } ?: "--", color = colors.bearish)
                LevelItem(label = "R1", value = levels?.let { "₹${it.r1.toInt()}" } ?: "--", color = colors.bearish)
                LevelItem(label = "PIVOT", value = levels?.let { "₹${it.pivot.toInt()}" } ?: "--", color = colors.textPrimary)
                LevelItem(label = "S1", value = levels?.let { "₹${it.s1.toInt()}" } ?: "--", color = colors.bullish)
                LevelItem(label = "S2", value = levels?.let { "₹${it.s2.toInt()}" } ?: "--", color = colors.bullish)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: PDH and PDL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = levels?.let { "PDH: ₹${it.pdh.toInt()}" } ?: "PDH: --",
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = levels?.let { "PDL: ₹${it.pdl.toInt()}" } ?: "PDL: --",
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun LevelItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    val colors = LocalTerminalColors.current
    Column {
        Text(text = label, color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
