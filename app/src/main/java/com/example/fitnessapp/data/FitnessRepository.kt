package com.example.fitnessapp.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.fitnessapp.model.Reminder
import com.example.fitnessapp.model.reminderDays
import java.time.*
import java.time.format.DateTimeFormatter

data class MonthlyStats(val sessions: Long, val minutes: Long, val completedExercises: Long)

/** Only the two modules' queries; other source tables are retained untouched. */
class FitnessRepository(private val db: SQLiteDatabase) {
    fun monthStats(month: YearMonth): MonthlyStats {
        val args = arrayOf(month.atDay(1).toString(), month.plusMonths(1).atDay(1).toString())
        // Aggregate workouts independently so duration is never multiplied by a join.
        val totals = db.rawQuery("SELECT COUNT(*), COALESCE(SUM(duration_minutes),0) FROM workouts WHERE workout_date >= ? AND workout_date < ?", args).use {
            it.moveToFirst(); it.getLong(0) to it.getLong(1)
        }
        val completed = db.rawQuery("""SELECT COUNT(*) FROM workout_exercises e
            JOIN workouts w ON w.workout_id = e.workout_id
            WHERE w.workout_date >= ? AND w.workout_date < ?
            AND EXISTS (SELECT 1 FROM workout_sets s WHERE s.workout_exercise_id=e.workout_exercise_id)
            AND NOT EXISTS (SELECT 1 FROM workout_sets s WHERE s.workout_exercise_id=e.workout_exercise_id AND s.completed != 1)""", args).use {
            it.moveToFirst(); it.getLong(0)
        }
        return MonthlyStats(totals.first, totals.second, completed)
    }
    /** Four columns in the design: days 1–7, 8–14, 15–21, and 22–end of month. */
    fun monthFrequency(month: YearMonth): List<Long> {
        val counts = MutableList(4) { 0L }
        db.rawQuery("""SELECT MIN((CAST(substr(workout_date,9,2) AS INTEGER)-1)/7,3), COUNT(*)
            FROM workouts WHERE workout_date >= ? AND workout_date < ? GROUP BY 1""",
            arrayOf(month.atDay(1).toString(), month.plusMonths(1).atDay(1).toString())).use { cursor ->
            while (cursor.moveToNext()) counts[cursor.getInt(0)] = cursor.getLong(1)
        }
        return counts
    }
    fun getPrimaryReminder(): Reminder = db.rawQuery("SELECT * FROM reminders ORDER BY reminder_id LIMIT 1", null).use { c ->
        fun text(n: String): String? = c.getColumnIndexOrThrow(n).let { if(c.isNull(it)) null else c.getString(it) }
        if (!c.moveToFirst()) Reminder() else Reminder(c.getLong(c.getColumnIndexOrThrow("reminder_id")), text("title")!!,
            text("message"), text("reminder_time")!!, text("repeat_type")!!,
            c.getInt(c.getColumnIndexOrThrow("is_enabled")) == 1, text("repeat_days"), text("scheduled_date"))
    }
    fun saveReminder(reminder: Reminder): Reminder {
        val time = LocalTime.parse(reminder.reminderTime)
        require(reminder.repeatType in listOf("DAILY", "WEEKLY", "ONCE"))
        val days = reminderDays(reminder.repeatDays)
        require(reminder.repeatType != "WEEKLY" || days.isNotEmpty()) { "Chọn ít nhất một ngày trong tuần." }
        if(reminder.repeatType == "ONCE") LocalDate.parse(reminder.scheduledDate)
        db.beginTransaction()
        try {
            // Re-read the primary key inside the transaction; never insert duplicates from a stale form.
            val primary = getPrimaryReminder()
            val normalized = reminder.copy(id=primary.id, reminderTime=time.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                repeatDays=if(reminder.repeatType=="WEEKLY") days.sorted().joinToString(",") else null,
                scheduledDate=if(reminder.repeatType=="ONCE") reminder.scheduledDate else null)
            val v=ContentValues().apply {
                put("title",normalized.title); put("message",normalized.message); put("reminder_time",normalized.reminderTime)
                put("repeat_type",normalized.repeatType); put("repeat_days",normalized.repeatDays); put("scheduled_date",normalized.scheduledDate)
                put("is_enabled",if(normalized.isEnabled) 1 else 0); put("updated_at",LocalDateTime.now().toString())
            }
            val id=if(primary.id==0L) db.insertOrThrow("reminders",null,v) else {
                check(db.update("reminders",v,"reminder_id=?",arrayOf(primary.id.toString()))==1);primary.id
            }
            db.setTransactionSuccessful()
            return normalized.copy(id=id)
        } finally { db.endTransaction() }
    }
}
