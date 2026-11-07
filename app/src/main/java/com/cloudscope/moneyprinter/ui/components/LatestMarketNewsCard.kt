package com.cloudscope.moneyprinter.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudscope.moneyprinter.domain.news.MarketNewsArticle
import com.cloudscope.moneyprinter.domain.news.NewsImpactLevel
import com.cloudscope.moneyprinter.domain.news.NewsSentiment
import com.cloudscope.moneyprinter.ui.theme.LocalTerminalColors

@Composable
fun LatestMarketNewsCard(
    newsArticles: List<MarketNewsArticle>,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Newspaper,
                        contentDescription = "News",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LATEST MARKET NEWS",
                        color = colors.textMuted,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surface)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "SENTIMENT INTELLIGENCE",
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (newsArticles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Scanning authenticated financial news feeds...",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                val displayList = newsArticles.take(7)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    displayList.forEach { article ->
                        NewsItemRow(article = article, onArticleClick = {
                            try {
                                if (article.url.isNotBlank() && article.url.startsWith("http")) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                }
                            } catch (_: Exception) {
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsItemRow(
    article: MarketNewsArticle,
    onArticleClick: () -> Unit
) {
    val colors = LocalTerminalColors.current

    val (sentimentColor, sentimentBg) = when (article.sentiment) {
        NewsSentiment.POSITIVE -> Pair(colors.bullish, colors.bullishBg)
        NewsSentiment.NEGATIVE -> Pair(colors.bearish, colors.bearishBg)
        NewsSentiment.NEUTRAL -> Pair(colors.textSecondary, colors.surface)
    }

    val impactColor = when (article.impact) {
        NewsImpactLevel.HIGH -> Color(0xFFFF5252)
        NewsImpactLevel.MEDIUM -> Color(0xFFFFB300)
        NewsImpactLevel.LOW -> colors.textMuted
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onArticleClick() }
            .padding(12.dp)
    ) {
        Column {
            // Top metadata row: Source, Publication Time (IST), Impact badge, Open icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.source,
                        color = colors.textMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )

                    Text(
                        text = " • ${article.publishedTimeIst}",
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Impact Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(impactColor.copy(alpha = 0.15f))
                            .border(1.dp, impactColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = article.impact.displayName,
                            color = impactColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        tint = colors.textMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Headline with mandatory sentiment label prefix (🟢 +VE | / 🔴 -VE | / ⚪ NEUTRAL | )
            Text(
                text = article.displayHeadline,
                color = colors.textPrimary,
                fontSize = 13.5.sp,
                lineHeight = 18.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
