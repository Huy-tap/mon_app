package com.example.fitnessapp.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSourceBottomSheet(
    onDismissRequest: () -> Unit = {},
    onRecordVideoClick: () -> Unit = {},
    onGalleryVideoClick: () -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) = MediaSourceSheet(onDismissRequest, sheetState) {
    SheetActionButton("Quay video bằng camera", { onDismissRequest(); onRecordVideoClick() })
    SheetActionButton("Chọn video từ thư viện", { onDismissRequest(); onGalleryVideoClick() })
    SheetActionButton("Hủy", onDismissRequest)
}
