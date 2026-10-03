package com.example.fitnessapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fitnessapp.data.ReminderAvailability
import com.example.fitnessapp.data.ReminderScheduler
import com.example.fitnessapp.model.*
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import androidx.lifecycle.repeatOnLifecycle

@Composable private fun reminderClock(): ZonedDateTime {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { now = ZonedDateTime.now(); delay(1000) }
        }
    }
    return now
}

fun reminderScheduleLabel(r: Reminder): String = if (r.id == 0L) "Chưa thiết lập" else when (r.repeatType) {
    "WEEKLY" -> "${reminderDays(r.repeatDays).sorted().joinToString(", ") { if (it == 7) "CN" else "T${it + 1}" }} · ${r.reminderTime.take(5)}"
    "ONCE" -> "${displayDate(r.scheduledDate ?: "")} · ${r.reminderTime.take(5)}"
    "DAILY" -> "Hằng ngày lúc ${r.reminderTime.take(5)}"
    else -> "Lịch không hợp lệ"
}

@Composable internal fun notificationPermission(): Boolean {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var allowed by remember { mutableStateOf(ReminderScheduler.permitted(context)) }
    DisposableEffect(context, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) allowed = ReminderScheduler.permitted(context) }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    return allowed
}

internal fun nextReminderLabel(at: ZonedDateTime?, now: LocalDate = LocalDate.now()): String {
    if (at == null) return "Chưa có lịch sắp tới"
    val date = when (at.toLocalDate()) { now -> "Hôm nay"; now.plusDays(1) -> "Ngày mai"; else -> at.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
    return "$date, ${at.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

private fun openSystemSettings(context: Context, exact: Boolean = false) {
    val manager = context.getSystemService(android.app.NotificationManager::class.java)
    val intent = when {
        exact && Build.VERSION.SDK_INT >= 31 -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        manager.getNotificationChannel(ReminderScheduler.CHANNEL)?.importance == android.app.NotificationManager.IMPORTANCE_NONE ->
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, ReminderScheduler.CHANNEL)
        else -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun reminderStatus(r: Reminder, availability: ReminderAvailability): String = when {
    r.id == 0L -> "Chưa thiết lập"
    !r.isEnabled -> "Đang tắt"
    !availability.notifications || availability.error != null || availability.nextAt == null -> "Chưa hoạt động"
    else -> "Đang bật"
}

@Composable fun SettingsScreen(reminder: Reminder, dark: Boolean, onDark: (Boolean) -> Unit,
    onReminder: () -> Unit, onTab: (BottomTab) -> Unit, availability: ReminderAvailability) {
    val context = LocalContext.current
    val status = reminderStatus(reminder, availability)
    Page("Cài đặt", onBack = { onTab(BottomTab.HOME) }, tab = BottomTab.SETTINGS, onTab = onTab, titleSize = 22) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Label("Giao diện", 16, true)
                Panel(padding = 16, radius = 16) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(false to "☀️ Sáng", true to "🌙 Tối").forEach { (value, label) ->
                            val selected = dark == value
                            val selectedBg = if (dark) Color(0xFF4F378B) else Color(0xFFEFF6FF)
                            Surface(Modifier.weight(1f).selectable(selected, onClick = { onDark(value) }, role = Role.RadioButton),
                                shape = RoundedCornerShape(12.dp), color = if (selected) selectedBg else MaterialTheme.colorScheme.surface,
                                border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                Box(Modifier.padding(vertical = 10.dp), Alignment.Center) { Label(label, 14, selected, color = if (selected) { if (dark) Color(0xFFE8DEF8) else Color(0xFF2563EB) } else MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Label("Nhắc nhở tập luyện", 16, true)
                Panel(padding = 16, radius = 16) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(when (status) { "Đang bật" -> Color(0xFF10B981); "Chưa hoạt động" -> Color(0xFFF97316); else -> Color(0xFF9CA3AF) }, RoundedCornerShape(4.dp)))
                            Spacer(Modifier.width(8.dp)); Label("Trạng thái", modifier = Modifier.weight(1f)); Label(status, muted = true)
                        }
                        if (reminder.isEnabled && !availability.notifications) {
                            Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(8.dp)) { Label("⚠ Quyền thông báo đang bị chặn", 12, color = Color(0xFFC2410C), modifier = Modifier.fillMaxWidth().padding(8.dp)) }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        if (reminder.id != 0L) {
                            SettingsRow("Lịch nhắc", reminderScheduleLabel(reminder))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            if (reminder.isEnabled && !availability.notifications) SettingsRow("Mở cài đặt hệ thống", onClick = { openSystemSettings(context) })
                            else SettingsRow("Lần nhắc kế tiếp", if (reminder.isEnabled) nextReminderLabel(availability.nextAt) else "Đã tắt")
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        SettingsRow("Thiết lập lịch nhắc", onClick = onReminder)
                    }
                }
                if (reminder.isEnabled && availability.notifications && !availability.exact) ExactAlarmNotice()
                availability.error?.let { Label(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable private fun SettingsRow(title: String, value: String? = null, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Label(title, modifier = Modifier.weight(1f))
        if (value != null) Text(value, Modifier.weight(1.15f), fontSize = 14.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else FigmaIcon("ba9dd.svg", Modifier.size(16.dp))
    }
}

@Composable private fun ExactAlarmNotice() {
    val context = LocalContext.current
    Panel(padding = 12, radius = 12) {
        Label("Có thể nhắc trễ", 14, true)
        Label("Cho phép báo thức chính xác để nhắc gần giờ bạn chọn.", 12, muted = true)
        TextButton({ openSystemSettings(context, exact = true) }) { Text("Cho phép nhắc đúng giờ") }
    }
}

@Composable fun ReminderSettingsScreen(reminder: Reminder, onBack: () -> Unit,
    availability: ReminderAvailability, saving: Boolean = false, saveError: String? = null, saved: Boolean = false,
    onPermissionChanged: () -> Unit = {}, onSave: (Reminder) -> Unit) {
    val context = LocalContext.current
    var enabled by rememberSaveable { mutableStateOf(reminder.isEnabled) }
    var repeat by rememberSaveable { mutableStateOf(reminder.repeatType) }
    var time by rememberSaveable { mutableStateOf(reminder.reminderTime) }
    var days by rememberSaveable { mutableStateOf(reminderDays(reminder.repeatDays).sorted().joinToString(",")) }
    var picker by rememberSaveable { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var edited by rememberSaveable { mutableStateOf(false) }
    val draft = reminder.copy(isEnabled = enabled, repeatType = repeat, reminderTime = time, repeatDays = if (repeat == "WEEKLY") days else null,
        scheduledDate = if (repeat == "ONCE") reminder.scheduledDate else null)
    val validation = reminderError(draft)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onPermissionChanged() }
    val accent = if (MaterialTheme.colorScheme.background == Color(0xFF1C1B1F)) Color(0xFF6750A4) else Color(0xFF1E3A8A)
    val blocked = enabled && !availability.notifications
    val now = reminderClock()
    val nextAt = if (!enabled || blocked) null else if (!edited && reminder.id != 0L) availability.nextAt else ReminderRules.next(draft, now)
    val inactive = blocked || enabled && !edited && reminder.id != 0L && (availability.error != null || nextAt == null)
    ProvideTextStyle(LocalTextStyle.current.copy(fontFamily = ReminderFont)) {
        Page("Nhắc nhở tập luyện", onBack, roundBack = true, footer = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (saved && !edited) Label("Đã lưu cài đặt", 13, color = MaterialTheme.colorScheme.secondary)
                saveError?.let { Label(it, 13, color = MaterialTheme.colorScheme.error) }
                PrimaryButton(if (saving) "Đang lưu…" else "Lưu cài đặt", enabled = !saving, radius = 999, height = 52, color = accent) {
                    attempted = true
                    if (validation == null) { edited = false; onSave(draft) }
                }
            }
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (blocked) Panel(padding = 16, radius = 16) {
                    Label("Chưa hoạt động · Thiếu quyền thông báo", 13, true, color = Color(0xFFC2410C))
                    Spacer(Modifier.height(12.dp)); Label("Quyền thông báo bị tắt", 16, true, color = Color(0xFFC2410C))
                    Spacer(Modifier.height(12.dp)); Label("Ứng dụng không thể gửi lời nhắc tập luyện vì thông báo bị chặn từ hệ thống. Hãy cấp quyền để duy trì thói quen tập luyện.", 14, muted = true)
                    Spacer(Modifier.height(16.dp)); PrimaryButton("Mở cài đặt hệ thống", radius = 999, height = 52, color = accent) { openSystemSettings(context) }
                }
                Panel(padding = 20, radius = 16) {
                    ReminderHeading("Trạng thái nhắc nhở", 18)
                    Label(if (!enabled) "Đang tắt" else if (inactive) "Chưa hoạt động" else "Đang bật", 14, color = if (inactive) Color(0xFFC2410C) else accent)
                    Spacer(Modifier.height(8.dp))
                    Switch(enabled, onCheckedChange = {
                        enabled = it; edited = true
                        if (it && Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }, modifier = Modifier.testTag("reminder/enabled"), colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.White))
                }
                Panel(padding = 16, radius = 16) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FigmaIcon("clock.svg", Modifier.size(20.dp))
                        Label(if (blocked) "Lời nhắc đang bị chặn" else if (!enabled) "Lời nhắc đang tắt" else "Lần nhắc tiếp theo: ${nextReminderLabel(nextAt)}", 14, muted = true)
                    }
                }
                if (enabled && availability.notifications && !availability.exact) ExactAlarmNotice()
                availability.error?.let { Label(it, 13, color = MaterialTheme.colorScheme.error) }
                ReminderHeading("TẦN SUẤT LẶP LẠI", 15, muted = true)
                if (repeat == "ONCE") Label("Một lần · ${displayDate(reminder.scheduledDate ?: "")}", 14, true)
                Surface(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("DAILY" to "Hằng ngày", "WEEKLY" to "Theo tuần").forEach { (value, label) ->
                            Box(Modifier.weight(1f).background(if (repeat == value) accent else Color.Transparent, RoundedCornerShape(12.dp))
                                .selectable(repeat == value, onClick = { repeat = value; edited = true }, role = Role.RadioButton).padding(vertical = 12.dp), Alignment.Center) {
                                Label(label, 14, true, color = if (repeat == value) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                ReminderHeading("THỜI GIAN NHẮC", 15, muted = true)
                Panel(padding = 20, radius = 16) {
                    Text(time.take(5), fontFamily = ReminderHeadingFont, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.testTag("reminder/time"))
                    Spacer(Modifier.height(12.dp))
                    Surface(color = accent, shape = RoundedCornerShape(999.dp)) {
                        Row(Modifier.clickable { picker = true }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FigmaIcon("edit.svg", Modifier.size(16.dp)); Label("Thay đổi", 13, true, color = Color.White)
                        }
                    }
                }
                if (repeat == "WEEKLY") {
                    ReminderHeading("CHỌN NGÀY TRONG TUẦN", 15, muted = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..7).forEach { day ->
                            val selected = day in reminderDays(days)
                            Box(Modifier.weight(1f).aspectRatio(1f).background(if (selected) accent else Color(0xFF2B2930), RoundedCornerShape(50))
                                .selectable(selected, onClick = {
                                    val current = reminderDays(days)
                                    days = (if (selected) current - day else current + day).sorted().joinToString(","); edited = true
                                }, role = Role.Checkbox).testTag("day/$day"), Alignment.Center) {
                                Label(if (day == 7) "CN" else "T${day + 1}", 14, true, color = if (selected) Color.White else Color(0xFF9E9EAF))
                            }
                        }
                    }
                }
                if (attempted && validation != null) Label(validation, 13, color = MaterialTheme.colorScheme.error)
            }
        }
        if (picker) ReminderTimeSheet(time, accent, { picker = false }) { time = it; edited = true; picker = false }
    }
}

@Composable private fun ReminderHeading(text: String, size: Int, muted: Boolean = false) {
    Text(text, fontFamily = ReminderHeadingFont, fontWeight = FontWeight.Bold, fontSize = size.sp,
        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun ReminderTimeSheet(time: String, accent: Color, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var hour by rememberSaveable { mutableStateOf(time.take(2)) }
    var minute by rememberSaveable { mutableStateOf(time.drop(3).take(2)) }
    val valid = hour.toIntOrNull() in 0..23 && minute.toIntOrNull() in 0..59
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Label("Chọn thời gian", 20, true)
            Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField("Giờ", hour, 23) { hour = it }
                Label(":", 36, true)
                TimeField("Phút", minute, 59) { minute = it }
            }
            if (!valid) Label("Nhập giờ 00–23 và phút 00–59.", 13, color = MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onDismiss, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Hủy") }
                Box(Modifier.weight(1f)) { PrimaryButton("Xác nhận", enabled = valid, color = accent) { onConfirm("%02d:%02d:00".format(hour.toInt(), minute.toInt())) } }
            }
        }
    }
}

@Composable private fun TimeField(label: String, value: String, maximum: Int, onChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val accent = Color(0xFF1E3A8A)
    val invalid = value.toIntOrNull() !in 0..maximum
    val border = if (invalid) MaterialTheme.colorScheme.error else if (focused) accent else MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.width(100.dp).height(80.dp).border(1.dp, border, RoundedCornerShape(12.dp))
        .background(if (focused) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
        .padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BasicTextField(value, { if (it.length <= 2 && it.all { c -> c in '0'..'9' }) onChange(it) },
            Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.testTag("time/$label").semantics { contentDescription = label },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), cursorBrush = SolidColor(accent),
            textStyle = LocalTextStyle.current.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                color = if (focused) accent else MaterialTheme.colorScheme.onSurface))
        Label(label, 12, muted = true)
    }
}
