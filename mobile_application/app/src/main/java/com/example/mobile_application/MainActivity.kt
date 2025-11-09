package com.example.mobile_application


import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.mobile_application.ui.theme.Mobile_applicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicListScreen()
        }
    }
}

@Composable
fun MusicListScreen() {
    val context = LocalContext.current
    val permission = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            Manifest.permission.READ_MEDIA_AUDIO
        }
        else -> {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            isGranted -> hasPermission = isGranted
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (hasPermission) {

            val audioFiles = remember { mutableStateListOf<AudioDetails>() }

            LaunchedEffect(Unit) {
                audioFiles.clear()
                audioFiles.addAll(loadAudioFiles(context))
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(audioFiles) { audioFile ->
                    ShowAudioItem(audioFile)
                }
            }
        } else {
            Button(onClick = { launcher.launch(permission) }) {
                Text("Grant Permission to Access Music")
            }
        }
    }
}

data class AudioDetails(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri
)

fun loadAudioFiles(context: Context): List<AudioDetails> {

    val songs = mutableListOf<AudioDetails>()

    val collection = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else ->
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST
    )

    val sortedOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    context.contentResolver.query(
        collection,
        projection,
        null,
        null,
        sortedOrder
    )?. use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val title = cursor.getString(titleColumn) ?: "Unknown Title"
            val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
            )
            songs.add(AudioDetails(id, title, artist, uri))
        }
    }

    return songs
}

@Composable
fun ShowAudioItem(audioDetails: AudioDetails) {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.music_note),
            contentDescription = "Music Note",
            modifier = Modifier
        )
        Column (
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(audioDetails.title)
            Text(audioDetails.artist)
        }
    }
}
