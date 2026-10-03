package com.example.fitnessapp.data

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.fitnessapp.MainActivity
import com.example.fitnessapp.R
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.model.*
import org.json.JSONObject
import java.time.*
import java.util.UUID
import java.util.concurrent.Executors

data class ReminderAvailability(val notifications: Boolean = true, val exact: Boolean = true,
    val nextAt: ZonedDateTime? = null, val error: String? = null)

object ReminderScheduler {
    const val CHANNEL = "workout_reminders"
    const val RECORD = "com.example.fitnessapp.RECORD_WORKOUT"
    internal const val REGULAR = "com.example.fitnessapp.REMINDER"
    internal const val SNOOZE = "com.example.fitnessapp.SNOOZE_ALARM"
    internal const val SNOOZE_REQUEST = "com.example.fitnessapp.SNOOZE_REQUEST"
    // UI saves and receiver work share this lock; SQLite is the persistent source of truth.
    val lock = Any()
    private const val NOTIFICATION_ID = 41
    private val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun ensureChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Nhắc nhở tập luyện", NotificationManager.IMPORTANCE_DEFAULT))
    }
    fun permitted(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE &&
            (Build.VERSION.SDK_INT < 28 || manager.getNotificationChannel(CHANNEL)?.group?.let { manager.getNotificationChannelGroup(it)?.isBlocked } != true)
    }
    fun exactAllowed(context: Context): Boolean = Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun next(r: Reminder, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? = ReminderRules.next(r, now.atZone(ZoneId.systemDefault()))?.toLocalDateTime()
    private fun key(action: String) = "alarm/$action"
    private fun identity(r: Reminder) = "${r.id}:${r.revision}"
    private fun token(c: FitnessController, action: String): JSONObject? = c.getState(key(action))?.takeIf { it.isNotBlank() }?.let { runCatching { JSONObject(it) }.getOrNull() }
    private fun pending(context: Context, action: String, payload: JSONObject? = null): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        payload?.let { intent.putExtra("payload", it.toString()) }
        return PendingIntent.getBroadcast(context, if (action == REGULAR) 101 else if (action == SNOOZE) 102 else 103, intent, flags)
    }
    private fun cancel(context: Context, c: FitnessController, action: String) {
        val p = pending(context, action)
        context.getSystemService(AlarmManager::class.java).cancel(p)
        p.cancel()
        c.setState(key(action), "")
    }
    private fun putAlarm(context: Context, c: FitnessController, r: Reminder, action: String, at: Long) {
        val payload = JSONObject().put("identity", identity(r)).put("token", UUID.randomUUID().toString()).put("at", at)
        val p = pending(context, action, payload)
        val manager = context.getSystemService(AlarmManager::class.java)
        c.setState(key(action), payload.toString())
        try {
            if (exactAllowed(context)) {
                try {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
                    c.setState("alarm/inexact", "false")
                    return
                } catch (_: SecurityException) { /* Access changed between checking and scheduling. */ }
            }
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
            c.setState("alarm/inexact", "true")
        } catch (e: Exception) { cancel(context, c, action); throw e }
    }
    /** On IO. force is for reboot/clock/access changes, not ordinary recomposition. */
    fun reconcile(context: Context, c: FitnessController, force: Boolean = false): ReminderAvailability = synchronized(lock) {
        ensureChannel(context)
        val r = c.getPrimaryReminder()
        val allowed = permitted(context)
        var problem: String? = null
        try {
            val manager = context.getSystemService(AlarmManager::class.java)
            PendingIntent.getBroadcast(context, 41, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { manager.cancel(it); it.cancel() }
            if (c.getState("alarm/identity") != identity(r)) {
                cancel(context, c, REGULAR); cancel(context, c, SNOOZE)
                c.setState("alarm/notification", "")
                context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
                c.setState("alarm/identity", identity(r))
            }
            if (!r.isEnabled || !allowed || reminderError(r) != null) {
                cancel(context, c, REGULAR); cancel(context, c, SNOOZE)
                if (r.isEnabled) problem = reminderError(r)
            } else {
                val access = "${exactAllowed(context)}:${ZoneId.systemDefault()}"
                val refresh = force || c.getState("alarm/access") != access
                val existing = token(c, REGULAR)
                if (refresh || existing == null) {
                    cancel(context, c, REGULAR)
                    if (r.repeatType != "ONCE" || c.getState("alarm/onceConsumed") != identity(r)) {
                        ReminderRules.next(r)?.let { putAlarm(context, c, r, REGULAR, it.toInstant().toEpochMilli()) }
                    }
                }
                if (refresh) token(c, SNOOZE)?.let {
                    val at = it.getLong("at")
                    cancel(context, c, SNOOZE)
                    if (at > System.currentTimeMillis()) putAlarm(context, c, r, SNOOZE, at)
                }
                c.setState("alarm/access", access)
            }
        } catch (e: Exception) {
            Log.e("WorkoutReminder", "Cannot schedule reminder", e)
            problem = "Đã lưu, chưa đặt được lời nhắc. Hãy thử lại."
        }
        val at = token(c, REGULAR)?.optLong("at")?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
        ReminderAvailability(allowed, exactAllowed(context) && c.getState("alarm/inexact") != "true", at, problem)
    }
    internal fun receive(context: Context, c: FitnessController, intent: Intent) = synchronized(lock) {
        val action = intent.action
        if (action in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {
            reconcile(context, c, force = true)
            return@synchronized
        }
        if (action !in listOf(REGULAR, SNOOZE, SNOOZE_REQUEST)) return@synchronized
        val r = c.getPrimaryReminder()
        val payload = runCatching { JSONObject(intent.getStringExtra("payload") ?: "") }.getOrNull() ?: return@synchronized
        if (!r.isEnabled || reminderError(r) != null || payload.optString("identity") != identity(r)) return@synchronized
        if (!permitted(context)) { reconcile(context, c); return@synchronized }
        if (action == SNOOZE_REQUEST) {
            if (c.getState("alarm/notification") != payload.optString("token")) return@synchronized
            c.setState("alarm/notification", "")
            cancel(context, c, SNOOZE)
            putAlarm(context, c, r, SNOOZE, System.currentTimeMillis() + 15 * 60_000L)
            context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
            return@synchronized
        }
        if (token(c, action!!)?.optString("token") != payload.optString("token") || payload.optLong("at") > System.currentTimeMillis()) return@synchronized
        cancel(context, c, action)
        if (action == REGULAR && r.repeatType == "ONCE") c.setState("alarm/onceConsumed", identity(r))
        try { notify(context, c, r) } finally { reconcile(context, c) }
    }
    private fun notify(context: Context, c: FitnessController, r: Reminder) {
        ensureChannel(context)
        if (!permitted(context)) return
        val notificationToken = UUID.randomUUID().toString()
        c.setState("alarm/notification", notificationToken)
        val payload = JSONObject().put("identity", identity(r)).put("token", notificationToken)
        val open = PendingIntent.getActivity(context, 42, Intent(context, MainActivity::class.java), flags)
        val record = PendingIntent.getActivity(context, 43, Intent(context, MainActivity::class.java).setAction(RECORD)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("reminderIdentity", identity(r)), flags)
        val message = r.message?.takeIf { it.isNotBlank() } ?: "Hãy dành thời gian cho buổi tập hôm nay. Chỉ cần 15 phút để duy trì phong độ!"
        try {
            context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID,
                NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_workout_notification)
                    .setContentTitle("Đến giờ tập luyện! 💪").setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message)).setContentIntent(open).setAutoCancel(true)
                    .addAction(0, "Bắt đầu ngay", record)
                    .addAction(0, "Nhắc lại sau 15p", pending(context, SNOOZE_REQUEST, payload)).build())
        } catch (e: SecurityException) { Log.w("WorkoutReminder", "Notification permission changed", e) }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(ReminderScheduler.REGULAR, ReminderScheduler.SNOOZE, ReminderScheduler.SNOOZE_REQUEST,
                Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) return
        val pending = goAsync()
        try {
            executor.execute {
                try { ReminderScheduler.receive(context, FitnessController(context), intent) }
                catch (e: Exception) { Log.e("WorkoutReminder", "Receiver failed", e) }
                finally { pending.finish() }
            }
        } catch (e: Exception) { pending.finish(); Log.e("WorkoutReminder", "Cannot start receiver", e) }
    }
    companion object { private val executor = Executors.newSingleThreadExecutor() }
}
