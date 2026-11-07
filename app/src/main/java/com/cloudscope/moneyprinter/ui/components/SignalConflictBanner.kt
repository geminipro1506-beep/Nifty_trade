package com.cloudscope.moneyprinter.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.domain.signal.SignalConflictState
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors

@Composable
fun SignalConflictBanner(
    conflictState: SignalConflictState?,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val hasConflict = conflictState?.hasConflict == true

    AnimatedVisibility(
        visible = hasConflict,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val warningOrange = Color(0xFFFF9100)
        val warningBg = Color(0x22FF9100)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(warningBg)
                .border(1.5.dp, warningOrange, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Signal Conflict Warning",
                    tint = warningOrange,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SIGNAL CONFLICT",
                            color = warningOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(warningOrange.copy(alpha = 0.25f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DO NOT FORCE TRADE",
                                color = warningOrange,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = conflictState?.conflictReason ?: "Normal Technicals and Smart Money flow indicate opposing directions. Standby.",
                        color = colors.textPrimary,
                        fontSize = 12.5.sp,
                        lineHeight = 17.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
