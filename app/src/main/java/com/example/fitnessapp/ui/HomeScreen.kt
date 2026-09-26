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

import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable fun HomeScreen(workouts: List<Workout>, reminder: Reminder, onTab: (BottomTab) -> Unit,
    onRecord: () -> Unit, onDetail: (Long) -> Unit, onReminder: () -> Unit) {
    val allowed = notificationPermission()
    val today = LocalDate.now()
    val stats = FitnessRules.monthStats(workouts, YearMonth.from(today))
    val recent = workouts.firstOrNull()
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = { FitnessBottomNavigation(BottomTab.HOME, onTab) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column {
                Label("Trang chủ", 26, true)
                Label(today.format(DateTimeFormatter.ofPattern("EEEE, d 'tháng' M", Locale.forLanguageTag("vi-VN"))).replaceFirstChar { it.uppercase() }, 13)
            }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label("Sẵn sàng vận động?", 20, true); Label("ghi lại buổi tập hôm nay", 13)
                    Button(onRecord, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) { Text("Ghi nhận buổi tập", fontSize = 15.sp) }
                    Spacer(Modifier.height(1.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Label("Tháng ${today.monthValue} của bạn", 17, true)
                Panel(border = false, padding = 12) {
                    Row(Modifier.fillMaxWidth()) {
                        listOf(stats.totalWorkouts to "Buổi tập", stats.totalMinutes to "Phút tập", stats.completedSets to "Lượt bài xong").forEach { (n, title) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Label(n.toString().padStart(2, '0'), 24, true); Label(title, 12)
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Label("Buổi tập gần nhất", 17, true)
                    Label("Xem tất cả →", 13, modifier = Modifier.clickable { onTab(BottomTab.HISTORY) }, color = MaterialTheme.colorScheme.secondary)
                }
                Panel(modifier = if (recent != null) Modifier.clickable { onDetail(recent.id) } else Modifier) {
                    if (recent == null) { Label("Chưa có buổi tập", 15, true); Label("Ghi nhận buổi tập đầu tiên của bạn.", 13, muted = true) }
                    else {
                        Label(if (recent.workoutDate == today.toString()) "Hôm nay" else displayDate(recent.workoutDate), 15, true)
                        Spacer(Modifier.height(5.dp)); Label(recent.exerciseNames.joinToString(", "), 14)
                        Spacer(Modifier.height(5.dp)); Label("${recent.durationMinutes} phút   ·   ${recent.items.size} lượt bài hoàn thành", 12)
                        Label("Chi tiết →", 12, true, modifier = Modifier.align(Alignment.End), color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            Panel(border = false, padding = 16) {
                Label("Nhắc nhở tập luyện", 15, true); Spacer(Modifier.height(5.dp))
                Label(reminderScheduleLabel(reminder), 14); Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Label(if (!reminder.isEnabled) "Đang tắt" else if (!allowed) "Chưa được cấp quyền" else "Đang bật", 12)
                    Label("Chỉnh sửa →", 12, modifier = Modifier.clickable(onClick = onReminder), color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
