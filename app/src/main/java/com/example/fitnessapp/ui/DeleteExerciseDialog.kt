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

@Composable fun DeleteExerciseDialog(exerciseName: String, onConfirmDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Label("Xóa bài \"$exerciseName\"?", 20, true) },
        text = { Label("Bài tập sẽ bị xóa khỏi danh mục. Lịch sử và kết quả các buổi tập vẫn được giữ lại.") },
        confirmButton = { TextButton(onConfirmDelete) { Text("Xóa") } }, dismissButton = { TextButton(onDismiss) { Text("Hủy") } })
}
