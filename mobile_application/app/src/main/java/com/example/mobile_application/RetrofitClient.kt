package com.example.mobile_application

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.apply
import kotlin.jvm.java

object RetrofitClient {

    // ⚠️ Use ONE of these depending on where you run:
    // Emulator: "http://10.0.2.2:8000/"
    // Physical device: "http://10.65.99.24:8000/"
//    private const val BASE_URL = "http://10.65.99.24:8000/"

    private const val BASE_URL = "https://httphacks-yt-converter.fly.dev/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)   // time to connect to FastAPI
        .readTimeout(120, TimeUnit.SECONDS)     // time to wait for yt-dlp+ffmpeg to finish
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val api: YoutubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubeApiService::class.java)
    }
}
