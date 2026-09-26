package com.example.fitnessapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// BOTTOM SHEET: CHỌN NGUỒN VIDEO
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSourceBottomSheet(
    onDismissRequest: () -> Unit = {},
    onRecordVideoClick: () -> Unit = {},
    onGalleryVideoClick: () -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        scrimColor = Color(0x66000000),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nút 1: Quay video bằng camera
            SheetActionButton(
                text = "Quay video bằng camera",
                onClick = {
                    onRecordVideoClick()
                    onDismissRequest()
                }
            )

            // Nút 2: Chọn video từ thư viện
            SheetActionButton(
                text = "Chọn video từ thư viện",
                onClick = {
                    onGalleryVideoClick()
                    onDismissRequest()
                }
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Nút 3: Hủy
            SheetActionButton(
                text = "Hủy",
                onClick = onDismissRequest
            )
        }
    }
}

// ==========================================
// PREVIEW CHO ANDROID STUDIO
// ==========================================
@Preview(showBackground = true, name = "Video Source Action Sheet")
@Composable
fun VideoSourceBottomSheetPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x33000000))
                .padding(top = 100.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SheetActionButton(text = "Quay video bằng camera", onClick = {})
                    SheetActionButton(text = "Chọn video từ thư viện", onClick = {})
                    Spacer(modifier = Modifier.height(2.dp))
                    SheetActionButton(text = "Hủy", onClick = {})
                }
            }
        }
    }
}
