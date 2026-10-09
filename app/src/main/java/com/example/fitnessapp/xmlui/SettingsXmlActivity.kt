package com.example.fitnessapp.xmlui

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.PictureDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.caverock.androidsvg.SVG
import com.example.fitnessapp.R
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.ReminderScheduler
import com.example.fitnessapp.model.Reminder
import com.example.fitnessapp.model.reminderDays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Figma settings UI. Edits are a session preview; no database writes or alarms. */
class SettingsXmlActivity : ComponentActivity() {
    private var dark = false
    private var editing = false
    private var ready = false
    private var reminder = Reminder()
    private var draft = Reminder()
    private var picker: Dialog? = null
    private var scrollPosition = 0
    private var settingsScroll = 0
    private var pickerOpen = false
    private var pickerHour: String? = null
    private var pickerMinute: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_SettingsXml)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        dark = savedInstanceState?.getBoolean("dark") ?: intent.getBooleanExtra("dark", false)
        editing = savedInstanceState?.getBoolean("editing") ?: false
        ready = savedInstanceState?.getBoolean("ready") ?: false
        scrollPosition = savedInstanceState?.getInt("scroll") ?: 0
        settingsScroll = savedInstanceState?.getInt("settingsScroll") ?: 0
        pickerOpen = savedInstanceState?.getBoolean("picker") ?: false
        pickerHour = savedInstanceState?.getString("hour")
        pickerMinute = savedInstanceState?.getString("minute")
        savedInstanceState?.getBundle("reminder")?.let { reminder = readReminder(it) }
        savedInstanceState?.getBundle("draft")?.let { draft = readReminder(it) }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = back()
        })
        render()
        if (!ready) lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) { FitnessController(applicationContext).getPrimaryReminder() } }
                .onSuccess { reminder = it; draft = it; ready = true; render() }
                .onFailure {
                    android.app.AlertDialog.Builder(this@SettingsXmlActivity)
                        .setMessage("Không thể đọc cài đặt. Vui lòng mở lại màn hình.")
                        .setPositiveButton("Đóng") { _, _ -> finish() }.show()
                }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ready) { scrollPosition = findViewById<ScrollView>(R.id.settings_scroll).scrollY; render() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("dark", dark); outState.putBoolean("editing", editing); outState.putBoolean("ready", ready)
        outState.putInt("scroll", findViewById<ScrollView>(R.id.settings_scroll).scrollY)
        outState.putInt("settingsScroll", settingsScroll)
        outState.putBundle("reminder", reminder.bundle()); outState.putBundle("draft", draft.bundle())
        outState.putBoolean("picker", picker?.isShowing == true)
        outState.putString("hour", picker?.findViewById<EditText>(R.id.picker_hour)?.text?.toString())
        outState.putString("minute", picker?.findViewById<EditText>(R.id.picker_minute)?.text?.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() { picker?.dismiss(); super.onDestroy() }
    private fun Reminder.bundle() = Bundle().apply {
        putLong("id", id); putString("title", title); putString("message", message)
        putString("time", reminderTime); putString("repeat", repeatType); putBoolean("enabled", isEnabled)
        putString("days", repeatDays); putString("date", scheduledDate)
    }
    private fun readReminder(b: Bundle) = Reminder(b.getLong("id"), b.getString("title") ?: "Nhắc nhở tập luyện",
        b.getString("message"), b.getString("time") ?: "18:00:00", b.getString("repeat") ?: "DAILY",
        b.getBoolean("enabled"), b.getString("days"), b.getString("date"))
    private fun color(attr: Int) = TypedValue().also { theme.resolveAttribute(attr, it, true) }.data
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun text(id: Int, value: String) { findViewById<TextView>(id).text = value }
    private fun visible(id: Int, show: Boolean) { findViewById<View>(id).visibility = if (show) View.VISIBLE else View.GONE }
    private fun click(id: Int, action: () -> Unit) { findViewById<View>(id).setOnClickListener { action() } }
    private fun asset(id: Int, name: String) {
        findViewById<ImageView>(id).apply {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            setImageDrawable(assets.open("figma/settings/$name.svg").use { PictureDrawable(SVG.getFromInputStream(it).renderToPicture()) })
        }
    }
    private fun render() {
        setTheme(if (dark) R.style.Theme_SettingsXml_Dark else R.style.Theme_SettingsXml)
        setContentView(if (editing) R.layout.activity_reminder_settings_xml else R.layout.activity_settings_xml)
        val root = findViewById<View>(R.id.settings_root)
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            findViewById<View>(R.id.settings_inset).layoutParams = findViewById<View>(R.id.settings_inset).layoutParams.apply {
                height = maxOf(bars.bottom, keyboard.bottom)
            }
            insets
        }
        if (editing) renderReminder() else renderSettings()
        findViewById<ScrollView>(R.id.settings_scroll).post {
            findViewById<ScrollView>(R.id.settings_scroll).scrollTo(0, scrollPosition)
            if (pickerOpen && editing && picker?.isShowing != true) showTimePicker()
        }
    }
    private fun nextLabel(r: Reminder): String {
        val next = ReminderScheduler.next(r) ?: return "Chưa có lịch nhắc"
        val date = when (next.toLocalDate()) {
            LocalDate.now() -> "Hôm nay"
            LocalDate.now().plusDays(1) -> "Ngày mai"
            else -> if (next.dayOfWeek.value == 7) "Chủ nhật" else "Thứ ${next.dayOfWeek.value + 1}"
        }
        return "$date, ${next.format(DateTimeFormatter.ofPattern("HH:mm"))}"
    }
    private fun scheduleLabel(r: Reminder): String = when (r.repeatType) {
        "WEEKLY" -> reminderDays(r.repeatDays).joinToString(", ") { if (it == 7) "CN" else "T${it + 1}" } + " lúc ${r.reminderTime.take(5)}"
        "ONCE" -> "${r.scheduledDate.orEmpty()} · ${r.reminderTime.take(5)}"
        else -> "Hằng ngày lúc ${r.reminderTime.take(5)}"
    }
    private fun renderSettings() {
        val configured = reminder.id != 0L
        val blocked = configured && reminder.isEnabled && !ReminderScheduler.permitted(this)
        val prefix = when { blocked -> "settings_blocked"; !configured || !reminder.isEnabled -> "settings_empty"; dark -> "settings_dark"; else -> "settings_light" }
        val themed = if (dark) "settings_dark" else "settings_light"
        asset(R.id.settings_back, "${themed}_backbtn")
        asset(R.id.settings_dot, "${prefix}_ellipse")
        asset(R.id.settings_chevron, "${themed}_chevron")
        asset(R.id.settings_system_chevron, "${themed}_chevron")
        listOf(R.id.settings_line_schedule, R.id.settings_line_next, R.id.settings_line_system, R.id.settings_line_setup).forEach { asset(it, "${themed}_line") }
        text(R.id.settings_status, when { !ready -> "Đang tải…"; !configured -> "Chưa thiết lập"; blocked -> "Chưa hoạt động"; reminder.isEnabled -> "Đang bật"; else -> "Đang tắt" })
        text(R.id.settings_schedule, scheduleLabel(reminder)); text(R.id.settings_next, nextLabel(reminder))
        visible(R.id.settings_schedule_group, configured)
        visible(R.id.settings_next_group, configured && reminder.isEnabled && !blocked)
        visible(R.id.settings_warning, blocked); visible(R.id.settings_system_group, blocked)
        listOf(R.id.settings_light to !dark, R.id.settings_dark to dark).forEach { (id, selected) ->
            findViewById<TextView>(id).apply {
                isSelected = selected
                setBackgroundResource(if (selected) R.drawable.settings_selected else R.drawable.settings_option)
                setTextColor(color(if (selected) R.attr.settingsOnSelected else R.attr.settingsMuted))
                setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            }
        }
        click(R.id.settings_light) { changeTheme(false) }; click(R.id.settings_dark) { changeTheme(true) }
        click(R.id.settings_back) { back() }; click(R.id.settings_system) { openSystemSettings() }
        findViewById<View>(R.id.settings_setup).isEnabled = ready
        click(R.id.settings_setup) {
            settingsScroll = findViewById<ScrollView>(R.id.settings_scroll).scrollY
            draft = reminder; editing = true; scrollPosition = 0; render()
        }
        XmlNavigation.bind(this, "SETTINGS", dark) { tab -> setResult(RESULT_OK, Intent().putExtra("tab", tab)); finish() }
        // Reuse navigation wiring, with the exact E01 icon assets and geometry.
        val icons = listOf(R.id.ex_icon_HOME, R.id.ex_icon_EXERCISES, R.id.ex_icon_HISTORY, R.id.ex_icon_STATS, R.id.ex_icon_SETTINGS)
        val labels = listOf(R.id.ex_tab_label_HOME, R.id.ex_tab_label_EXERCISES, R.id.ex_tab_label_HISTORY, R.id.ex_tab_label_STATS, R.id.ex_tab_label_SETTINGS)
        icons.zip(listOf("house", "dumbbell", "clock", "barchart", "cog")).forEach { (id, name) ->
            asset(id, "${themed}_$name")
            findViewById<ImageView>(id).layoutParams = findViewById<ImageView>(id).layoutParams.apply { width = dp(22); height = dp(22) }
        }
        labels.forEach { id -> findViewById<TextView>(id).apply { textSize = 10f; includeFontPadding = false; setLineHeight(dp(12)); setTextColor(color(if (id == R.id.ex_tab_label_SETTINGS) R.attr.settingsOnSelected else R.attr.settingsMuted)) } }
        val nav = findViewById<View>(R.id.ex_tab_HOME).parent as View
        nav.setPadding(dp(8), 0, dp(8), 0)
        nav.setBackgroundColor(color(if (dark) R.attr.exBackground else R.attr.exSurface))
        findViewById<View>(R.id.settings_inset).setBackgroundColor(color(if (dark) R.attr.exBackground else R.attr.exSurface))
        findViewById<ImageView>(R.id.ex_icon_SETTINGS).apply {
            layoutParams = layoutParams.apply { width = dp(54); height = dp(30) }
            setPadding(dp(16), dp(4), dp(16), dp(4))
            if (dark) setBackgroundResource(R.drawable.settings_selected)
        }
    }
    private fun changeTheme(value: Boolean) {
        if (dark == value) return
        scrollPosition = findViewById<ScrollView>(R.id.settings_scroll).scrollY
        dark = value; render()
    }
    private fun renderReminder() {
        asset(R.id.reminder_back, "reminder_arrowleft")
        asset(R.id.reminder_clock, "reminder_clock"); asset(R.id.reminder_edit_icon, "reminder_edit")
        asset(R.id.reminder_warning_icon, "permission_alerttriangle")
        val blocked = draft.isEnabled && !ReminderScheduler.permitted(this)
        visible(R.id.reminder_permission, blocked)
        visible(R.id.reminder_banner, blocked)
        asset(R.id.reminder_banner_icon, "permission_circlealert")
        text(R.id.reminder_status, if (blocked) "Chưa hoạt động" else if (draft.isEnabled) "Đang bật" else "Đang tắt")
        findViewById<TextView>(R.id.reminder_status).setTextColor(if (blocked) Color.parseColor("#C2410C") else if (dark) color(R.attr.exAccent) else Color.parseColor("#1E3A8A"))
        if (dark) findViewById<ImageView>(R.id.reminder_back).setColorFilter(color(R.attr.exText))
        asset(R.id.reminder_toggle, if (draft.isEnabled) "reminder_switch" else "permission_stateoff")
        findViewById<View>(R.id.reminder_toggle).apply {
            isSelected = draft.isEnabled; contentDescription = if (draft.isEnabled) "Tắt nhắc nhở" else "Bật nhắc nhở"
        }
        text(R.id.reminder_next, "Lần nhắc tiếp theo: ${nextLabel(draft)}")
        visible(R.id.reminder_next_card, !blocked)
        val weekly = draft.repeatType == "WEEKLY"
        visible(R.id.reminder_days, weekly)
        listOf(R.id.reminder_daily to !weekly, R.id.reminder_weekly to weekly).forEach { (id, selected) ->
            findViewById<TextView>(id).apply {
                isSelected = selected
                setBackgroundResource(if (selected) R.drawable.settings_segment else android.R.color.transparent)
                setTextColor(if (selected) Color.WHITE else color(R.attr.settingsMuted))
            }
        }
        text(R.id.reminder_time, draft.reminderTime.take(5))
        text(R.id.reminder_period, if ((draft.reminderTime.take(2).toIntOrNull() ?: 0) >= 12) "PM" else "AM")
        val dayIds = listOf(R.id.reminder_day_1, R.id.reminder_day_2, R.id.reminder_day_3, R.id.reminder_day_4, R.id.reminder_day_5, R.id.reminder_day_6, R.id.reminder_day_7)
        dayIds.forEachIndexed { i, id ->
            val selected = i + 1 in reminderDays(draft.repeatDays)
            findViewById<TextView>(id).apply {
                isSelected = selected; setBackgroundResource(if (selected) R.drawable.settings_primary else R.drawable.settings_day)
                setTextColor(if (selected) Color.WHITE else Color.parseColor("#9E9EAF"))
                setTypeface(null, Typeface.BOLD)
                contentDescription = (if (i == 6) "Chủ nhật" else "Thứ ${i + 2}") + if (selected) ", đã chọn" else ", chưa chọn"
            }
            click(id) {
                val days = reminderDays(draft.repeatDays).toMutableSet()
                if (!days.add(i + 1)) days.remove(i + 1)
                updateDraft(draft.copy(repeatDays = days.sorted().joinToString(",")))
            }
        }
        click(R.id.reminder_back) { back() }; click(R.id.reminder_system) { openSystemSettings() }
        click(R.id.reminder_toggle) { updateDraft(draft.copy(isEnabled = !draft.isEnabled)) }
        click(R.id.reminder_daily) { updateDraft(draft.copy(repeatType = "DAILY")) }
        click(R.id.reminder_weekly) { updateDraft(draft.copy(repeatType = "WEEKLY")) }
        click(R.id.reminder_edit) { showTimePicker() }
        click(R.id.reminder_save) {
            if (weekly && reminderDays(draft.repeatDays).isEmpty()) {
                Toast.makeText(this, "Chọn ít nhất một ngày trong tuần.", Toast.LENGTH_SHORT).show()
            } else {
                reminder = draft.copy(id = if (draft.id == 0L) -1 else draft.id)
                editing = false; scrollPosition = settingsScroll; render()
                Toast.makeText(this, "Đã cập nhật bản xem thử. Chưa lưu lịch nhắc vào thiết bị.", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun updateDraft(value: Reminder) {
        draft = value
        scrollPosition = findViewById<ScrollView>(R.id.settings_scroll).scrollY
        render()
    }
    private fun back() {
        if (editing) { editing = false; scrollPosition = settingsScroll; render() }
        else { setResult(RESULT_OK, Intent().putExtra("tab", "HOME")); finish() }
    }
    private fun openSystemSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }
    private fun showTimePicker() {
        if (picker?.isShowing == true) return
        pickerOpen = true
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.settings_time_picker)
        val hour = dialog.findViewById<EditText>(R.id.picker_hour)
        val minute = dialog.findViewById<EditText>(R.id.picker_minute)
        hour.doAfterTextChanged { hour.error = null }
        minute.doAfterTextChanged { minute.error = null }
        hour.setText(pickerHour ?: draft.reminderTime.take(2)); minute.setText(pickerMinute ?: draft.reminderTime.substring(3, 5))
        dialog.findViewById<View>(R.id.picker_cancel).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.picker_confirm).setOnClickListener {
            val h = hour.text.toString().toIntOrNull(); val m = minute.text.toString().toIntOrNull()
            if (h == null || h !in 0..23) { hour.error = "Nhập giờ từ 00 đến 23"; return@setOnClickListener }
            if (m == null || m !in 0..59) { minute.error = "Nhập phút từ 00 đến 59"; return@setOnClickListener }
            dialog.dismiss()
            updateDraft(draft.copy(reminderTime = "%02d:%02d:00".format(java.util.Locale.ROOT, h, m)))
        }
        dialog.setOnDismissListener { pickerOpen = false; pickerHour = null; pickerMinute = null; picker = null }
        picker = dialog
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)); setGravity(Gravity.BOTTOM)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
