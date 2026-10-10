package com.example.fitnessapp

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.FitnessDatabase
import com.example.fitnessapp.model.Exercise
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExerciseNameMigrationTest {
    private lateinit var file: File
    private lateinit var db: SQLiteDatabase

    @Before fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File.createTempFile("exercise-migration-", ".db", context.cacheDir)
        context.assets.open("fitness_app.db").use { input -> file.outputStream().use { input.copyTo(it) } }
        db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
    }
    @After fun close() { db.close(); file.delete() }

    private fun rows(sql: String): List<List<String?>> = db.rawQuery(sql, null).use { c ->
        buildList { while (c.moveToNext()) add((0 until c.columnCount).map { if (c.isNull(it)) null else c.getString(it) }) }
    }
    private fun flag() = rows("PRAGMA foreign_keys").single().single()
    private fun snapshot() = listOf("exercises", "workouts", "workout_exercises", "workout_sets", "reminders", "app_state")
        .associateWith { rows("SELECT * FROM $it ORDER BY 1") }

    @Test fun upgradePreservesAllRowsColumnsIndexesTriggersAndNextId() {
        // Reproduce the installed schema before this change, including archived rows and history snapshots.
        db.execSQL("ALTER TABLE exercises ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE exercises ADD COLUMN extra_note TEXT")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN duration_seconds INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN exercise_name TEXT")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN muscle_group TEXT")
        db.execSQL("""UPDATE workout_exercises SET exercise_name =
            (SELECT name FROM exercises WHERE exercises.exercise_id = workout_exercises.exercise_id),
            muscle_group = (SELECT muscle_group FROM exercises WHERE exercises.exercise_id = workout_exercises.exercise_id),
            duration_seconds = 75""")
        db.execSQL("CREATE TABLE app_state (state_key TEXT PRIMARY KEY, state_value TEXT NOT NULL)")
        db.execSQL("INSERT INTO app_state VALUES ('dark_theme', 'true')")
        db.execSQL("UPDATE exercises SET is_archived = 1, instruction_image = '/saved/pushup.jpg', instruction_video = '/saved/pushup.mp4', extra_note = 'kept' WHERE exercise_id = 1")
        db.execSQL("UPDATE sqlite_sequence SET seq = 1000 WHERE name = 'exercises'")
        db.execSQL("CREATE INDEX migration_extra_index ON exercises(description)")
        db.execSQL("""CREATE TRIGGER migration_extra_trigger AFTER UPDATE OF description ON exercises
            BEGIN INSERT OR REPLACE INTO app_state VALUES ('trigger_ran', 'true'); END""")
        db.setForeignKeyConstraintsEnabled(true)
        val before = snapshot()
        val columns = rows("PRAGMA table_info(exercises)")
        FitnessDatabase.migrate(db)
        FitnessDatabase.migrate(db)
        assertEquals(before, snapshot())
        assertEquals(columns, rows("PRAGMA table_info(exercises)"))
        assertEquals("1", flag())
        assertTrue(rows("PRAGMA foreign_key_check").isEmpty())
        assertEquals(listOf(listOf("ok")), rows("PRAGMA integrity_check"))
        assertEquals(3, rows("SELECT name FROM sqlite_master WHERE name IN ('idx_exercise_muscle', 'migration_extra_index', 'migration_extra_trigger')").size)
        val controller = FitnessController(db)
        val newId = controller.insertExercise(Exercise(name = "Push Up", muscleGroup = "Ngực"))
        assertEquals(1001L, newId)
        assertEquals("/saved/pushup.jpg", controller.getExerciseById(1)!!.instructionImage)
        assertTrue(rows("SELECT * FROM workout_exercises WHERE exercise_id = 1").isNotEmpty())
        assertThrows(SQLiteConstraintException::class.java) { db.execSQL("UPDATE exercises SET default_sets = 0 WHERE exercise_id = ?", arrayOf(newId)) }
        assertThrows(SQLiteConstraintException::class.java) { db.execSQL("UPDATE exercises SET default_reps = NULL WHERE exercise_id = ?", arrayOf(newId)) }
        assertThrows(SQLiteConstraintException::class.java) { db.execSQL("DELETE FROM exercises WHERE exercise_id = 1") }
        db.execSQL("UPDATE exercises SET description = 'updated' WHERE exercise_id = ?", arrayOf(newId))
        assertEquals(listOf(listOf("true")), rows("SELECT state_value FROM app_state WHERE state_key = 'trigger_ran'"))
    }

    @Test fun invalidHistoryRollsBackSchemaAndRowsAndRestoresForeignKeys() {
        db.execSQL("""INSERT INTO workout_exercises(workout_id, exercise_id, exercise_order)
            VALUES (1, 999999, 999)""")
        db.setForeignKeyConstraintsEnabled(true)
        val schema = rows("SELECT type, name, sql FROM sqlite_master ORDER BY type, name")
        val tables = listOf("exercises", "workouts", "workout_exercises", "workout_sets", "reminders", "sqlite_sequence")
        val before = tables.associateWith { rows("SELECT * FROM $it ORDER BY 1") }
        assertThrows(IllegalStateException::class.java) { FitnessDatabase.migrate(db) }
        assertEquals(schema, rows("SELECT type, name, sql FROM sqlite_master ORDER BY type, name"))
        assertEquals(before, tables.associateWith { rows("SELECT * FROM $it ORDER BY 1") })
        assertEquals("1", flag())
    }
}
