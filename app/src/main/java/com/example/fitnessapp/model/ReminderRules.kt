package com.example.fitnessapp.model

import java.time.*

object ReminderRules {
    /** Resolve wall time in the device zone before comparing instants, including DST. */
    fun next(r: Reminder, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (!r.isEnabled || reminderError(r) != null) return null
        val time = LocalTime.parse(r.reminderTime)
        val days = reminderDays(r.repeatDays)
        val dates = if (r.repeatType == "ONCE") listOf(LocalDate.parse(r.scheduledDate))
            else (0L..7L).map { now.toLocalDate().plusDays(it) }
        return dates.asSequence().filter { r.repeatType != "WEEKLY" || it.dayOfWeek.value in days }
            .map { it.atTime(time).atZone(now.zone) }.firstOrNull { it.toInstant().isAfter(now.toInstant()) }
    }
}
