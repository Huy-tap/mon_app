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

@Composable fun WorkoutHistoryScreen(workouts: List<Workout>, month: YearMonth, onMonth: (YearMonth) -> Unit,
    onRecord: () -> Unit, onDetail: (Long) -> Unit, onTab: (BottomTab) -> Unit) {
    val groups = workouts.filter { it.workoutDate.startsWith(month.toString()) }.groupBy { it.workoutDate }
    Page("Lịch sử tập luyện", tab = BottomTab.HISTORY, onTab = onTab, action = {
        Button(onRecord, Modifier.height(32.dp), shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)) {
            Text("＋ Ghi nhận", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp)) {
            MonthSelector(month, onMonth)
            if (groups.isEmpty()) EmptyState("Tháng này chưa có buổi tập", "Hãy chọn tháng khác để xem lại các buổi đã tập.")
            else LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                groups.forEach { (date, sessions) ->
                    item(key = date) { Label(displayDate(date), 16, true) }
                    items(sessions, key = { it.id }) { w ->
                        Panel(Modifier.clickable { onDetail(w.id) }) {
                            Label("${w.items.size} bài · ${w.durationMinutes} phút", 16, true)
                            Spacer(Modifier.height(6.dp))
                            w.items.forEach { Label("${it.exerciseName} · ${it.sets.size} hiệp", 13, muted = true, modifier = Modifier.padding(vertical = 2.dp)) }
                        }
                    }
                }
            }
        }
    }
}
