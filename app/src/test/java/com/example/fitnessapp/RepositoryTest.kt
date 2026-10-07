package com.example.fitnessapp

import android.database.sqlite.SQLiteDatabase
import com.example.fitnessapp.data.*
import com.example.fitnessapp.model.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class RepositoryTest {
    private lateinit var file: File
    private lateinit var db: SQLiteDatabase
    private lateinit var repo: FitnessRepository
    @Before fun setup() {
        file=File.createTempFile("fitness-test", ".db")
        File("src/main/assets/fitness_app.db").copyTo(file,overwrite=true)
        db=SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READWRITE)
        db.setForeignKeyConstraintsEnabled(true);FitnessDatabase.migrate(db);repo=FitnessRepository(db)
    }
    @After fun teardown() { db.close();file.delete() }
    @Test fun sourceStatsAndEmptyMonth() {
        assertEquals(MonthlyStats(11,450,30),repo.monthStats(YearMonth.of(2026,9)))
        assertEquals(MonthlyStats(0,0,0),repo.monthStats(YearMonth.of(2026,10)))
        assertEquals(MonthlyStats(0,0,0),repo.monthStats(YearMonth.of(2025,9)))
    }
    @Test fun sameDayAndRepeatedExerciseCountEntriesNotSets() {
        db.execSQL("INSERT INTO workouts(workout_id,workout_date,duration_minutes) VALUES(1001,'2028-01-01',20),(1002,'2028-01-01',30),(1003,'2028-02-01',99)")
        val ex=db.rawQuery("SELECT exercise_id FROM exercises LIMIT 2",null).use { it.moveToFirst();val a=it.getLong(0);it.moveToNext();a to it.getLong(0) }
        db.execSQL("INSERT INTO workout_exercises(workout_exercise_id,workout_id,exercise_id,exercise_order) VALUES(1001,1001,${ex.first},1),(1002,1002,${ex.first},1),(1003,1001,${ex.second},2),(1004,1002,${ex.second},2)")
        db.execSQL("INSERT INTO workout_sets(workout_exercise_id,set_number,reps,completed) VALUES(1001,1,10,1),(1001,2,10,1),(1002,1,10,1),(1004,1,10,1),(1004,2,10,0)")
        assertEquals(MonthlyStats(2,50,2),repo.monthStats(YearMonth.of(2028,1)))
    }
    @Test fun savePrimaryWithoutDuplicatesAndReopen() {
        val original=repo.getPrimaryReminder()
        val others=db.rawQuery("SELECT * FROM reminders WHERE reminder_id != ?",arrayOf(original.id.toString())).use { buildList { while(it.moveToNext()) add((0 until it.columnCount).map { index -> it.getString(index) }) } }
        repeat(3) { repo.saveReminder(original.copy(repeatType="WEEKLY",repeatDays="MON,3,SUN",reminderTime="06:05",isEnabled=true)) }
        assertEquals(3L,android.database.DatabaseUtils.longForQuery(db,"SELECT COUNT(*) FROM reminders",null))
        assertEquals(others,db.rawQuery("SELECT * FROM reminders WHERE reminder_id != ?",arrayOf(original.id.toString())).use { buildList { while(it.moveToNext()) add((0 until it.columnCount).map { index -> it.getString(index) }) } })
        db.close();db=SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READWRITE);repo=FitnessRepository(db)
        assertEquals("06:05:00",repo.getPrimaryReminder().reminderTime)
        assertEquals("1,3,7",repo.getPrimaryReminder().repeatDays)
        assertThrows(IllegalArgumentException::class.java) { repo.saveReminder(original.copy(repeatType="WEEKLY",repeatDays="0,8,BAD")) }
    }
    @Test fun oncePreservedAndMigrationIdempotent() {
        val before=repo.monthStats(YearMonth.of(2026,9))
        FitnessDatabase.migrate(db);FitnessDatabase.migrate(db)
        assertEquals(before,repo.monthStats(YearMonth.of(2026,9)))
        repo.saveReminder(repo.getPrimaryReminder().copy(repeatType="ONCE",scheduledDate="2027-01-02",repeatDays=null))
        val r=repo.getPrimaryReminder();repo.saveReminder(r.copy(isEnabled=false))
        assertEquals("ONCE",repo.getPrimaryReminder().repeatType)
        assertEquals("2027-01-02",repo.getPrimaryReminder().scheduledDate)
    }
}
