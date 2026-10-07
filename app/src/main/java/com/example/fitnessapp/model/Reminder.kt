package com.example.fitnessapp.model

/**
 * [MODEL] Đại diện cho lời nhắc nhở tập luyện (bảng `reminders` trong SQLite)
 */
data class Reminder(
    val id: Long = 0,
    val title: String = "Nhắc nhở tập luyện",
    val message: String? = null,
    val reminderTime: String = "18:00:00",
    val repeatType: String = "DAILY",
    val isEnabled: Boolean = false,
    val repeatDays: String? = null,
    val scheduledDate: String? = null
)

fun reminderDays(raw: String?): List<Int> = raw?.split(",")?.mapNotNull {
    it.trim().toIntOrNull() ?: mapOf("MON" to 1, "TUE" to 2, "WED" to 3, "THU" to 4, "FRI" to 5, "SAT" to 6, "SUN" to 7)[it.trim().uppercase()]
}?.filter { it in 1..7 }?.distinct().orEmpty()
