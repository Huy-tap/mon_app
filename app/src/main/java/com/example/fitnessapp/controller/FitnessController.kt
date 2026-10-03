package com.example.fitnessapp.controller

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.fitnessapp.data.FitnessDatabase
import com.example.fitnessapp.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

/** SQLite is the single source for the catalog, history, reminders, preferences and draft. */
class FitnessController(private val database: SQLiteDatabase) {
    constructor(context: Context) : this(FitnessDatabase.open(context.applicationContext))
    private fun Cursor.text(name: String) = getString(getColumnIndexOrThrow(name))
    private fun Cursor.number(name: String) = getInt(getColumnIndexOrThrow(name))
    private fun Cursor.long(name: String) = getLong(getColumnIndexOrThrow(name))
    private fun Cursor.optionalInt(name: String): Int? = getColumnIndexOrThrow(name).let { if (isNull(it)) null else getInt(it) }
    private fun Cursor.exercise() = Exercise(long("exercise_id"), text("name"), text("muscle_group"), text("tracking_type"),
        number("default_sets"), number("default_reps"), optionalInt("default_duration_seconds"), text("instruction_image"), text("instruction_video"), text("description"))
    fun getAllExercises(): List<Exercise> = database.rawQuery("SELECT * FROM exercises WHERE is_archived = 0 ORDER BY exercise_id", null).use { c ->
        buildList { while (c.moveToNext()) add(c.exercise()) }
    }
    fun getExerciseById(id: Long): Exercise? = database.rawQuery("SELECT * FROM exercises WHERE exercise_id = ?", arrayOf(id.toString())).use {
        if (it.moveToFirst()) it.exercise() else null
    }
    private fun values(e: Exercise) = ContentValues().apply {
        put("name", e.name.trim()); put("muscle_group", e.muscleGroup); put("tracking_type", e.trackingType)
        put("default_sets", e.defaultSets)
        if (e.trackingType == "TIME") { putNull("default_reps"); put("default_duration_seconds", e.defaultDurationSeconds) }
        else { put("default_reps", e.defaultReps); putNull("default_duration_seconds") }
        put("instruction_image", e.instructionImage); put("instruction_video", e.instructionVideo); put("description", e.description)
        put("updated_at", java.time.LocalDateTime.now().toString())
    }
    fun insertExercise(e: Exercise): Long {
        require(FitnessRules.exerciseError(e) == null) { FitnessRules.exerciseError(e)!! }
        return database.insertOrThrow("exercises", null, values(e))
    }
    fun updateExercise(e: Exercise): Boolean {
        require(FitnessRules.exerciseError(e) == null) { FitnessRules.exerciseError(e)!! }
        return database.update("exercises", values(e), "exercise_id = ?", arrayOf(e.id.toString())) > 0
    }
    // Archiving preserves foreign keys and historical results, including renamed exercises.
    fun deleteExercise(id: Long): Boolean = database.update("exercises", ContentValues().apply { put("is_archived", 1) },
        "exercise_id = ?", arrayOf(id.toString())) > 0

