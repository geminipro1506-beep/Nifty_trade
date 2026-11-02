package com.cloudscope.moneyprinter.data.remote

import com.google.gson.annotations.SerializedName

data class YahooChartResponse(
    @SerializedName("chart") val chart: YahooChartData? = null
)

data class YahooChartData(
    @SerializedName("result") val result: List<YahooChartResult>? = null,
    @SerializedName("error") val error: YahooError? = null
)

data class YahooError(
    @SerializedName("code") val code: String? = null,
    @SerializedName("description") val description: String? = null
)

data class YahooChartResult(
    @SerializedName("meta") val meta: YahooChartMeta? = null,
    @SerializedName("timestamp") val timestamp: List<Long>? = null,
    @SerializedName("indicators") val indicators: YahooIndicatorsWrapper? = null
)

data class YahooChartMeta(
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("symbol") val symbol: String? = null,
    @SerializedName("regularMarketPrice") val regularMarketPrice: Double? = null,
    @SerializedName("regularMarketDayHigh") val regularMarketDayHigh: Double? = null,
    @SerializedName("regularMarketDayLow") val regularMarketDayLow: Double? = null,
    @SerializedName("chartPreviousClose") val chartPreviousClose: Double? = null,
    @SerializedName("previousClose") val previousClose: Double? = null,
    @SerializedName("regularMarketVolume") val regularMarketVolume: Long? = null,
    @SerializedName("regularMarketTime") val regularMarketTime: Long? = null
)

data class YahooIndicatorsWrapper(
    @SerializedName("quote") val quote: List<YahooQuoteList>? = null
)

data class YahooQuoteList(
    @SerializedName("open") val open: List<Double?>? = null,
    @SerializedName("high") val high: List<Double?>? = null,
    @SerializedName("low") val low: List<Double?>? = null,
    @SerializedName("close") val close: List<Double?>? = null,
    @SerializedName("volume") val volume: List<Long?>? = null
)
