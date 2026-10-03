package com.example.fitnessapp.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import com.example.fitnessapp.data.MediaStorage
import com.example.fitnessapp.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable fun EditExerciseScreen(exercise: Exercise, onBack: () -> Unit, onSave: (Exercise) -> Unit) {
    ExerciseForm(exercise, onBack, onSave)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseForm(initial: Exercise?, onBack: () -> Unit, onSave: (Exercise) -> Unit) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var muscle by rememberSaveable { mutableStateOf(initial?.muscleGroup ?: "") }
    var sets by rememberSaveable { mutableStateOf((initial?.defaultSets ?: 3).toString()) }
    val timeBased = initial?.trackingType == "TIME"
    val initialValue = (if (timeBased) initial?.defaultDurationSeconds ?: 30 else initial?.defaultReps ?: 12).toString()
    var reps by rememberSaveable { mutableStateOf(initialValue) }
    var imagePath by rememberSaveable { mutableStateOf(initial?.instructionImage) }
    var videoPath by rememberSaveable { mutableStateOf(initial?.instructionVideo) }
    var cameraFile by rememberSaveable { mutableStateOf<String?>(null) }
    var recordingFile by rememberSaveable { mutableStateOf<String?>(null) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var imageSheet by remember { mutableStateOf(false) }
    var videoSheet by remember { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    var previewPath by rememberSaveable { mutableStateOf<String?>(null) }
    var previewVideo by rememberSaveable { mutableStateOf(false) }
    fun message(text: String) { Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
    val draft = (initial ?: Exercise(name = "", muscleGroup = "")).copy(
        name = name.trim(), muscleGroup = muscle, defaultSets = sets.toIntOrNull() ?: 0,
        defaultReps = if (timeBased) 0 else reps.toIntOrNull() ?: 0,
        defaultDurationSeconds = if (timeBased) reps.toIntOrNull() else null,
        instructionImage = imagePath, instructionVideo = videoPath)
    val changed = name != (initial?.name ?: "") || muscle != (initial?.muscleGroup ?: "") ||
        sets != (initial?.defaultSets ?: 3).toString() || reps != initialValue ||
        imagePath != initial?.instructionImage || videoPath != initial?.instructionVideo
    val back = { if (!busy) { if (changed) discard = true else onBack() } }
    BackHandler(onBack = back)
    fun importUri(uri: Uri?, video: Boolean) {
        if (uri == null) return
        scope.launch {
            busy = true
            try {
                val path = withContext(Dispatchers.IO) { MediaStorage.import(context, uri, video) }
                if (video) videoPath = path else imagePath = path
            } catch (e: Exception) { message(e.message ?: "Không đọc được tệp.") }
            finally { busy = false }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { importUri(it, false) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { importUri(it, true) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        cameraFile?.let { path ->
            val file = File(path)
            if (ok && file.length() in 1..(10L * 1024 * 1024)) imagePath = path
            else { file.delete(); file.parentFile?.delete(); if (ok) message("Ảnh trống hoặc vượt giới hạn 10 MiB.") }
        }
        cameraFile = null
    }
    val recorder = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        val path = recordingFile
        recordingFile = null
        if (path != null) scope.launch {
            busy = true
            try {
                val valid = withContext(Dispatchers.IO) {
                    val file = File(path)
                    ok && file.length() in 1..(100L * 1024 * 1024) && MediaStorage.videoDuration(path) != null
                }
                if (valid) videoPath = path
                else { File(path).delete(); File(path).parentFile?.delete(); if (ok) message("Video không hợp lệ hoặc vượt giới hạn 100 MiB.") }
            } finally { busy = false }
        }
    }
    fun openCamera(video: Boolean) {
        var file: File? = null
        try {
            file = MediaStorage.newFile(context, if (video) "mp4" else "jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            if (video) { recordingFile = file.absolutePath; recorder.launch(uri) }
            else { cameraFile = file.absolutePath; camera.launch(uri) }
        } catch (e: Exception) {
            file?.delete(); file?.parentFile?.delete()
            if (video) recordingFile = null else cameraFile = null
            message("Không mở được camera. Bạn có thể chọn ${if (video) "video" else "ảnh"} từ thư viện.")
        }
    }
    fun openPicker(video: Boolean) {
        runCatching {
            if (video) videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            else imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }.onFailure { message("Không mở được thư viện trên thiết bị này.") }
    }
    val nameError = when {
        !attempted -> null
        name.isBlank() -> "Vui lòng nhập tên bài tập."
        name.trim().length > 100 -> "Tên bài tập tối đa 100 ký tự."
        else -> null
    }
    val setsError = if (attempted && sets.toIntOrNull() !in 1..100) "Số hiệp phải từ 1 đến 100." else null
    val repsError = if (attempted && reps.toIntOrNull() !in 1..(if (timeBased) 86400 else 1000))
        if (timeBased) "Số giây phải từ 1 đến 86400." else "Số lần phải từ 1 đến 1000." else null
    Page(if (initial == null) "Thêm bài tập" else "Sửa bài tập", back, footer = {
        PrimaryButton(if (busy) "Đang sao chép media…" else if (initial == null) "Lưu bài tập" else "Lưu thay đổi", !busy) {
            keyboard?.hide(); attempted = true
            if (FitnessRules.exerciseError(draft) == null) onSave(draft)
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).alpha(if (busy) 0.45f else 1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Label(when {
                busy -> "Đang sao chép ảnh/video…"
                attempted && FitnessRules.exerciseError(draft) != null -> "Chưa lưu. Vui lòng kiểm tra thông tin."
                initial != null -> "* Thông tin bắt buộc"
                else -> "* Tên 1–100 ký tự · Hiệp 1–100 · ${if (timeBased) "Giây 1–86400" else "Lần 1–1000"}"
            }, if (initial == null) 11 else 12)
            FormField("Tên bài tập *", name, { if (!busy) name = it }, Modifier.heightIn(min = 76.dp), placeholder = "VD: Hít đất", error = nameError)
            MuscleGroupField(muscle, { muscle = it }, !busy, attempted && muscle.isBlank())
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                FormField("Số hiệp mặc định *", sets, { if (!busy) sets = it.filter(Char::isDigit).take(3) }, Modifier.weight(1f).heightIn(min = 76.dp), numeric = true, error = setsError)
                FormField(if (timeBased) "Số giây mỗi hiệp *" else "Số lần mỗi hiệp *", reps,
                    { if (!busy) reps = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f).heightIn(min = 76.dp), numeric = true, error = repsError)
            }
            Label("Ảnh hướng dẫn · Tùy chọn", 14, true)
            MediaFormRow(imagePath, "＋ Thêm ảnh", { keyboard?.hide(); imageSheet = true }, { imagePath = null }, {
                previewVideo = false; previewPath = imagePath
            }, false, !busy)
            Label("JPEG / PNG · Tối đa 10 MiB", 12)
            Label("Video hướng dẫn · Tùy chọn", 14, true)
            MediaFormRow(videoPath, "＋ Thêm video", { keyboard?.hide(); videoSheet = true }, { videoPath = null }, {
                previewVideo = true; previewPath = videoPath
            }, true, !busy)
            Label("MP4 · Tối đa 100 MiB", 12)
            Spacer(Modifier.height(16.dp))
        }
    }
    if (imageSheet) ImageSourceBottomSheet({ imageSheet = false }, { openCamera(false) }, { openPicker(false) })
    if (videoSheet) VideoSourceBottomSheet({ videoSheet = false }, { openCamera(true) }, { openPicker(true) })
    if (previewPath != null) Dialog({ previewPath = null }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        if (previewVideo) ExerciseVideoScreen(previewPath) { previewPath = null }
        else ExerciseImageScreen(previewPath) { previewPath = null }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Label("Bỏ thay đổi?", 20, true) }, text = { Label("Các thay đổi chưa lưu sẽ bị bỏ.") },
        confirmButton = { TextButton({ discard = false; onBack() }) { Text("Bỏ thay đổi") } }, dismissButton = { TextButton({ discard = false }) { Text("Tiếp tục nhập") } })
}

@Composable
internal fun MuscleGroupField(value: String, onSelect: (String) -> Unit, enabled: Boolean, error: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    var width by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val keyboard = LocalSoftwareKeyboardController.current
    val options = listOf("Ngực", "Lưng", "Chân", "Tay", "Bụng", "Vai", "Toàn thân")
    Column(Modifier.heightIn(min = 76.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Label("Nhóm cơ *", 13, true)
        Box {
            Row(Modifier.fillMaxWidth().height(48.dp).onGloballyPositioned { width = it.size.width }
                .clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface)
                .border(if (expanded) 2.dp else 1.dp, when { expanded -> MaterialTheme.colorScheme.onSurface; error -> MaterialTheme.colorScheme.error; else -> Color.Transparent }, RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, role = Role.DropdownList) { keyboard?.hide(); expanded = true }
                .testTag("exercise/muscle-picker").padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Label(if (value.isBlank()) "Chọn nhóm cơ" else muscleLabel(value), 14, muted = value.isBlank(), modifier = Modifier.weight(1f))
                Label("▾", 12)
            }
            if (expanded) Popup(alignment = Alignment.TopStart, offset = IntOffset(0, with(density) { 52.dp.roundToPx() }),
                onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = true)) {
                Surface(Modifier.width(with(density) { width.toDp() }), shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.heightIn(max = 308.dp).verticalScroll(rememberScrollState())) {
                        options.forEach { group ->
                            val selected = muscleLabel(value) == group || (value.isBlank() && group == options.first())
                            Box(Modifier.fillMaxWidth().height(44.dp)
                                .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                .selectable(selected = selected, role = Role.RadioButton) { onSelect(group); expanded = false }
                                .padding(horizontal = 16.dp), Alignment.CenterStart) { Label(group, 14) }
                        }
                    }
                }
            }
        }
        if (error) Label("Vui lòng chọn nhóm cơ.", 12, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun MediaFormRow(path: String?, add: String, onPick: () -> Unit, onRemove: () -> Unit, onPreview: () -> Unit, video: Boolean, enabled: Boolean) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) {
        if (path.isNullOrBlank()) Box(Modifier.fillMaxWidth().height(48.dp).clickable(enabled, role = Role.Button, onClick = onPick), Alignment.Center) { Label(add, 14, true) }
        else Row(Modifier.heightIn(min = 48.dp).padding(start = 16.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (!MediaStorage.exists(path)) "Tệp không có trên thiết bị" else if (video) File(path).name else "Ảnh đã chọn",
                Modifier.weight(1f).heightIn(min = 48.dp).clickable(enabled, role = Role.Button, onClick = onPreview)
                    .semantics { contentDescription = if (video) "Xem trước video" else "Xem trước ảnh" }.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MediaAction("Thay", enabled, false, onPick)
            MediaAction("Gỡ", enabled, true, onRemove)
        }
    }
}

@Composable
private fun MediaAction(text: String, enabled: Boolean, remove: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (remove) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.surfaceVariant)
        .clickable(enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp), Alignment.Center) {
        Label(text, 14, true, color = if (remove) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface)
    }
}
