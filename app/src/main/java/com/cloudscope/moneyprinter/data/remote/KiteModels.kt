package com.cloudscope.moneyprinter.data.remote

import com.google.gson.annotations.SerializedName

data class KiteEnvelope<T>(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: T?,
    @SerializedName("message") val message: String?,
    @SerializedName("error_type") val errorType: String?
)

data class KiteOhlc(
    @SerializedName("open") val open: Double = 0.0,
    @SerializedName("high") val high: Double = 0.0,
    @SerializedName("low") val low: Double = 0.0,
    @SerializedName("close") val close: Double = 0.0
)

data class KiteDepthItem(
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("quantity") val quantity: Int = 0,
    @SerializedName("orders") val orders: Int = 0
)

data class KiteMarketDepth(
    @SerializedName("buy") val buy: List<KiteDepthItem> = emptyList(),
    @SerializedName("sell") val sell: List<KiteDepthItem> = emptyList()
)

data class KiteQuote(
    @SerializedName("instrument_token") val instrumentToken: Long = 0L,
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("last_price") val lastPrice: Double = 0.0,
    @SerializedName("net_change") val netChange: Double = 0.0,
    @SerializedName("ohlc") val ohlc: KiteOhlc? = null,
    @SerializedName("volume") val volume: Long = 0L,
    @SerializedName("average_price") val averagePrice: Double = 0.0,
    @SerializedName("oi") val openInterest: Long = 0L,
    @SerializedName("oi_day_high") val oiDayHigh: Long = 0L,
    @SerializedName("oi_day_low") val oiDayLow: Long = 0L,
    @SerializedName("depth") val depth: KiteMarketDepth? = null
)

data class KiteHistoricalData(
    @SerializedName("candles") val candles: List<List<Any>> = emptyList()
)
