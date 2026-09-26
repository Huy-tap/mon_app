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

import com.example.fitnessapp.data.MediaStorage

@Composable fun ExerciseDetailScreen(exercise: Exercise, onBack: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onMedia: (Boolean) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val hasImage = MediaStorage.exists(exercise.instructionImage)
    val hasVideo = MediaStorage.exists(exercise.instructionVideo)
    Page("Chi tiết bài tập", onBack, footer = {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(2f)) { PrimaryButton("Sửa bài tập", onClick = onEdit) }
            OutlinedButton({ confirm = true }, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Xóa") }
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (hasImage) ExercisePhoto(exercise.instructionImage, Modifier.fillMaxWidth().height(214.dp).clip(RoundedCornerShape(12.dp)).clickable { onMedia(false) }, "ẢNH HƯỚNG DẪN")
            Label(exercise.name, if (hasImage) 24 else 28, true)
            Label("Nhóm cơ: ${muscleLabel(exercise.muscleGroup)}", 15)
            Surface(shape = RoundedCornerShape(12.dp)) {
                Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                    Label("${exercise.defaultSets} hiệp   ×   ${if (exercise.trackingType == "TIME") "${exercise.defaultDurationSeconds} giây" else "${exercise.defaultReps} lần"} / hiệp", 18, true)
                }
            }
            if (hasVideo) {
                Label("Video hướng dẫn", 16, true)
                OutlinedButton({ onMedia(true) }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("▷  Phát video") }
            }
            if (!hasImage && !hasVideo) Panel {
                Label("Bài tập này chưa có ảnh hoặc video hướng dẫn.", 13, true)
                Spacer(Modifier.height(6.dp)); Label("Chọn Sửa bài tập để thêm. Phần ảnh/video được ẩn khi không có dữ liệu.", 12, muted = true)
            }
            exercise.description?.takeIf { it.isNotBlank() }?.let { Label(it, 13) }
            Label("Chưa bắt đầu buổi tập. Đây là thông tin mặc định của bài tập.", 12)
        }
    }
    if (confirm) DeleteExerciseDialog(exercise.name, { confirm = false; onDelete() }, { confirm = false })
}
