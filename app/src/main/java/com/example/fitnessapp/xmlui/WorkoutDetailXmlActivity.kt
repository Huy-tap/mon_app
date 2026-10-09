package com.example.fitnessapp.xmlui

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fitnessapp.R
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.model.displayDate
import com.example.fitnessapp.model.durationLabel
import com.example.fitnessapp.model.muscleLabel

/**
 * Activity màn hình Chi tiết buổi tập (Hình 3).
 */
class WorkoutDetailXmlActivity : ComponentActivity() {

    private lateinit var controller: FitnessController

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppTheme.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val dark = AppTheme.isDark(this)
        setTheme(if (dark) R.style.Theme_ExerciseXml_Dark else R.style.Theme_ExerciseXml)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_workout_detail_xml)

        controller = FitnessController(this)

        val root = findViewById<View>(R.id.detail_root)
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            findViewById<View>(R.id.detail_bottom_inset).apply {
                layoutParams = layoutParams.apply { height = bars.bottom }
            }
            insets
        }

        // Nút quay lại ‹
        findViewById<View>(R.id.detail_back).setOnClickListener {
            finish()
        }

        val workoutId = intent.getLongExtra("workout_id", 0L)
        val workout = controller.getWorkoutDetail(workoutId)

        if (workout == null) {
            finish()
            return
        }

        // Tóm tắt buổi tập
        findViewById<TextView>(R.id.detail_workout_date).text = displayDate(workout.workoutDate)
        findViewById<TextView>(R.id.detail_workout_summary).text =
            "${workout.items.size} bài · ${workout.durationMinutes} phút"

        // Danh sách chi tiết từng bài tập
        val exercisesContainer = findViewById<LinearLayout>(R.id.detail_exercises_container)
        exercisesContainer.removeAllViews()

        val inflater = LayoutInflater.from(this)
        val mutedColor = TypedValue().also { theme.resolveAttribute(R.attr.exMuted, it, true) }.data

        workout.items.forEach { item ->
            val cardView = inflater.inflate(R.layout.item_workout_detail_exercise_xml, exercisesContainer, false)

            cardView.findViewById<TextView>(R.id.detail_item_name).text = item.exerciseName
            cardView.findViewById<TextView>(R.id.detail_item_muscle).text = muscleLabel(item.muscle)

            // Danh sách các hiệp tập
            val setsContainer = cardView.findViewById<LinearLayout>(R.id.detail_item_sets_container)
            setsContainer.removeAllViews()

            item.sets.forEach { set ->
                val setText = TextView(this).apply {
                    val unitStr = set.reps?.let { "$it lần" } ?: "${set.durationSeconds ?: 0} giây"
                    text = "Hiệp ${set.setNumber}: $unitStr"
                    textSize = 13f
                    setPadding(0, dp(2), 0, dp(2))
                }
                setsContainer.addView(setText)
            }

            // Thời lượng
            val durationText = cardView.findViewById<TextView>(R.id.detail_item_duration)
            if (item.durationSeconds > 0) {
                durationText.visibility = View.VISIBLE
                durationText.text = durationLabel(item.durationSeconds)
            } else {
                durationText.visibility = View.GONE
            }

            // Ghi chú nếu có
            val noteText = cardView.findViewById<TextView>(R.id.detail_item_note)
            if (!item.notes.isNullOrBlank()) {
                noteText.visibility = View.VISIBLE
                noteText.text = "Ghi chú: ${item.notes}"
            } else {
                noteText.visibility = View.GONE
            }

            exercisesContainer.addView(cardView)
        }
    }

    override fun onResume() {
        super.onResume()
        if (AppTheme.needsRefresh(this)) recreate()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}
