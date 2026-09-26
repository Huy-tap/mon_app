package com.example.fitnessapp
import com.example.fitnessapp.model.*
import com.example.fitnessapp.data.ReminderScheduler
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime
import java.time.YearMonth

class FitnessRulesTest {
    @Test fun validationRejectsBoundsAndZeroDuration() {
        assertNotNull(FitnessRules.exerciseError(Exercise(name = "", muscleGroup = "Ngực")))
        assertNotNull(FitnessRules.exerciseError(Exercise(name = "Push", muscleGroup = "Ngực", defaultSets = 101)))
        assertNotNull(FitnessRules.entryError(DraftEntry(1, "Push", "Ngực", listOf(0), 60)))
        assertNotNull(FitnessRules.entryError(DraftEntry(1, "Push", "Ngực", listOf(10), 0)))
        assertNull(FitnessRules.entryError(DraftEntry(1, "Push", "Ngực", listOf(10), 1)))
    }
    @Test fun weekdaysFromBundledDatabaseRemainValid() {
        assertEquals(listOf(1, 3, 5), reminderDays("MON,WED,FRI"))
        assertEquals(listOf(1, 3, 5), reminderDays("1,3,5"))
    }
    @Test fun weeklyReminderFindsNextMatchingDay() {
        val now = LocalDateTime.of(2026, 9, 26, 20, 0)
        val r = Reminder(isEnabled = true, reminderTime = "18:00:00", repeatType = "WEEKLY", repeatDays = "MON,WED,FRI")
        assertEquals(LocalDateTime.of(2026, 9, 28, 18, 0), ReminderScheduler.next(r, now))
        assertNull(ReminderScheduler.next(r.copy(isEnabled = false), now))
    }
    @Test fun chartCountsLastPartialWeekWithoutDroppingDays() {
        val list = listOf(Workout(workoutDate = "2026-09-30"), Workout(workoutDate = "2026-08-30"))
        assertEquals(listOf(0,0,0,1), FitnessRules.weeklyCounts(list, YearMonth.of(2026,9)))
    }
}
