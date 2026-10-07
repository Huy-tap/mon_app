package com.example.fitnessapp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/** Opens the preloaded SQLite database bundled in app/src/main/assets. */
object FitnessDatabase {
    private const val DATABASE_NAME = "fitness_app.db"

    @Volatile
    private var instance: SQLiteDatabase? = null

    fun open(context: Context): SQLiteDatabase {
        instance?.takeIf { it.isOpen }?.let { return it }

        return synchronized(this) {
            instance?.takeIf { it.isOpen } ?: openInternal(context.applicationContext)
        }
    }

    private fun openInternal(context: Context): SQLiteDatabase {
        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        if (!databaseFile.exists() || databaseFile.length() == 0L) {
            copyBundledDatabase(context, databaseFile)
        }

        return SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        ).also { database ->
            database.setForeignKeyConstraintsEnabled(true)
            migrate(database)
            instance = database
        }
    }


    // Additive, idempotent migration: never replaces the user's installed database.
    fun migrate(database: SQLiteDatabase) {
        database.beginTransaction()
        try {
            fun addColumn(table: String, name: String, definition: String) {
                val columns = database.rawQuery("PRAGMA table_info($table)", null).use { c ->
                    buildList { while (c.moveToNext()) add(c.getString(1)) }
                }
                if (name !in columns) database.execSQL("ALTER TABLE $table ADD COLUMN $name $definition")
            }
            addColumn("exercises", "is_archived", "INTEGER NOT NULL DEFAULT 0")
            addColumn("workout_exercises", "duration_seconds", "INTEGER NOT NULL DEFAULT 0")
            addColumn("workout_exercises", "exercise_name", "TEXT")
            addColumn("workout_exercises", "muscle_group", "TEXT")
            database.execSQL("""UPDATE workout_exercises SET
                exercise_name = (SELECT name FROM exercises WHERE exercises.exercise_id = workout_exercises.exercise_id),
                muscle_group = (SELECT muscle_group FROM exercises WHERE exercises.exercise_id = workout_exercises.exercise_id)
                WHERE exercise_name IS NULL""")
            database.execSQL("CREATE TABLE IF NOT EXISTS app_state (state_key TEXT PRIMARY KEY, state_value TEXT NOT NULL)")
            database.setTransactionSuccessful()
        } finally { database.endTransaction() }
    }

    private fun copyBundledDatabase(context: Context, databaseFile: File) {
        databaseFile.parentFile?.mkdirs()
        val temporaryFile = File(databaseFile.parentFile, "$DATABASE_NAME.tmp")
        if (temporaryFile.exists()) temporaryFile.delete()

        context.assets.open(DATABASE_NAME).use { input ->
            temporaryFile.outputStream().use { output -> input.copyTo(output) }
        }

        if (!temporaryFile.renameTo(databaseFile)) {
            temporaryFile.copyTo(databaseFile, overwrite = true)
            temporaryFile.delete()
        }
    }
}
