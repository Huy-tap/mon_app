package com.example.fitnessapp.data

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.fitnessapp.MainActivity
import com.example.fitnessapp.R
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.model.Reminder
import com.example.fitnessapp.model.reminderDays
import java.time.*

object ReminderScheduler {
    private const val CHANNEL = "workout_reminders"
    fun permitted(context: Context): Boolean = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun next(r: Reminder, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!r.isEnabled) return null
        val time = runCatching { LocalTime.parse(r.reminderTime) }.getOrNull() ?: return null
        if (r.repeatType == "ONCE") return runCatching { LocalDate.parse(r.scheduledDate).atTime(time) }.getOrNull()?.takeIf { it.isAfter(now) }
        val days = reminderDays(r.repeatDays)
        return (0L..7L).map { now.toLocalDate().plusDays(it).atTime(time) }.firstOrNull {
            it.isAfter(now) && (r.repeatType != "WEEKLY" || it.dayOfWeek.value in days)
        }
    }
    fun schedule(context: Context, r: Reminder) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(context, 41, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.cancel(pending)
        val next = next(r) ?: return
        if (!permitted(context)) return
        // Inexact alarm does not require special exact-alarm access.
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pending)
    }
    fun notify(context: Context, r: Reminder) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Nhắc nhở tập luyện", NotificationManager.IMPORTANCE_DEFAULT))
        if (!permitted(context)) return
        val pending = PendingIntent.getActivity(context, 42, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(41, NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(r.title).setContentText(r.message ?: "Đã đến giờ vận động. Ghi lại buổi tập hôm nay nhé!")
            .setContentIntent(pending).setAutoCancel(true).build())
    }
}
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                val reminder = FitnessController(context).getPrimaryReminder()
                if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_TIME_CHANGED && intent.action != Intent.ACTION_TIMEZONE_CHANGED && reminder.isEnabled) ReminderScheduler.notify(context, reminder)
                ReminderScheduler.schedule(context, reminder)
            } finally { pending.finish() }
        }.start()
    }
}
