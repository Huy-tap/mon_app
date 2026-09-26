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
// BẢNG MÀU CHO BOTTOM SHEET CHỌN NGUỒN ẢNH
// ==========================================
private object SheetColors {
    val SheetBackground = Color(0xFFFFFFFF)
    val ButtonBackground = Color(0xFFF1F4F9)     // Nền nút màu xám xanh nhạt mềm mại
    val TextPrimary = Color(0xFF141624)          // Chữ đen đậm
    val ScrimColor = Color(0x66000000)           // Màu nền mờ che phía sau
}

// ==========================================
// BOTTOM SHEET: A07 · CHỌN NGUỒN ẢNH
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSourceBottomSheet(
    onDismissRequest: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = SheetColors.ScrimColor,
        dragHandle = null // Không hiển thị thanh kéo ngang theo đúng ảnh thiết kế
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nút 1: Chụp ảnh bằng camera
            SheetActionButton(
                text = "Chụp ảnh bằng camera",
                onClick = {
                    onCameraClick()
                    onDismissRequest()
                }
            )

            // Nút 2: Chọn ảnh từ thư viện
            SheetActionButton(
                text = "Chọn ảnh từ thư viện",
                onClick = {
                    onGalleryClick()
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

/**
 * Nút bấm tùy chỉnh bên trong Bottom Sheet
 */
@Composable
fun SheetActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ==========================================
// PREVIEW CHO ANDROID STUDIO
// ==========================================
@Preview(showBackground = true, name = "Image Source Action Sheet")
@Composable
fun ImageSourceBottomSheetContentPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x33000000))
                .padding(top = 100.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SheetActionButton(text = "Chụp ảnh bằng camera", onClick = {})
                    SheetActionButton(text = "Chọn ảnh từ thư viện", onClick = {})
                    Spacer(modifier = Modifier.height(2.dp))
                    SheetActionButton(text = "Hủy", onClick = {})
                }
            }
        }
    }
}
