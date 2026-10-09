package com.example.fitnessapp.xmlui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.R
import com.example.fitnessapp.controller.FitnessController
import java.time.YearMonth

/**
 * Activity màn hình Lịch sử tập luyện (Hình 1 khi tháng trống, Hình 2 khi có buổi tập).
 */
class WorkoutHistoryXmlActivity : ComponentActivity() {

    private lateinit var controller: FitnessController
    private var dark: Boolean = false
    private var currentMonth: YearMonth = YearMonth.now()

    private lateinit var monthText: TextView
    private lateinit var emptyLayout: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WorkoutHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        controller = FitnessController(this)
        dark = controller.getState("dark_theme") == "true"
        setTheme(if (dark) R.style.Theme_ExerciseXml_Dark else R.style.Theme_ExerciseXml)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_workout_history_xml)

        val root = findViewById<View>(R.id.history_root)
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            findViewById<View>(R.id.history_bottom_inset).apply {
                layoutParams = layoutParams.apply { height = bars.bottom }
            }
            insets
        }

        monthText = findViewById(R.id.history_month_text)
        emptyLayout = findViewById(R.id.history_empty_layout)
        recyclerView = findViewById(R.id.history_recycler_view)

        // Nút + Ghi nhận ở góc phải
        findViewById<View>(R.id.history_btn_record).setOnClickListener {
            val intent = Intent(this, RecordWorkoutXmlActivity::class.java).apply {
                putExtra("dark", dark)
            }
            startActivity(intent)
        }

        // Nút chuyển tháng bằng mũi tên
        findViewById<View>(R.id.history_btn_prev_month).setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            loadHistoryData()
        }

        findViewById<View>(R.id.history_btn_next_month).setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            loadHistoryData()
        }

        // Bấm vào tháng để mở lịch/dialog chọn tháng nhanh (12 tháng + chuyển năm)
        findViewById<View>(R.id.history_month_picker_btn).setOnClickListener {
            showMonthPickerDialog()
        }
        monthText.setOnClickListener {
            showMonthPickerDialog()
        }

        // Khởi tạo RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = WorkoutHistoryAdapter { workoutId ->
            val intent = Intent(this, WorkoutDetailXmlActivity::class.java).apply {
                putExtra("workout_id", workoutId)
                putExtra("dark", dark)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Gắn Bottom Navigation Bar
        XmlNavigation.bind(this, "HISTORY", dark) { tab ->
            when (tab) {
                "HOME" -> {
                    startActivity(Intent(this, HomeXmlActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                }
                "EXERCISES" -> {
                    startActivity(Intent(this, ExerciseXmlActivity::class.java).apply {
                        putExtra("route", "list")
                    })
                    finish()
                }
                "HISTORY" -> {
                    // Đang ở màn hình này
                }
                else -> FeatureAvailability.showUnavailable(this)
            }
        }
    }

    /**
     * Mở Dialog chọn tháng/năm nhanh dạng lưới trực quan
     */
    private fun showMonthPickerDialog() {
        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_month_picker_xml, null)

        var pickerYear = currentMonth.year
        val yearText = view.findViewById<TextView>(R.id.picker_year_text)
        val btnPrevYear = view.findViewById<View>(R.id.picker_btn_prev_year)
        val btnNextYear = view.findViewById<View>(R.id.picker_btn_next_year)

        val monthButtons = listOf(
            view.findViewById<TextView>(R.id.picker_month_1),
            view.findViewById<TextView>(R.id.picker_month_2),
            view.findViewById<TextView>(R.id.picker_month_3),
            view.findViewById<TextView>(R.id.picker_month_4),
            view.findViewById<TextView>(R.id.picker_month_5),
            view.findViewById<TextView>(R.id.picker_month_6),
            view.findViewById<TextView>(R.id.picker_month_7),
            view.findViewById<TextView>(R.id.picker_month_8),
            view.findViewById<TextView>(R.id.picker_month_9),
            view.findViewById<TextView>(R.id.picker_month_10),
            view.findViewById<TextView>(R.id.picker_month_11),
            view.findViewById<TextView>(R.id.picker_month_12)
        )

        val primaryColor = color(R.attr.exPrimary)
        val textColor = color(R.attr.exText)

        fun updateUI() {
            yearText.text = pickerYear.toString()
            monthButtons.forEachIndexed { index, tv ->
                val month = index + 1
                val isSelected = (pickerYear == currentMonth.year && month == currentMonth.monthValue)
                if (isSelected) {
                    tv.setBackgroundResource(R.drawable.ex_chip_month_selected)
                    tv.setTextColor(textColor)
                    tv.setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    tv.setBackgroundResource(R.drawable.ex_chip_unselected)
                    tv.setTextColor(textColor)
                    tv.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
            }
        }

        btnPrevYear.setOnClickListener {
            pickerYear--
            updateUI()
        }

        btnNextYear.setOnClickListener {
            pickerYear++
            updateUI()
        }

        monthButtons.forEachIndexed { index, tv ->
            val month = index + 1
            tv.setOnClickListener {
                currentMonth = YearMonth.of(pickerYear, month)
                loadHistoryData()
                dialog.dismiss()
            }
        }

        view.findViewById<View>(R.id.picker_btn_current_month).setOnClickListener {
            currentMonth = YearMonth.now()
            loadHistoryData()
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.picker_btn_close).setOnClickListener {
            dialog.dismiss()
        }

        updateUI()

        dialog.setContentView(view)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.45f)
        }
        dialog.show()
        dialog.window?.setLayout(
            minOf(resources.displayMetrics.widthPixels - dp(48), dp(400)),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun color(attr: Int): Int =
        android.util.TypedValue().also { theme.resolveAttribute(attr, it, true) }.data

    private fun dp(value: Int): Int =
        android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()

    override fun onResume() {
        super.onResume()
        loadHistoryData()
    }

    private fun loadHistoryData() {
        // Cập nhật text tháng: Tháng MM/yyyy
        val monthStr = currentMonth.monthValue.toString().padStart(2, '0')
        monthText.text = "Tháng $monthStr/${currentMonth.year}"

        // Lấy danh sách buổi tập từ database và lọc theo tháng hiện tại
        val allWorkouts = controller.getAllWorkouts()
        val monthPrefix = currentMonth.toString() // dạng "2026-09"
        val workoutsInMonth = allWorkouts.filter { it.workoutDate.startsWith(monthPrefix) }

        if (workoutsInMonth.isEmpty()) {
            // Hình 1: Tháng này chưa có buổi tập
            emptyLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            // Hình 2: Đã có buổi tập, gom nhóm theo ngày
            emptyLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            val groups = workoutsInMonth.groupBy { it.workoutDate }
            val items = ArrayList<HistoryItem>()

            // Sắp xếp ngày giảm dần
            groups.toSortedMap(compareByDescending { it }).forEach { (date, sessions) ->
                items.add(HistoryItem.DateHeader(date))
                sessions.forEach { workout ->
                    items.add(HistoryItem.WorkoutCard(workout))
                }
            }

            adapter.submitItems(items)
        }
    }
}
