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

@Composable fun WorkoutDetailScreen(workout: Workout, onBack: () -> Unit) {
    Page("Chi tiết buổi tập", onBack) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Column { Label(displayDate(workout.workoutDate), 24, true); Label("${workout.items.size} bài · ${workout.durationMinutes} phút", 14, muted = true) } }
            items(workout.items, key = { it.workoutExerciseId }) { e ->
                Panel {
                    Label(e.exerciseName, 16, true); Label(muscleLabel(e.muscle), 13, muted = true)
                    Spacer(Modifier.height(10.dp))
                    e.sets.forEach { s -> Label("Hiệp ${s.setNumber}: ${s.reps?.let { "$it lần" } ?: "${s.durationSeconds ?: 0} giây"}", 13) }
                    if (e.durationSeconds > 0) Label(durationLabel(e.durationSeconds), 13, true)
                    e.notes?.takeIf { it.isNotBlank() }?.let { Spacer(Modifier.height(6.dp)); Label("Ghi chú: $it", 12, muted = true) }
                }
            }
        }
    }
}
