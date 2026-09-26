package com.example.fitnessapp.model

/**
 * [MODEL] Đại diện cho thực thể Bài tập (bảng `exercises` trong SQLite)
 */
data class Exercise(
    val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val trackingType: String = "REPS",
    val defaultSets: Int = 3,
    val defaultReps: Int = 10,
    val defaultDurationSeconds: Int? = null,
    val instructionImage: String? = null,
    val instructionVideo: String? = null,
    val description: String? = null
)
