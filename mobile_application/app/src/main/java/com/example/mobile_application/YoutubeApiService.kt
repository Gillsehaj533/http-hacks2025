package com.example.mobile_application

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class DownloadRequest(val url: String)

data class DownloadResponse(
    val status: String,
    val download_url: String,
    val title: String,
    val duration: Int,
    val thumbnail: String
)

interface YoutubeApiService {
    @POST("/download")
    fun downloadAudio(@Body request: DownloadRequest): Call<DownloadResponse>
}
