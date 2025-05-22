package com.lloppy.neironki_5sem.data

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("/predict_from_file")
    suspend fun predictFromFile(
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @Multipart
    @POST("/predict_from_file")
    suspend fun predictFromNpy(
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    companion object {
        private const val BASE_URL = "https://4915-34-19-48-65.ngrok-free.app"
        fun create(): ApiService {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}