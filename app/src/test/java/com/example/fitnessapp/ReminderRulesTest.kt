package com.example.fitnessapp

import com.example.fitnessapp.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReminderRulesTest {
    private val zone=ZoneId.of("Asia/Ho_Chi_Minh")
    private fun at(s: String)=LocalDateTime.parse(s).atZone(zone)
    private fun r(type: String="DAILY",days: String?=null)=Reminder(reminderTime="07:00:00",repeatType=type,repeatDays=days,isEnabled=true)
    @Test fun dailyBeforeAfterAndYearBoundary() {
        assertEquals(at("2026-12-31T07:00"),ReminderRules.next(r(),at("2026-12-31T06:59")))
        assertEquals(at("2027-01-01T07:00"),ReminderRules.next(r(),at("2026-12-31T07:00")))
        assertEquals(at("2026-10-01T07:00"),ReminderRules.next(r(),at("2026-09-30T23:59")))
    }
    @Test fun weeklySundayMultipleDaysAndWeekRollover() {
        assertEquals(at("2026-10-11T07:00"),ReminderRules.next(r("WEEKLY","7"),at("2026-10-05T08:00")))
        assertEquals(at("2026-10-12T07:00"),ReminderRules.next(r("WEEKLY","1,3,7"),at("2026-10-11T08:00")))
        assertEquals(at("2026-10-07T07:00"),ReminderRules.next(r("WEEKLY","MON,WED,FRI"),at("2026-10-05T08:00")))
    }
    @Test fun disabledInvalidDaysAndOnce() {
        assertNull(ReminderRules.next(r().copy(isEnabled=false)))
        assertNull(ReminderRules.next(r("WEEKLY","0,8,NOPE")))
        assertNull(ReminderRules.next(r().copy(reminderTime="25:90")))
        assertEquals(listOf(1,3,7),reminderDays("MON,3,SUN,1,9,garbage"))
        val once=r("ONCE").copy(scheduledDate="2026-12-31")
        assertEquals(at("2026-12-31T07:00"),ReminderRules.next(once,at("2026-12-30T08:00")))
        assertNull(ReminderRules.next(once,at("2027-01-01T08:00")))
    }
    @Test fun daylightSavingGapAndOverlapStillFuture() {
        val z=ZoneId.of("Europe/Berlin")
        val now=ZonedDateTime.of(2026,3,29,1,59,0,0,z)
        val next=ReminderRules.next(r().copy(reminderTime="02:30:00"),now)!!
        assertTrue(next.isAfter(now));assertEquals(3,next.hour)
        val autumn=ZonedDateTime.of(2026,10,25,2,45,0,0,z).withEarlierOffsetAtOverlap()
        assertTrue(ReminderRules.next(r().copy(reminderTime="02:30:00"),autumn)!!.isAfter(autumn))
    }
}
