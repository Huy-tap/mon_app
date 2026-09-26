package com.example.fitnessapp.model

/**
 * [MODEL] Đại diện cho một Buổi tập (bảng `workouts` trong SQLite)
 */
data class Workout(
    val id: Long = 0,
    val workoutDate: String,
    val durationMinutes: Int = 0,
    val notes: String? = null,
    val exerciseNames: List<String> = emptyList(),
    val totalSets: Int = 0,
    val items: List<WorkoutExerciseItem> = emptyList()
)

/**
 * [MODEL] Đại diện cho bài tập được ghi trong buổi tập (bảng `workout_exercises`)
 */
data class WorkoutExerciseItem(
    val workoutExerciseId: Long = 0,
    val workoutId: Long = 0,
    val exerciseId: Long = 0,
    val exerciseName: String,
    val exerciseOrder: Int = 1,
    val muscle: String = "",
    val durationSeconds: Int = 0,
    val notes: String? = null,
    val sets: List<WorkoutSetItem> = emptyList()
)

/**
 * [MODEL] Đại diện cho từng hiệp tập (bảng `workout_sets`)
 */
data class WorkoutSetItem(
    val setId: Long = 0,
    val workoutExerciseId: Long = 0,
    val setNumber: Int = 1,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val weightKg: Double? = null,
    val completed: Boolean = true,
    val notes: String? = null
)
