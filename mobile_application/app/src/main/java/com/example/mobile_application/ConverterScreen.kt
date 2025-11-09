package com.example.mobile_application

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.telephony.mbms.DownloadRequest
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.text.isNotEmpty

@Composable
fun ConverterScreen() {

    val context = LocalContext.current

    var youtubeUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var songTitle by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }

    // Reset everything when leaving screen (Compose disposal)
    DisposableEffect(Unit) {
        onDispose {
            youtubeUrl = ""
            songTitle = ""
            downloadUrl = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "YouTube → MP3 Converter", fontSize = 22.sp)

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = youtubeUrl,
            onValueChange = { youtubeUrl = it },
            label = { Text("Paste YouTube link") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && songTitle.isEmpty()  // disable input while converting
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                isLoading = true
                songTitle = ""
                downloadUrl = ""

                RetrofitClient.api.downloadAudio(DownloadRequest(youtubeUrl))
                    .enqueue(object : Callback<DownloadResponse> {
                        override fun onResponse(
                            call: Call<DownloadResponse>,
                            response: Response<DownloadResponse>
                        ) {
                            isLoading = false

                            if (!response.isSuccessful) {
                                Log.e("RETROFIT_DEBUG", "❌ HTTP ERROR: ${response.code()}")
                                return
                            }

                            val data = response.body()
                            downloadUrl = data?.download_url ?: ""
                            songTitle = data?.title ?: ""

                            // clears input once Download button appears
                            youtubeUrl = ""
                        }

                        override fun onFailure(call: Call<DownloadResponse>, t: Throwable) {
                            isLoading = false
                            Log.e("RETROFIT_DEBUG", "❌ FAILURE: ${t.message}", t)
                        }
                    })
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = youtubeUrl.isNotEmpty() && !isLoading && songTitle.isEmpty() // disable convert properly
        ) {
            Text(if (isLoading) "Converting..." else "Convert")
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(20.dp))
            CircularProgressIndicator()
        }

        if (songTitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("✅ Converted: $songTitle")

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val request = DownloadManager.Request(Uri.parse(downloadUrl))
                        .setTitle("$songTitle.mp3")
                        .setMimeType("audio/mpeg")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)

                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_MUSIC,
                        "$songTitle.mp3"
                    )

                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(request)

                    // ✅ After download is clicked, clear screen
                    songTitle = ""
                    downloadUrl = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Download MP3")
            }
        }
    }
}
