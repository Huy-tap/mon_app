package com.example.fitnessapp

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteConstraintException
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.FitnessDatabase
import com.example.fitnessapp.model.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class FitnessPersistenceTest {
    private lateinit var file: File
    private lateinit var db: SQLiteDatabase
    private lateinit var controller: FitnessController
    @Before fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File.createTempFile("fitness-test-", ".db", context.cacheDir)
        context.assets.open("fitness_app.db").use { input -> file.outputStream().use { input.copyTo(it) } }
        db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
        db.setForeignKeyConstraintsEnabled(true)
        FitnessDatabase.migrate(db)
        db.execSQL("DELETE FROM workouts"); db.execSQL("DELETE FROM exercises"); db.execSQL("DELETE FROM reminders")
        controller = FitnessController(db)
    }
    @After fun close() { db.close(); file.delete() }
    private fun exercise(name: String = "Bài kiểm thử") = Exercise(name = name, muscleGroup = "Ngực", defaultSets = 3, defaultReps = 12)
    private fun entry(id: Long) = DraftEntry(id, "Bài kiểm thử", "Ngực", listOf(12, 10, 8), 637, "Hiệp cuối hơi mỏi")

    @Test fun workoutRoundTripKeepsDateIdentitySetsSecondsAndNotes() {
        val id = controller.insertExercise(exercise())
        val draft = WorkoutDraft(LocalDate.now().minusDays(2).toString(), listOf(entry(id)))
        controller.saveDraft(draft)
        assertEquals(draft, controller.getDraft())
        val saved = controller.saveWorkout(draft)
        val actual = controller.getWorkoutDetail(saved)!!
        assertEquals(draft.date, actual.workoutDate)
        assertEquals(11, actual.durationMinutes)
        assertEquals(id, actual.items.single().exerciseId)
        assertEquals(listOf(12, 10, 8), actual.items.single().sets.map { it.reps })
        assertEquals(637, actual.items.single().durationSeconds)
        assertEquals("Hiệp cuối hơi mỏi", actual.items.single().notes)
        assertTrue(actual.items.single().sets.all { it.weightKg == null })
        assertTrue(controller.getDraft().entries.isEmpty())
    }
    @Test fun failedChildInsertRollsBackWholeWorkoutAndPreservesDraft() {
        val id = controller.insertExercise(exercise())
        val draft = WorkoutDraft(entries = listOf(entry(id), entry(999999).copy(name = "Không tồn tại")))
        controller.saveDraft(draft)
        assertThrows(IllegalStateException::class.java) { controller.saveWorkout(draft) }
        assertTrue(controller.getAllWorkouts().isEmpty())
        assertEquals(draft, controller.getDraft())
    }
    @Test fun archiveAndRenameDoNotRewriteHistory() {
        val id = controller.insertExercise(exercise())
        val saved = controller.saveWorkout(WorkoutDraft(entries = listOf(entry(id))))
        controller.updateExercise(exercise("Tên đã sửa").copy(id = id))
        assertTrue(controller.deleteExercise(id))
        assertTrue(controller.getAllExercises().isEmpty())
        val historical = controller.getWorkoutDetail(saved)!!.items.single()
        assertEquals("Bài kiểm thử", historical.exerciseName)
        assertEquals(3, historical.sets.size)
    }
    @Test fun emptyAndOtherMonthsNeverUseMockStats() {
        assertEquals(MonthlyStats(), controller.getMonthlyStats())
        val id = controller.insertExercise(exercise())
        controller.saveWorkout(WorkoutDraft(LocalDate.now().withDayOfMonth(1).minusDays(1).toString(), listOf(entry(id))))
        assertEquals(MonthlyStats(), controller.getMonthlyStats(YearMonth.now()))
        assertEquals(1, controller.getMonthlyStats(YearMonth.now().minusMonths(1)).completedExercises)
    }
    @Test fun timeTrackedExercisesKeepSecondsAndNullReps() {
        val id = controller.insertExercise(exercise().copy(trackingType = "TIME", defaultReps = 0, defaultDurationSeconds = 30))
        val saved = controller.saveWorkout(WorkoutDraft(entries = listOf(entry(id).copy(trackingType = "TIME", values = listOf(30, 45)))))
        val sets = controller.getWorkoutDetail(saved)!!.items.single().sets
        assertEquals(listOf(30,45), sets.map { it.durationSeconds }); assertTrue(sets.all { it.reps == null })
    }
    @Test fun reminderCanBeCreatedThenEditedAndSurvivesReopen() {
        controller.saveReminder(Reminder(reminderTime = "06:45:00", repeatType = "WEEKLY", repeatDays = "1,3,5", isEnabled = true))
        val reminder = controller.getPrimaryReminder()
        assertTrue(reminder.id > 0)
        controller.saveReminder(reminder.copy(reminderTime = "20:15:00", isEnabled = false))
        controller.setState("dark_theme", "true")
        db.close(); db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE); controller = FitnessController(db)
        assertEquals("20:15:00", controller.getPrimaryReminder().reminderTime)
        assertFalse(controller.getPrimaryReminder().isEnabled)
        assertEquals("true", controller.getState("dark_theme"))
    }
    @Test fun migrationIsIdempotentAndRetainsRows() {
        controller.insertExercise(exercise())
        FitnessDatabase.migrate(db); FitnessDatabase.migrate(db)
        assertEquals(1, controller.getAllExercises().size)
    }
    @Test fun duplicateNameFailsWithoutReplacingExistingExercise() {
        controller.insertExercise(exercise())
        assertThrows(SQLiteConstraintException::class.java) { controller.insertExercise(exercise()) }
        assertEquals(1, controller.getAllExercises().size)
    }
    @Test fun staleZeroIdUpdatesPrimaryAndInvalidDaysDoNotChangeSavedSchedule() {
        controller.saveReminder(Reminder(isEnabled = true, repeatType = "WEEKLY", repeatDays = "MON,WED,FRI", reminderTime = "07:00"))
        val first = controller.getPrimaryReminder()
        controller.saveReminder(Reminder(isEnabled = true, reminderTime = "08:15:00"))
        val edited = controller.getPrimaryReminder()
        assertEquals(first.id, edited.id)
        assertEquals(first.revision + 1, edited.revision)
        db.rawQuery("SELECT COUNT(*) FROM reminders", null).use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        assertThrows(IllegalArgumentException::class.java) { controller.saveReminder(edited.copy(repeatType = "WEEKLY", repeatDays = "0,8,no")) }
        assertEquals(edited, controller.getPrimaryReminder())
    }
    @Test fun onceAndAdditiveMigrationPreserveConfiguration() {
        controller.saveReminder(Reminder(repeatType = "ONCE", scheduledDate = "2027-01-01", reminderTime = "07:05", isEnabled = true))
        val before = controller.getPrimaryReminder()
        assertEquals("07:05:00", before.reminderTime)
        FitnessDatabase.migrate(db); FitnessDatabase.migrate(db)
        db.close(); db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
        controller = FitnessController(db)
        assertEquals(before, controller.getPrimaryReminder())
    }
    @Test fun eachWorkoutRoundsSecondsBeforeMonthlySumAndSameDayCountsTwice() {
        val id = controller.insertExercise(exercise())
        repeat(2) { controller.saveWorkout(WorkoutDraft(entries = listOf(entry(id).copy(durationSeconds = 61)))) }
        assertEquals(MonthlyStats(2, 4, 2), controller.getMonthlyStats())
    }
}
