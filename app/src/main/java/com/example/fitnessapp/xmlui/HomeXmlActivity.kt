package com.example.fitnessapp.xmlui

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.fitnessapp.R
import com.example.fitnessapp.data.ReminderScheduler
import com.example.fitnessapp.model.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Điều khiển Trang chủ XML; chỉ mở module đã hoàn thiện. */
open class HomeXmlActivity : ComponentActivity() {
    private lateinit var model: HomeXmlModel
    private var displayedTheme: Boolean? = null
    private var pendingScroll: Int? = null
    private val screens = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) result.data?.getStringExtra("tab")?.let { navigate(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ExerciseXml)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        pendingScroll = savedInstanceState?.getInt("homeScroll")
        model = ViewModelProvider(this)[HomeXmlModel::class.java]
        showLayout(model.state.value.data?.dark ?: false)
        lifecycleScope.launch { model.state.collect { render(it) } }
        if (savedInstanceState == null && intent.getBooleanExtra("reminder", false)) {
            intent.removeExtra("reminder")
            openReminder()
        }
    }

    override fun onResume() {
        super.onResume()
        model.refresh()
        ReminderScheduler.restore(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("reminder", false)) {
            intent.removeExtra("reminder")
            openReminder()
        }
    }

    private fun navigate(tab: String) {
        val dark = model.state.value.data?.dark ?: false
        when (tab) {
            "EXERCISES" -> screens.launch(Intent(this, ExerciseXmlActivity::class.java).putExtra("dark", dark))
            "STATS" -> screens.launch(Intent(this, StatisticsReminderXmlActivity::class.java))
        }
    }

    private fun openReminder() {
        screens.launch(Intent(this, StatisticsReminderXmlActivity::class.java).putExtra("reminder", true))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("homeScroll", findViewById<ScrollView>(R.id.home_scroll).scrollY)
        super.onSaveInstanceState(outState)
    }

    private fun showLayout(dark: Boolean) {
        displayedTheme = dark
        setTheme(if (dark) R.style.Theme_ExerciseXml_Dark else R.style.Theme_ExerciseXml)
        setContentView(R.layout.activity_home_xml)
        val root = findViewById<View>(R.id.home_root)
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            findViewById<View>(R.id.home_bottom_inset).apply {
                layoutParams = layoutParams.apply { height = bars.bottom }
            }
            insets
        }
        XmlNavigation.bind(this, "HOME", dark, ::navigate)
        findViewById<View>(R.id.home_reminder_edit).setOnClickListener { openReminder() }
        // TODO: Trang này chưa phát triển. Mở từng chức năng sau khi chuyển sang XML và kiểm thử.
        listOf(R.id.home_record, R.id.home_history, R.id.home_recent).forEach { id ->
            findViewById<View>(id).setOnClickListener { FeatureAvailability.showUnavailable(this) }
        }
        findViewById<View>(R.id.home_retry).setOnClickListener { model.refresh() }
    }

    private fun text(id: Int, value: String) { findViewById<TextView>(id).text = value }

    private fun render(state: HomeXmlState) {
        val data = state.data
        if (data != null && displayedTheme != data.dark) {
            if (pendingScroll == null) pendingScroll = findViewById<ScrollView>(R.id.home_scroll).scrollY
            showLayout(data.dark)
        }
        findViewById<View>(R.id.home_loading).visibility = if (data == null && state.error == null) View.VISIBLE else View.GONE
        findViewById<View>(R.id.home_error).visibility = if (state.error != null) View.VISIBLE else View.GONE
        findViewById<View>(R.id.home_scroll).visibility = if (data != null) View.VISIBLE else View.INVISIBLE
        state.error?.let { text(R.id.home_error_text, it) }
        if (data == null) return
        val today = LocalDate.now()
        val stats = FitnessRules.monthStats(data.workouts, YearMonth.from(today))
        text(R.id.home_date, today.format(DateTimeFormatter.ofPattern("EEEE, d 'tháng' M", Locale.forLanguageTag("vi-VN"))).replaceFirstChar { it.uppercase() })
        text(R.id.home_month, "Tháng ${today.monthValue} của bạn")
        text(R.id.home_workouts, stats.totalWorkouts.toString().padStart(2, '0'))
        text(R.id.home_minutes, stats.totalMinutes.toString().padStart(2, '0'))
        text(R.id.home_completed, stats.completedSets.toString().padStart(2, '0'))
        val recent = data.workouts.firstOrNull()
        text(R.id.home_recent_date, if (recent == null) "Chưa có buổi tập" else if (recent.workoutDate == today.toString()) "Hôm nay" else displayDate(recent.workoutDate))
        text(R.id.home_recent_names, recent?.exerciseNames?.joinToString(", ") ?: "Ghi nhận buổi tập đầu tiên của bạn.")
        text(R.id.home_recent_summary, recent?.let { "${it.durationMinutes} phút   ·   ${it.items.size} lượt bài hoàn thành" } ?: "")
        findViewById<View>(R.id.home_recent_summary).visibility = if (recent == null) View.GONE else View.VISIBLE
        findViewById<View>(R.id.home_recent_detail).visibility = if (recent == null) View.GONE else View.VISIBLE
        val reminder = data.reminder
        text(R.id.home_reminder_schedule, if (reminder.id == 0L) "Chưa thiết lập" else "${reminder.reminderTime.take(5)} · " + when (reminder.repeatType) {
            "WEEKLY" -> "Theo tuần (${reminderDays(reminder.repeatDays).joinToString(", ") { if (it == 7) "CN" else "T${it + 1}" }})"
            "ONCE" -> displayDate(reminder.scheduledDate ?: "")
            else -> "Hằng ngày"
        })
        text(R.id.home_reminder_status, if (!reminder.isEnabled) "Đang tắt" else if (!ReminderScheduler.permitted(this)) "Chưa được cấp quyền" else "Đang bật")
        // Đợi dữ liệu và kích thước nội dung hoàn tất trước khi khôi phục vị trí cuộn.
        pendingScroll?.let { position ->
            findViewById<ScrollView>(R.id.home_scroll).post {
                findViewById<ScrollView>(R.id.home_scroll).scrollTo(0, position)
                pendingScroll = null
            }
        }
    }
}
