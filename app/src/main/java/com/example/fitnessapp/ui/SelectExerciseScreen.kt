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

@Composable fun SelectExerciseScreen(exercises: List<Exercise>, onBack: () -> Unit, onSelect: (Long) -> Unit, onAdd: () -> Unit) {
    var category by rememberSaveable { mutableStateOf("Tất cả") }
    val categories = listOf("Tất cả") + exercises.map { muscleLabel(it.muscleGroup) }.distinct()
    val filtered = exercises.filter { category == "Tất cả" || muscleLabel(it.muscleGroup) == category }
    Page("Chọn bài tập", onBack) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp)) {
                categories.forEach { title ->
                    FilterChip(category == title, { category = title }, label = { Label(title, 13, true,
                        color = if (category == title) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) },
                        shape = RoundedCornerShape(18.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, containerColor = MaterialTheme.colorScheme.surface))
                }
            }
            Spacer(Modifier.height(16.dp))
            if (exercises.isEmpty()) { EmptyState("Danh mục chưa có bài tập", "Thêm bài tập để bắt đầu ghi nhận buổi tập."); Spacer(Modifier.height(20.dp)); PrimaryButton("＋ Thêm bài tập", onClick = onAdd) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(filtered, key = { it.id }) { e -> ExerciseRow(e) { onSelect(e.id) } }
            }
        }
    }
}
