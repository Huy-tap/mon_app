package com.example.fitnessapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.fitnessapp.data.ReminderScheduler
import com.example.fitnessapp.model.*

// Hàm hỗ trợ để Trang chủ hiển thị chuỗi giờ nhắc nhở (Bắt buộc giữ lại để Trang chủ không lỗi)
fun reminderScheduleLabel(r: Reminder): String = if (r.id == 0L) "Chưa thiết lập" else "${r.reminderTime.take(5)} · ${when (r.repeatType) {
    "WEEKLY" -> "Theo tuần (${reminderDays(r.repeatDays).joinToString(", ") { if (it == 7) "CN" else "T${it + 1}" }})"
    "ONCE" -> displayDate(r.scheduledDate ?: "")
    else -> "Hằng ngày"
}}"

// Hàm hỗ trợ kiểm tra quyền thông báo cho Trang chủ (Bắt buộc giữ lại để Trang chủ không lỗi)
@Composable internal fun notificationPermission(): Boolean {
    val context = LocalContext.current
    return ReminderScheduler.permitted(context)
}

@Composable
fun SettingsScreen(
    reminder: Reminder,
    dark: Boolean,
    onDark: (Boolean) -> Unit,
    onReminder: () -> Unit,
    onTab: (BottomTab) -> Unit
) {
    Page(title = "Cài đặt", tab = BottomTab.SETTINGS, onTab = onTab) { pad ->
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
            Label("⚙️", 56)
            Spacer(Modifier.height(8.dp))
            Label("Module Cài Đặt", 22, bold = true)
            Spacer(Modifier.height(4.dp))
            Label("Khu vực lập trình các tùy chọn cài đặt của bạn.", 14, muted = true)
            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = onReminder,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text("👉 Bấm vào đây để mở trang: Thiết lập giờ nhắc nhở")
            }
        }
    }
}

@Composable
fun ReminderSettingsScreen(
    reminder: Reminder,
    onBack: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    Page(title = "Cài đặt nhắc nhở", onBack = onBack) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Label("⏰", 56)
            Spacer(Modifier.height(8.dp))
            Label("Trang thiết lập lịch nhắc", 20, bold = true)
            Spacer(Modifier.height(4.dp))
            Label("Khu vực lập trình bộ hẹn giờ thông báo tập luyện.", 14, muted = true)
            Spacer(Modifier.height(20.dp))

            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Label("💡 Trạng thái hiện tại:", 14, bold = true)
                    Spacer(Modifier.height(6.dp))
                    Label("• Giờ nhắc: ${reminder.reminderTime}", 13)
                    Label("• Trạng thái: ${if (reminder.isEnabled) "Đang bật" else "Đang tắt"}", 13)
                }
            }
        }
    }
}
