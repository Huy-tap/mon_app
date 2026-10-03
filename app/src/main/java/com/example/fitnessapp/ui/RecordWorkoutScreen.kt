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

import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler

@Composable fun RecordWorkoutScreen(draft: WorkoutDraft, onDraft: (WorkoutDraft) -> Unit, onBack: () -> Unit, onAdd: () -> Unit,
    onEdit: (Long) -> Unit, onSave: () -> Unit) {
    val context = LocalContext.current
    var removeId by remember { mutableStateOf<Long?>(null) }
    var discard by remember { mutableStateOf(false) }
    val back = { if (draft.entries.isNotEmpty()) discard = true else onBack() }
    BackHandler(onBack = back)
    Page("Ghi nhận buổi tập", back, footer = { PrimaryButton("Lưu buổi tập", draft.entries.isNotEmpty(), onClick = onSave) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Panel(Modifier.clickable {
                val day = LocalDate.parse(draft.date)
                DatePickerDialog(context, { _, y, m, d -> onDraft(draft.copy(date = LocalDate.of(y, m + 1, d).toString())) }, day.year, day.monthValue - 1, day.dayOfMonth).apply {
                    datePicker.maxDate = System.currentTimeMillis(); show()
                }
            }, padding = 12) {
                Label("Ngày tập", 12)
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Label(displayDate(draft.date), 16, true); Label("▦", 20) }
            }
            if (draft.entries.isEmpty()) Panel { Label("Chưa có bài tập trong phiếu", 16, true); Label("Chọn bài tập và nhập kết quả bạn vừa thực hiện.", 13, muted = true) }
            draft.entries.forEach { e ->
                Panel {
                    Label(e.name, 16, true); Label("${muscleLabel(e.muscle)} · ${e.values.size} hiệp", 13)
                    Label("${e.values.joinToString(" / ")} ${if (e.trackingType == "TIME") "giây" else "lần"} · ${durationLabel(e.durationSeconds)}", 13)
                    if (e.note.isNotBlank()) Label("Ghi chú: ${e.note}", 12)
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), Arrangement.spacedBy(6.dp, Alignment.End)) {
                        Box(Modifier.size(44.dp, 32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onEdit(e.exerciseId) }, Alignment.Center) { Label("Sửa", 12, true) }
                        Box(Modifier.size(36.dp, 32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { removeId = e.exerciseId }, Alignment.Center) { Label("×", 20) }
                    }
                }
            }
            OutlinedButton(onAdd, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) { Text("＋ Chọn bài tập", fontWeight = FontWeight.Bold) }
            Column { Label("${draft.entries.size} bài · ${durationLabel(draft.entries.sumOf { it.durationSeconds })}", 16, true); Label("Chưa lưu vào lịch sử", 12, muted = true) }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (removeId != null) AlertDialog(onDismissRequest = { removeId = null }, title = { Label("Bỏ bài khỏi phiếu?", 20, true) },
        text = { Label("Bài tập vẫn được giữ trong danh mục.") }, confirmButton = { TextButton({ onDraft(draft.copy(entries = draft.entries.filter { it.exerciseId != removeId })); removeId = null }) { Text("Bỏ bài") } },
        dismissButton = { TextButton({ removeId = null }) { Text("Hủy") } })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Label("Bỏ cả phiếu?", 20, true) }, text = { Label("Các kết quả trong phiếu chưa được lưu vào lịch sử.") },
        confirmButton = { TextButton({ onDraft(WorkoutDraft()); discard = false; onBack() }) { Text("Bỏ phiếu") } }, dismissButton = { TextButton({ discard = false }) { Text("Tiếp tục tập") } })
}
