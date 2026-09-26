package com.example.fitnessapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessapp.model.*
import java.time.LocalDate
import java.time.YearMonth

import android.net.Uri
import android.widget.VideoView
import android.widget.MediaController
import androidx.compose.ui.viewinterop.AndroidView

@Composable fun ExerciseVideoScreen(path: String?, onBack: () -> Unit) {
    var error by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<VideoView?>(null) }
    DisposableEffect(Unit) { onDispose { player?.stopPlayback() } }
    Page("Hướng dẫn bài tập", onBack) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (path.isNullOrBlank() || error) EmptyState("Không phát được video", "Tệp không tồn tại hoặc định dạng video không được hỗ trợ.")
            else AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { context -> VideoView(context).apply {
                player = this
                val controls = MediaController(context); controls.setAnchorView(this); setMediaController(controls)
                setOnErrorListener { _, _, _ -> error = true; true }
                setVideoURI(Uri.fromFile(java.io.File(path))); setOnPreparedListener { start() }
            } })
            Label("Video hướng dẫn", 13, modifier = Modifier.padding(12.dp))
        }
    }
}

