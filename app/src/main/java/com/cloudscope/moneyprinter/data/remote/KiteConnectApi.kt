package com.cloudscope.moneyprinter.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface KiteConnectApi {

    @GET("quote")
    suspend fun getQuotes(
        @Header("Authorization") authHeader: String,
        @Header("X-Kite-Version") version: String = "3",
        @Query("i", encoded = true) instruments: List<String>
    ): Response<KiteEnvelope<Map<String, KiteQuote>>>

    @GET("instruments/historical/{instrument_token}/{interval}")
    suspend fun getHistoricalCandles(
        @Header("Authorization") authHeader: String,
        @Header("X-Kite-Version") version: String = "3",
        @Path("instrument_token") instrumentToken: Long,
        @Path("interval") interval: String,
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<KiteEnvelope<KiteHistoricalData>>

    @GET("user/profile")
    suspend fun getProfile(
        @Header("Authorization") authHeader: String,
        @Header("X-Kite-Version") version: String = "3"
    ): Response<KiteEnvelope<Map<String, Any>>>
}
