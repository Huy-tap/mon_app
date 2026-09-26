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
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.fitnessapp.data.MediaStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable fun EditExerciseScreen(exercise: Exercise, onBack: () -> Unit, onSave: (Exercise) -> Unit) { ExerciseForm(exercise, onBack, onSave) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ExerciseForm(initial: Exercise?, onBack: () -> Unit, onSave: (Exercise) -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var muscle by rememberSaveable { mutableStateOf(initial?.muscleGroup ?: "") }
    var sets by rememberSaveable { mutableStateOf((initial?.defaultSets ?: 3).toString()) }
    var reps by rememberSaveable { mutableStateOf((if (initial?.trackingType == "TIME") initial.defaultDurationSeconds else initial?.defaultReps ?: 12).toString()) }
    var imagePath by rememberSaveable { mutableStateOf(initial?.instructionImage) }
    var videoPath by rememberSaveable { mutableStateOf(initial?.instructionVideo) }
    var cameraFile by rememberSaveable { mutableStateOf<String?>(null) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var dropdown by remember { mutableStateOf(false) }; var imageSheet by remember { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    val draft = (initial ?: Exercise(name = "", muscleGroup = "")).copy(name = name.trim(), muscleGroup = muscle, defaultSets = sets.toIntOrNull() ?: 0,
        defaultReps = if (initial?.trackingType == "TIME") 0 else reps.toIntOrNull() ?: 0,
        defaultDurationSeconds = if (initial?.trackingType == "TIME") reps.toIntOrNull() else null, instructionImage = imagePath, instructionVideo = videoPath)
    val changed = name != (initial?.name ?: "") || muscle != (initial?.muscleGroup ?: "") || sets != (initial?.defaultSets ?: 3).toString() ||
        reps != (if (initial?.trackingType == "TIME") initial.defaultDurationSeconds else initial?.defaultReps ?: 12).toString() || imagePath != initial?.instructionImage || videoPath != initial?.instructionVideo
    val back = { if (!busy) { if (changed) discard = true else onBack() } }
    BackHandler(onBack = back)
    fun importUri(uri: Uri?, video: Boolean) { if (uri != null) scope.launch {
        busy = true
        try { val path = withContext(Dispatchers.IO) { MediaStorage.import(context, uri, video) }; if (video) videoPath = path else imagePath = path }
        catch (e: Exception) { Toast.makeText(context, e.message ?: "Không đọc được tệp.", Toast.LENGTH_LONG).show() }
        finally { busy = false }
    } }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importUri(it, false) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importUri(it, true) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        cameraFile?.let { if (ok && File(it).length() in 1..(10L * 1024 * 1024)) imagePath = it else {
            File(it).delete(); if (ok) Toast.makeText(context, "Ảnh vượt giới hạn 10 MiB.", Toast.LENGTH_LONG).show()
        } }; cameraFile = null
    }
    Page(if (initial == null) "Thêm bài tập" else "Sửa bài tập", back, footer = {
        PrimaryButton(if (busy) "Đang sao chép media…" else if (initial == null) "Lưu bài tập" else "Lưu thay đổi", !busy) {
            attempted = true
            if (FitnessRules.exerciseError(draft) == null) onSave(draft)
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label("* Tên 1–100 ký tự · Hiệp 1–100 · ${if (initial?.trackingType == "TIME") "Giây 1–86400" else "Lần 1–1000"}", 11)
            FormField("Tên bài tập *", name, { name = it }, placeholder = "VD: Hít đất", error = if (attempted && name.trim().length !in 1..100) "Tên phải có 1–100 ký tự." else null)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Label("Nhóm cơ *", 13, true)
                Box {
                    OutlinedButton({ dropdown = true }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Label(if (muscle.isBlank()) "Chọn nhóm cơ" else muscleLabel(muscle), modifier = Modifier.weight(1f)); Label("▾")
                    }
                    DropdownMenu(dropdown, { dropdown = false }) {
                        (listOf("Ngực", "Chân", "Bụng", "Vai", "Lưng", "Tay trước", "Tay sau", "Toàn thân", "Tim mạch") + listOfNotNull(initial?.muscleGroup)).distinctBy { muscleLabel(it) }.forEach { m ->
                            DropdownMenuItem(text = { Label(muscleLabel(m)) }, onClick = { muscle = m; dropdown = false })
                        }
                    }
                }
                if (attempted && muscle.isBlank()) Label("Vui lòng chọn nhóm cơ.", 12, color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                FormField("Số hiệp mặc định *", sets, { sets = it.filter(Char::isDigit).take(3) }, Modifier.weight(1f), numeric = true)
                FormField(if (initial?.trackingType == "TIME") "Số giây mỗi hiệp *" else "Số lần mỗi hiệp *", reps, { reps = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f), numeric = true)
            }
            if (attempted) FitnessRules.exerciseError(draft)?.let { Label(it, 12, color = MaterialTheme.colorScheme.error) }
            Label("Ảnh hướng dẫn · Tùy chọn", 14, true)
            MediaFormRow(imagePath, "＋ Thêm ảnh", { imageSheet = true }, { imagePath = null }, false, !busy)
            Label("JPEG / PNG · Tối đa 10 MiB", 12)
            Label("Video hướng dẫn · Tùy chọn", 14, true)
            MediaFormRow(videoPath, "＋ Chọn video từ thư viện", { videoPicker.launch(arrayOf("video/mp4")) }, { videoPath = null }, true, !busy)
            Label("MP4 · Tối đa 100 MiB", 12)
            Spacer(Modifier.height(16.dp))
        }
    }
    if (imageSheet) ImageSourceBottomSheet(onDismissRequest = { imageSheet = false }, onCameraClick = {
        imageSheet = false
        val file = MediaStorage.newFile(context, "jpg"); cameraFile = file.absolutePath
        runCatching { camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", file)) }.onFailure {
            file.delete(); cameraFile = null; Toast.makeText(context, "Thiết bị không có ứng dụng camera.", Toast.LENGTH_LONG).show()
        }
    }, onGalleryClick = { imageSheet = false; imagePicker.launch(arrayOf("image/jpeg", "image/png")) })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Label("Bỏ thay đổi?", 20, true) }, text = { Label("Các thay đổi chưa lưu sẽ bị bỏ.") },
        confirmButton = { TextButton({ discard = false; onBack() }) { Text("Bỏ thay đổi") } }, dismissButton = { TextButton({ discard = false }) { Text("Tiếp tục nhập") } })
}
@Composable private fun MediaFormRow(path: String?, add: String, onPick: () -> Unit, onRemove: () -> Unit, video: Boolean, enabled: Boolean) {
    Panel(border = false, padding = 4) {
        if (path.isNullOrBlank()) TextButton(onPick, Modifier.fillMaxWidth(), enabled = enabled) { Text(add, fontWeight = FontWeight.Bold) }
        else Row(verticalAlignment = Alignment.CenterVertically) {
            Label(if (!MediaStorage.exists(path)) "Tệp không có trên thiết bị" else if (video) "Video đã chọn" else "Ảnh đã chọn", 13, true, modifier = Modifier.weight(1f).padding(start = 8.dp))
            TextButton(onPick, enabled = enabled) { Text("Thay") }; TextButton(onRemove, enabled = enabled) { Text("Gỡ", color = MaterialTheme.colorScheme.error) }
        }
    }
}
