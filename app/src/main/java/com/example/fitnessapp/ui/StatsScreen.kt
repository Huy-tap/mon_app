package com.example.fitnessapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitnessapp.model.Workout
import java.time.YearMonth

@Composable
fun StatsScreen(
    workouts: List<Workout>,
    month: YearMonth,
    onMonth: (YearMonth) -> Unit,
    onTab: (BottomTab) -> Unit,
    onReminder: () -> Unit
) {
    Page(title = "Thống kê", tab = BottomTab.STATS, onTab = onTab) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Label("📊", 56)
            Spacer(Modifier.height(8.dp))
            Label("Module Thống Kê", 22, bold = true)
            Spacer(Modifier.height(4.dp))
            Label("Khu vực lập trình giao diện & biểu đồ của bạn.", 14, muted = true)
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Label("💡 Gợi ý dữ liệu có sẵn từ SQLite:", 14, bold = true)
                    Spacer(Modifier.height(6.dp))
                    Label("• Tổng số buổi tập: ${workouts.size} buổi", 13)
                    Label("• Tháng hiện tại: ${month.monthValue}/${month.year}", 13)
                }
            }
        }
    }
}
