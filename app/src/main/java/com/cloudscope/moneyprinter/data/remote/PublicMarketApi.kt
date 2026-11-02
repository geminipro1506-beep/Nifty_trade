package com.cloudscope.moneyprinter.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface PublicMarketApi {

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "5m",
        @Query("range") range: String = "1d",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Linux; Android 14) CloudscopeMoneyPrinter/1.0"
    ): Response<YahooChartResponse>
}
