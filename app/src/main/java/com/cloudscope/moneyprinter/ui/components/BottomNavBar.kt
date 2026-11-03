package com.cloudscope.moneyprinter.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors
import com.cloudscope.moneyprinter.ui.viewmodel.TerminalNavTab

// High-visibility mint/emerald financial accent color
private val MintEmeraldAccent = Color(0xFF00E676)
private val MintEmeraldBg = Color(0x1F00E676)

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
        shadowElevation = 16.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Subtle top demarcation line
            HorizontalDivider(thickness = 0.5.dp, color = colors.border)

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp) // Centered on large screens / tablets
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                val isNarrow = maxWidth < 380.dp
                val iconSize = if (isNarrow) 22.dp else 24.dp
                val labelSize = if (isNarrow) 9.5.sp else 11.sp

                val tabs = remember {
                    listOf(
                        Triple(TerminalNavTab.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
                        Triple(TerminalNavTab.SIGNALS, Icons.Default.Bolt, "Signals"),
                        Triple(TerminalNavTab.OPTION_CHAIN, Icons.Default.FormatListNumbered, "Chain"),
                        Triple(TerminalNavTab.PERFORMANCE, Icons.Default.Assessment, "Accuracy"),
                        Triple(TerminalNavTab.ANALYSIS, Icons.Default.Analytics, "Analysis"),
                        Triple(TerminalNavTab.SETTINGS, Icons.Default.Tune, "Settings")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { (tab, icon, label) ->
                        val isSelected = selectedTab == tab

                        val animatedItemColor by animateColorAsState(
                            targetValue = if (isSelected) MintEmeraldAccent else colors.textSecondary.copy(alpha = 0.85f),
                            animationSpec = tween(durationMillis = 200),
                            label = "nav_item_color"
                        )
                        val animatedBgColor by animateColorAsState(
                            targetValue = if (isSelected) MintEmeraldBg else Color.Transparent,
                            animationSpec = tween(durationMillis = 200),
                            label = "nav_bg_color"
                        )
                        val animatedScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1.0f,
                            animationSpec = tween(durationMillis = 200),
                            label = "nav_scale"
                        )

                        val interactionSource = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = rememberRipple(bounded = true, color = MintEmeraldAccent),
                                    role = Role.Tab,
                                    onClick = {
                                        if (selectedTab != tab) {
                                            onTabSelected(tab)
                                        }
                                    }
                                )
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp)
                                    .scale(animatedScale),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Top active tab indicator bar
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .height(2.5.dp)
                                            .clip(RoundedCornerShape(1.5.dp))
                                            .background(MintEmeraldAccent)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                } else {
                                    Spacer(modifier = Modifier.height(4.5.dp))
                                }

                                // Icon with animated color
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = animatedItemColor,
                                    modifier = Modifier.size(iconSize)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Tab label with animated typography
                                Text(
                                    text = label,
                                    color = animatedItemColor,
                                    fontSize = labelSize,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
