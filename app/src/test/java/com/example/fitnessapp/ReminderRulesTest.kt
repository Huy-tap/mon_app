package com.example.fitnessapp

import com.example.fitnessapp.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReminderRulesTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val daily = Reminder(isEnabled = true, reminderTime = "18:30:00")
    private fun at(s: String) = LocalDateTime.parse(s).atZone(zone)
    @Test fun dailyBeforeAtAndAfterTimeAreStrictlyFuture() {
        assertEquals(at("2026-10-02T18:30"), ReminderRules.next(daily, at("2026-10-02T18:29")))
        assertEquals(at("2026-10-03T18:30"), ReminderRules.next(daily, at("2026-10-02T18:30")))
        assertEquals(at("2026-10-03T18:30"), ReminderRules.next(daily, at("2026-10-02T19:00")))
    }
    @Test fun weeklyCrossesSundayMonthAndYear() {
        val monday = daily.copy(repeatType = "WEEKLY", repeatDays = "1")
        assertEquals(at("2026-11-02T18:30"), ReminderRules.next(monday, at("2026-11-01T20:00")))
        assertEquals(at("2027-01-04T18:30"), ReminderRules.next(monday, at("2026-12-31T23:59")))
        assertEquals(at("2026-10-12T18:30"), ReminderRules.next(monday, at("2026-10-05T18:30")))
    }
    @Test fun multipleDaysAndLegacyDaysHaveSameMeaning() {
        val numeric = daily.copy(repeatType = "WEEKLY", repeatDays = "1,3,5")
        val legacy = numeric.copy(repeatDays = "MON,WED,FRI")
        assertEquals(at("2026-10-07T18:30"), ReminderRules.next(numeric, at("2026-10-05T19:00")))
        assertEquals(ReminderRules.next(numeric, at("2026-10-05T19:00")), ReminderRules.next(legacy, at("2026-10-05T19:00")))
        assertEquals(listOf(1, 7), reminderDays(" MON,1, 7,SUN,0,8,garbage"))
    }
    @Test fun invalidOrDisabledSchedulesNeverRun() {
        listOf(daily.copy(isEnabled = false), daily.copy(repeatType = "UNKNOWN"), daily.copy(reminderTime = "25:00:00"),
            daily.copy(repeatType = "WEEKLY", repeatDays = "0,8,garbage"), daily.copy(repeatType = "WEEKLY", repeatDays = ""),
            daily.copy(repeatType = "ONCE", scheduledDate = null)).forEach { assertNull(ReminderRules.next(it, at("2026-10-02T00:00"))) }
    }
    @Test fun onceIsNeverConvertedToDaily() {
        val once = daily.copy(repeatType = "ONCE", scheduledDate = "2026-10-02")
        assertEquals(at("2026-10-02T18:30"), ReminderRules.next(once, at("2026-10-02T18:00")))
        assertNull(ReminderRules.next(once, at("2026-10-02T19:00")))
    }
    @Test fun dstUsesLocalWallClockInsteadOf24Hours() {
        val ny = ZoneId.of("America/New_York")
        val before = ZonedDateTime.of(2026, 3, 7, 8, 0, 0, 0, ny)
        val next = ReminderRules.next(daily.copy(reminderTime = "08:00:00"), before)!!
        assertEquals(8, next.hour)
        assertEquals(23, Duration.between(before, next).toHours())
        val gap = ReminderRules.next(daily.copy(reminderTime = "02:30:00"), ZonedDateTime.of(2026, 3, 8, 1, 0, 0, 0, ny))!!
        assertEquals(3, gap.hour)
        assertEquals(30, gap.minute)
    }
}
