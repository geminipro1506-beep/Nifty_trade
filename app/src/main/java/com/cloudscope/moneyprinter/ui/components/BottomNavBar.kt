package com.cloudscope.moneyprinter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import com.cloudscope.moneyprinter.ui.viewmodel.TerminalNavTab

@Composable
fun BottomNavBar(
    selectedTab: TerminalNavTab,
    onTabSelected: (TerminalNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current

    Surface(
        color = colors.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple(TerminalNavTab.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
                Triple(TerminalNavTab.SIGNALS, Icons.Default.Bolt, "Signals"),
                Triple(TerminalNavTab.OPTION_CHAIN, Icons.Default.FormatListNumbered, "Chain"),
                Triple(TerminalNavTab.PERFORMANCE, Icons.Default.Assessment, "Accuracy"),
                Triple(TerminalNavTab.ANALYSIS, Icons.Default.Analytics, "Analysis"),
                Triple(TerminalNavTab.SETTINGS, Icons.Default.Tune, "Settings")
            )

            tabs.forEach { (tab, icon, label) ->
                val isSelected = selectedTab == tab
                val itemColor = if (isSelected) colors.bullish else colors.textSecondary
                val bgColor = if (isSelected) colors.bullishBg else colors.surface

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = itemColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            color = itemColor,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
