package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class Web3FormsResponse(
    val success: Boolean,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class Web3FormsRequest(
    @Json(name = "access_key") val accessKey: String,
    val name: String,
    val email: String,
    val message: String,
    val subject: String = "TaskFlow Pro Developer Support Form"
)

interface Web3FormsService {
    @retrofit2.http.Headers(
        "Content-Type: application/json",
        "Accept: application/json",
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Origin: https://web3forms.com",
        "Referer: https://web3forms.com/"
    )
    @POST("submit")
    suspend fun submitForm(
        @Body request: Web3FormsRequest
    ): Response<Web3FormsResponse>
}

object Web3FormsClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.web3forms.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val service: Web3FormsService = retrofit.create(Web3FormsService::class.java)
}
