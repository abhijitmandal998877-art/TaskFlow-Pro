package com.example.network

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class Web3FormsResponse(
    val success: Boolean,
    val message: String?
)

interface Web3FormsService {
    @FormUrlEncoded
    @POST("submit")
    suspend fun submitForm(
        @Field("access_key") accessKey: String,
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("message") message: String,
        @Field("subject") subject: String = "TaskFlow Pro Developer Support Form"
    ): Response<Web3FormsResponse>
}

object Web3FormsClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.web3forms.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val service: Web3FormsService = retrofit.create(Web3FormsService::class.java)
}
