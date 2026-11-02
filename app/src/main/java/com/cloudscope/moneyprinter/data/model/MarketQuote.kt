package com.cloudscope.moneyprinter.data.model

data class MarketQuote(
    val symbol: String = "NIFTY 50",
    val lastPrice: Double,
    val change: Double,
    val pChange: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val vwap: Double,
    val volume: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class IndiaVixQuote(
    val lastPrice: Double,
    val change: Double,
    val pChange: Double,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OptionType {
    CE, PE
}

data class OptionContract(
    val symbol: String,
    val strikePrice: Double,
    val optionType: OptionType,
    val expiryDate: String,
    val lastPrice: Double,
    val change: Double,
    val pChange: Double,
    val openInterest: Long,
    val changeInOi: Long,
    val volume: Long,
    val impliedVolatility: Double,
    val delta: Double,
    val gamma: Double,
    val theta: Double,
    val vega: Double,
    val bidPrice: Double,
    val askPrice: Double,
    val bidQty: Int,
    val askQty: Int
)

data class StrikeRow(
    val strikePrice: Double,
    val callContract: OptionContract?,
    val putContract: OptionContract?
)

data class OptionChain(
    val spotPrice: Double,
    val atmStrike: Double,
    val pcr: Double,
    val maxPainStrike: Double,
    val totalCallOi: Long,
    val totalPutOi: Long,
    val totalCallOiChange: Long,
    val totalPutOiChange: Long,
    val rows: List<StrikeRow>,
    val timestamp: Long = System.currentTimeMillis()
)
