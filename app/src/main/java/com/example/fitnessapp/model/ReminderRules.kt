package com.example.fitnessapp.model

import java.time.*

object ReminderRules {
    fun next(r: Reminder, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (!r.isEnabled) return null
        val time=runCatching { LocalTime.parse(r.reminderTime) }.getOrNull() ?: return null
        if(r.repeatType=="ONCE") return runCatching { LocalDate.parse(r.scheduledDate).atTime(time).atZone(now.zone) }.getOrNull()?.takeIf { it.isAfter(now) }
        if(r.repeatType !in listOf("DAILY","WEEKLY")) return null
        val days=reminderDays(r.repeatDays)
        return (0L..7L).asSequence().map { now.toLocalDate().plusDays(it).atTime(time).atZone(now.zone) }
            .firstOrNull { it.isAfter(now) && (r.repeatType=="DAILY" || it.dayOfWeek.value in days) }
    }
    fun signature(r: Reminder): String = listOf(r.id,r.reminderTime,r.repeatType,r.repeatDays,r.scheduledDate,r.isEnabled).joinToString("|")
}
