package com.cloudscope.moneyprinter.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

data class AngelProfileResponse(
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: AngelUserData? = null
)

data class AngelUserData(
    @SerializedName("clientcode") val clientCode: String? = null,
    @SerializedName("name") val name: String? = null
)

interface AngelOneApi {
    @GET("rest/secure/angelbroking/user/v1/getProfile")
    suspend fun getProfile(
        @Header("Authorization") bearerToken: String,
        @Header("X-PrivateKey") apiKey: String
    ): Response<AngelProfileResponse>
}
