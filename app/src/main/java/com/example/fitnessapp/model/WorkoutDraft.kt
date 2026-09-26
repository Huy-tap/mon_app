package com.example.fitnessapp.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class WorkoutDraft(val date: String = LocalDate.now().toString(), val entries: List<DraftEntry> = emptyList())
data class DraftEntry(
    val exerciseId: Long, val name: String, val muscle: String,
    val values: List<Int>, val durationSeconds: Int, val note: String = "",
    val trackingType: String = "REPS"
)

object FitnessRules {
    fun exerciseError(e: Exercise): String? = when {
        e.name.trim().length !in 1..100 -> "Tên bài tập phải có 1–100 ký tự."
        e.muscleGroup.isBlank() -> "Vui lòng chọn nhóm cơ."
        e.defaultSets !in 1..100 -> "Số hiệp phải từ 1 đến 100."
        e.trackingType == "REPS" && e.defaultReps !in 1..1000 -> "Số lần phải từ 1 đến 1000."
        e.trackingType == "TIME" && (e.defaultDurationSeconds ?: 0) !in 1..86400 -> "Số giây phải từ 1 đến 86400."
        e.trackingType !in listOf("REPS", "TIME") -> "Kiểu theo dõi không hợp lệ."
        else -> null
    }
    fun entryError(e: DraftEntry): String? = when {
        e.exerciseId <= 0 -> "Không tìm thấy bài tập."
        e.values.size !in 1..100 -> "Số hiệp phải từ 1 đến 100."
        e.values.any { it !in 1..(if (e.trackingType == "TIME") 86400 else 1000) } -> "Vui lòng nhập kết quả hợp lệ cho từng hiệp."
        e.durationSeconds !in 1..86400 -> "Thời lượng phải lớn hơn 0 và không quá 24 giờ."
        else -> null
    }
    fun monthStats(workouts: List<Workout>, month: YearMonth): MonthlyStats {
        val selected = workouts.filter { it.workoutDate.startsWith(month.toString()) }
        return MonthlyStats(selected.size, selected.sumOf { it.durationMinutes }, selected.sumOf { w ->
            w.items.count { it.sets.isNotEmpty() && it.sets.all { s -> s.completed } }
        })
    }
    fun weeklyCounts(workouts: List<Workout>, month: YearMonth): List<Int> = (0..3).map { week ->
        workouts.count { it.workoutDate.startsWith(month.toString()) &&
            ((LocalDate.parse(it.workoutDate).dayOfMonth - 1) / 7).coerceAtMost(3) == week }
    }
}
fun displayDate(iso: String): String = runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(iso)
fun durationLabel(seconds: Int): String = when {
    seconds <= 0 -> "Chưa ghi thời lượng"
    seconds % 60 == 0 -> "${seconds / 60} phút"
    else -> "${seconds / 60} phút ${seconds % 60} giây"
}
fun muscleLabel(value: String): String = mapOf("Chest" to "Ngực", "Legs" to "Chân", "Abs" to "Bụng", "Core" to "Bụng", "Shoulders" to "Vai", "Back" to "Lưng", "Biceps" to "Tay trước", "Triceps" to "Tay sau", "Full Body" to "Toàn thân", "Cardio" to "Tim mạch")[value] ?: value
