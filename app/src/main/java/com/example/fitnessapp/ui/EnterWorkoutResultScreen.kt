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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.platform.testTag

@Composable fun EnterWorkoutResultScreen(exercise: Exercise, initial: DraftEntry?, onBack: () -> Unit, onGuide: () -> Unit, onSave: (DraftEntry) -> Unit) {
    var count by rememberSaveable { mutableStateOf((initial?.values?.size ?: exercise.defaultSets).toString()) }
    var values by rememberSaveable { mutableStateOf((initial?.values ?: List(exercise.defaultSets) { if (exercise.trackingType == "TIME") exercise.defaultDurationSeconds ?: 30 else exercise.defaultReps }).map { it.toString() }) }
    var minutes by rememberSaveable { mutableStateOf(((initial?.durationSeconds ?: 0) / 60).toString()) }
    var seconds by rememberSaveable { mutableStateOf(((initial?.durationSeconds ?: 0) % 60).toString().padStart(2, '0')) }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var changed by rememberSaveable { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    val safeCount = (count.toIntOrNull() ?: 0).coerceIn(0, 100)
    LaunchedEffect(safeCount) { values = List(safeCount) { values.getOrNull(it) ?: "" } }
    val back = { if (changed) discard = true else onBack() }
    BackHandler(onBack = back)
    val entry = DraftEntry(exercise.id, exercise.name, exercise.muscleGroup, values.map { it.toIntOrNull() ?: 0 },
        (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0), note.trim(), exercise.trackingType)
    val error = when {
        count.toIntOrNull() !in 1..100 -> "Số hiệp phải từ 1 đến 100."
        seconds.toIntOrNull() !in 0..59 -> "Số giây phải từ 0 đến 59."
        minutes.toIntOrNull() !in 0..1440 -> "Số phút phải từ 0 đến 1440."
        else -> FitnessRules.entryError(entry)
    }
    Page(if (initial == null) "Nhập kết quả" else "Sửa kết quả", back, footer = {
        PrimaryButton(if (initial == null) "Thêm vào phiếu" else "Lưu thay đổi") { attempted = true; if (error == null) onSave(entry) }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Label(exercise.name, 20, true); Label(muscleLabel(exercise.muscleGroup), 14, muted = true) }
                TextButton(onGuide) { Text("▷ Xem hướng dẫn", fontSize = 12.sp) }
            }
            FormField("Số hiệp đã thực hiện", count, { count = it.filter(Char::isDigit).take(3); changed = true }, numeric = true, embeddedLabel = true)
            Label("Số mặc định chỉ để gợi ý. Hãy nhập kết quả thực tế.", 12, muted = true)
            Label(if (exercise.trackingType == "TIME") "Số giây từng hiệp" else "Số lần từng hiệp", 14, true)
            values.forEachIndexed { index, value ->
                Panel(padding = 6) {
                    Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Label("Hiệp ${index + 1}", 14, modifier = Modifier.weight(1f))
                        BasicTextField(value, { v -> values = values.toMutableList().also { it[index] = v.filter(Char::isDigit).take(5) }; changed = true },
                            Modifier.width(64.dp).height(36.dp).testTag("set/${index + 1}").border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(8.dp)), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            decorationBox = { input -> Box(Modifier.fillMaxSize(), Alignment.Center) { input() } })
                        Label(if (exercise.trackingType == "TIME") "giây" else "lần", 12, muted = true, modifier = Modifier.padding(10.dp))
                    }
                }
            }
            Label("Thời lượng", 14, true)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                FormField("Phút", minutes, { minutes = it.filter(Char::isDigit).take(4); changed = true }, Modifier.weight(1f), numeric = true, embeddedLabel = true)
                FormField("Giây", seconds, { seconds = it.filter(Char::isDigit).take(2); changed = true }, Modifier.weight(1f), numeric = true, embeddedLabel = true)
            }
            Label("Nhập thời gian của bài, gồm nghỉ giữa các hiệp; không tính thời gian nhập liệu.", 12, muted = true)
            FormField("Ghi chú", note, { note = it.take(2000); changed = true }, singleLine = false, outlined = true)
            if (attempted && error != null) Label(error, 13, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Label("Bỏ thay đổi?", 20, true) }, text = { Label("Kết quả đang nhập chưa được thêm vào phiếu.") },
        confirmButton = { TextButton({ discard = false; onBack() }) { Text("Bỏ thay đổi") } }, dismissButton = { TextButton({ discard = false }) { Text("Tiếp tục nhập") } })
}
