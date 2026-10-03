package com.example.fitnessapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DeleteExerciseDialog(exerciseName: String, onConfirmDelete: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 420.dp),
            shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Label("Xóa bài “$exerciseName”?", 20, true)
                Label("Bài tập sẽ bị xóa khỏi danh mục.\nLịch sử và kết quả các buổi tập\nvẫn được giữ lại.", 14)
                PrimaryButton("Xóa bài tập", onClick = onConfirmDelete)
                SheetActionButton("Giữ lại", onDismiss)
            }
        }
    }
}
