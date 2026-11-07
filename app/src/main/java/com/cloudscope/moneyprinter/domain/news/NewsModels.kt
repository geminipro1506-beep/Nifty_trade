package com.cloudscope.moneyprinter.domain.news

enum class NewsSentiment(val label: String, val prefix: String) {
    POSITIVE("+VE", "🟢 +VE | "),
    NEGATIVE("-VE", "🔴 -VE | "),
    NEUTRAL("NEUTRAL", "⚪ NEUTRAL | ")
}

enum class NewsImpactLevel(val displayName: String) {
    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW")
}

data class MarketNewsArticle(
    val id: String,
    val headline: String,
    val summary: String,
    val source: String,
    val url: String,
    val publishedEpochMillis: Long,
    val publishedTimeIst: String,
    val sentiment: NewsSentiment,
    val impact: NewsImpactLevel,
    val isBreaking: Boolean = false
) {
    val displayHeadline: String
        get() = "${sentiment.prefix}$headline"
}