    fun getAllWorkouts(): List<Workout> = database.rawQuery("SELECT workout_id FROM workouts ORDER BY workout_date DESC, workout_id DESC", null).use { c ->
        buildList { while (c.moveToNext()) getWorkoutDetail(c.getLong(0))?.let { add(it) } }
    }
    fun getRecentWorkout(): Workout? = getAllWorkouts().firstOrNull()
    fun getMonthlyStats(month: YearMonth = YearMonth.now()): MonthlyStats = FitnessRules.monthStats(getAllWorkouts(), month)
    fun getWorkoutDetail(id: Long): Workout? {
        val workout = database.rawQuery("SELECT * FROM workouts WHERE workout_id = ?", arrayOf(id.toString())).use { c ->
            if (!c.moveToFirst()) return null
            Workout(c.long("workout_id"), c.text("workout_date"), c.number("duration_minutes"), c.text("notes"))
        }
        val entries = database.rawQuery("SELECT * FROM workout_exercises WHERE workout_id = ? ORDER BY exercise_order", arrayOf(id.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val entryId = c.long("workout_exercise_id")
                    val sets = database.rawQuery("SELECT * FROM workout_sets WHERE workout_exercise_id = ? ORDER BY set_number", arrayOf(entryId.toString())).use { s ->
                        buildList { while (s.moveToNext()) add(WorkoutSetItem(
                            setId = s.long("workout_set_id"), workoutExerciseId = entryId, setNumber = s.number("set_number"),
                            reps = s.optionalInt("reps"), durationSeconds = s.optionalInt("duration_seconds"),
                            weightKg = s.getColumnIndexOrThrow("weight_kg").let { if (s.isNull(it)) null else s.getDouble(it) },
                            completed = s.number("completed") == 1, notes = s.text("notes")
                        )) }
                    }
                    add(WorkoutExerciseItem(workoutExerciseId = entryId, workoutId = id, exerciseId = c.long("exercise_id"),
                        exerciseName = c.text("exercise_name") ?: "", exerciseOrder = c.number("exercise_order"),
                        muscle = c.text("muscle_group") ?: "", durationSeconds = c.number("duration_seconds"), notes = c.text("notes"), sets = sets))
                }
            }
        }
        return workout.copy(items = entries, exerciseNames = entries.map { it.exerciseName }, totalSets = entries.sumOf { it.sets.size })
    }
    fun saveWorkout(draft: WorkoutDraft): Long {
        val date = LocalDate.parse(draft.date)
        require(!date.isAfter(LocalDate.now())) { "Ngày tập không được ở tương lai." }
        require(draft.entries.isNotEmpty()) { "Hãy chọn ít nhất một bài tập." }
        require(draft.entries.map { it.exerciseId }.distinct().size == draft.entries.size) { "Một bài tập chỉ xuất hiện một lần trong phiếu." }
        draft.entries.forEach { require(FitnessRules.entryError(it) == null) { FitnessRules.entryError(it)!! } }
        database.beginTransaction()
        try {
            val workoutId = database.insertOrThrow("workouts", null, ContentValues().apply {
                put("workout_date", draft.date)
                put("duration_minutes", (draft.entries.sumOf { it.durationSeconds } + 59) / 60)
            })
            draft.entries.forEachIndexed { index, entry ->
                check(getExerciseById(entry.exerciseId) != null) { "Bài tập không còn tồn tại." }
                val entryId = database.insertOrThrow("workout_exercises", null, ContentValues().apply {
                    put("workout_id", workoutId); put("exercise_id", entry.exerciseId); put("exercise_order", index + 1)
                    put("exercise_name", entry.name); put("muscle_group", entry.muscle)
                    put("duration_seconds", entry.durationSeconds); put("notes", entry.note.ifBlank { null })
                })
                entry.values.forEachIndexed { setIndex, value ->
                    database.insertOrThrow("workout_sets", null, ContentValues().apply {
                        put("workout_exercise_id", entryId); put("set_number", setIndex + 1)
                        put(if (entry.trackingType == "TIME") "duration_seconds" else "reps", value)
                        put("completed", 1)
                    })
                }
            }
            database.delete("app_state", "state_key = ?", arrayOf("workout_draft"))
            database.setTransactionSuccessful()
            return workoutId
        } finally { database.endTransaction() }
    }
    fun getPrimaryReminder(): Reminder = database.rawQuery("SELECT * FROM reminders ORDER BY reminder_id LIMIT 1", null).use { c ->
        if (c.moveToFirst()) Reminder(c.long("reminder_id"), c.text("title"), c.text("message"), c.text("reminder_time"),
            c.text("repeat_type"), c.number("is_enabled") == 1, c.text("repeat_days"), c.text("scheduled_date")) else Reminder()
    }
    fun saveReminder(r: Reminder) {
        java.time.LocalTime.parse(r.reminderTime)
        require(r.repeatType != "WEEKLY" || !r.repeatDays.isNullOrBlank()) { "Chọn ít nhất một ngày trong tuần." }
        val v = ContentValues().apply {
            put("title", r.title); put("message", r.message); put("reminder_time", r.reminderTime)
            put("repeat_type", r.repeatType); put("repeat_days", if (r.repeatType == "WEEKLY") r.repeatDays else null)
            put("scheduled_date", if (r.repeatType == "ONCE") r.scheduledDate else null); put("is_enabled", if (r.isEnabled) 1 else 0)
        }
        if (r.id == 0L) database.insertOrThrow("reminders", null, v)
        else check(database.update("reminders", v, "reminder_id = ?", arrayOf(r.id.toString())) > 0)
    }
    fun getState(key: String): String? = database.rawQuery("SELECT state_value FROM app_state WHERE state_key = ?", arrayOf(key)).use {
        if (it.moveToFirst()) it.getString(0) else null
    }
    fun setState(key: String, value: String) { database.insertWithOnConflict("app_state", null, ContentValues().apply {
        put("state_key", key); put("state_value", value)
    }, SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) } }
    fun saveDraft(draft: WorkoutDraft) {
        val entries = JSONArray()
        draft.entries.forEach { e -> entries.put(JSONObject().apply {
            put("id", e.exerciseId); put("name", e.name); put("muscle", e.muscle); put("values", JSONArray(e.values))
            put("seconds", e.durationSeconds); put("note", e.note); put("tracking", e.trackingType)
        }) }
        setState("workout_draft", JSONObject().put("date", draft.date).put("entries", entries).toString())
    }
    fun getDraft(): WorkoutDraft {
        val json = getState("workout_draft") ?: return WorkoutDraft()
        val obj = JSONObject(json); val array = obj.getJSONArray("entries")
        return WorkoutDraft(obj.getString("date"), (0 until array.length()).map { index ->
            val e = array.getJSONObject(index); val v = e.getJSONArray("values")
            DraftEntry(e.getLong("id"), e.getString("name"), e.getString("muscle"), (0 until v.length()).map { v.getInt(it) },
                e.getInt("seconds"), e.optString("note"), e.optString("tracking", "REPS"))
        })
    }
}
