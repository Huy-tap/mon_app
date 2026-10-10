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
            try {
                database.setForeignKeyConstraintsEnabled(true)
                migrate(database)
                BundledExerciseMedia.install(context, database)
                instance = database
            } catch (e: Exception) {
                database.close()
                throw e
            }
        }
    }


    // Upgrades the installed database in place, preserving user data and exercise IDs.
    fun migrate(database: SQLiteDatabase) {
        check(!database.inTransaction()) { "Database migration must start outside a transaction." }
        val foreignKeys = database.rawQuery("PRAGMA foreign_keys", null).use { it.moveToFirst(); it.getInt(0) != 0 }
        // SQLite requires foreign keys to be disabled before rebuilding a referenced table.
        database.setForeignKeyConstraintsEnabled(false)
        try {
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
                migrateExerciseNames(database)
                database.rawQuery("PRAGMA foreign_key_check", null).use {
                    check(!it.moveToFirst()) { "Database migration would break workout history links." }
                }
                database.setTransactionSuccessful()
            } finally { database.endTransaction() }
        } finally { database.setForeignKeyConstraintsEnabled(foreignKeys) }
    }

    private fun migrateExerciseNames(database: SQLiteDatabase) {
        val schema = database.rawQuery("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'exercises'", null).use {
            check(it.moveToFirst()) { "Missing exercises table." }
            it.getString(0)
        }
        val oldUniqueName = Regex(""",\s*CONSTRAINT\s+"?uq_exercise_name"?\s+UNIQUE\s*\(\s*"?name"?\s*\)""", RegexOption.IGNORE_CASE)
        if (oldUniqueName.containsMatchIn(schema)) {
            val sequence = database.rawQuery("SELECT seq FROM sqlite_sequence WHERE name = 'exercises'", null).use {
                if (it.moveToFirst()) it.getLong(0) else 0L
            }
            val dependentSql = database.rawQuery("""SELECT sql FROM sqlite_master
                WHERE tbl_name = 'exercises' AND type IN ('index', 'trigger') AND sql IS NOT NULL""", null).use {
                buildList { while (it.moveToNext()) add(it.getString(0)) }
            }
            val columns = database.rawQuery("PRAGMA table_info(exercises)", null).use {
                buildList { while (it.moveToNext()) add("\"" + it.getString(1).replace("\"", "\"\"") + "\"") }.joinToString(", ")
            }
            // Retain the original defaults, CHECK constraints and any previously added columns.
            val tableName = Regex("""^CREATE\s+TABLE\s+"?exercises"?(?=\s*\()""", RegexOption.IGNORE_CASE)
            check(tableName.containsMatchIn(schema)) { "Unexpected exercises table definition." }
            database.execSQL(schema.replaceFirst(tableName, "CREATE TABLE exercises_name_migration").replace(oldUniqueName, ""))
            database.execSQL("INSERT INTO exercises_name_migration ($columns) SELECT $columns FROM exercises")
            database.execSQL("DROP TABLE exercises")
            database.execSQL("ALTER TABLE exercises_name_migration RENAME TO exercises")
            database.execSQL("UPDATE sqlite_sequence SET seq = MAX(seq, ?) WHERE name = 'exercises'", arrayOf(sequence))
            dependentSql.forEach { database.execSQL(it) }
        }
        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS uq_exercise_active_name
            ON exercises(name) WHERE is_archived = 0""")
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
