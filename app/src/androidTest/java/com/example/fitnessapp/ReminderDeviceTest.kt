package com.example.fitnessapp

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.ReminderScheduler
import com.example.fitnessapp.model.Reminder
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.io.FileInputStream

/** Uses only the QA application's sandbox, never the user's installed database. */
@RunWith(AndroidJUnit4::class)
class ReminderDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var controller: FitnessController
    private lateinit var original: Reminder
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private fun shell(command: String) { instrumentation.uiAutomation.executeShellCommand(command).use { FileInputStream(it.fileDescriptor).readBytes() } }
    @Before fun setup() {
        check(context.packageName.endsWith(".qa"))
        controller = FitnessController(context)
        original = controller.getPrimaryReminder()
        if (InstrumentationRegistry.getArguments().getString("notificationDenied") != "true") {
            shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        }
        if (InstrumentationRegistry.getArguments().getString("exactDenied") != "true") {
            shell("appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow")
        }
        ReminderScheduler.ensureChannel(context)
        manager.cancelAll()
    }
    @After fun restore() {
        synchronized(ReminderScheduler.lock) {
            controller.saveReminder(original)
            ReminderScheduler.reconcile(context, controller, force = true)
        }
        manager.cancelAll()
    }
    private fun configure(seconds: Long = 3600): Reminder {
        controller.saveReminder(Reminder(isEnabled = true, reminderTime = LocalTime.now().plusSeconds(seconds).format(DateTimeFormatter.ofPattern("HH:mm:ss"))))
        ReminderScheduler.reconcile(context, controller)
        return controller.getPrimaryReminder()
    }
    private fun payload(action: String) = JSONObject(checkNotNull(controller.getState("alarm/$action")))
    private fun deliver(action: String, body: JSONObject) = ReminderScheduler.receive(context, controller,
        Intent(context, com.example.fitnessapp.data.ReminderReceiver::class.java).setAction(action).putExtra("payload", body.toString()))
    @Test fun exactAlarmActuallyFiresAndSchedulesNextOccurrence() {
        configure(8)
        val first = payload(ReminderScheduler.REGULAR)
        val deadline = SystemClock.elapsedRealtime() + 30_000
        while (manager.activeNotifications.none { it.id == 41 } && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
        val notification = manager.activeNotifications.single { it.id == 41 }.notification
        assertEquals("Đến giờ tập luyện! 💪", notification.extras.getString("android.title"))
        assertEquals(listOf("Bắt đầu ngay", "Nhắc lại sau 15p"), notification.actions.map { it.title.toString() })
        val next = synchronized(ReminderScheduler.lock) { payload(ReminderScheduler.REGULAR) }
        assertTrue(next.getLong("at") > first.getLong("at"))
        assertTrue(next.getLong("at") > System.currentTimeMillis())
        try {
            shell("cmd statusbar expand-notifications")
            SystemClock.sleep(3000)
            val file = java.io.File(context.getExternalFilesDir(null), "module-qa/notification-preview.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        } finally { shell("cmd statusbar collapse") }
    }
    @Test fun snoozeDoesNotReplaceRegularAndEditingInvalidatesBothAndOldDelivery() {
        configure()
        val due = payload(ReminderScheduler.REGULAR).put("at", System.currentTimeMillis() - 1000)
        controller.setState("alarm/${ReminderScheduler.REGULAR}", due.toString())
        deliver(ReminderScheduler.REGULAR, due)
        val regular = controller.getState("alarm/${ReminderScheduler.REGULAR}")
        val r = controller.getPrimaryReminder()
        val action = JSONObject().put("identity", "${r.id}:${r.revision}").put("token", controller.getState("alarm/notification"))
        deliver(ReminderScheduler.SNOOZE_REQUEST, action)
        val snooze = payload(ReminderScheduler.SNOOZE)
        assertEquals(regular, controller.getState("alarm/${ReminderScheduler.REGULAR}"))
        assertTrue(snooze.getLong("at") in (System.currentTimeMillis() + 890_000)..(System.currentTimeMillis() + 900_000))
        assertTrue(manager.activeNotifications.none { it.id == 41 })
        controller.saveReminder(r.copy(isEnabled = false))
        ReminderScheduler.reconcile(context, controller)
        deliver(ReminderScheduler.SNOOZE, snooze.put("at", 0))
        deliver(ReminderScheduler.REGULAR, due)
        assertEquals("", controller.getState("alarm/${ReminderScheduler.REGULAR}"))
        assertEquals("", controller.getState("alarm/${ReminderScheduler.SNOOZE}"))
        assertTrue(manager.activeNotifications.none { it.id == 41 })
    }
    @Test fun restorationBroadcastsAndUnknownActionsNeverNotify() {
        configure()
        listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED, "unknown").forEach {
            ReminderScheduler.receive(context, controller, Intent(it))
            assertTrue(manager.activeNotifications.isEmpty())
        }
        assertTrue(payload(ReminderScheduler.REGULAR).getLong("at") > System.currentTimeMillis())
    }
    @Test fun missingExactAccessUsesFallback() {
        // Revocation kills the process on Android. Set this app-op before instrumentation starts.
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("exactDenied") == "true")
        configure()
        val result = ReminderScheduler.reconcile(context, controller, force = true)
        assertFalse(result.exact)
        assertTrue(result.notifications)
        assertNotNull(result.nextAt)
        assertNull(result.error)
    }
    @Test fun deniedNotificationPermissionRetainsScheduleAndGrantRestoresAlarm() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("notificationDenied") == "true")
        configure()
        val saved = controller.getPrimaryReminder()
        val blocked = ReminderScheduler.reconcile(context, controller)
        assertFalse(blocked.notifications)
        assertNull(blocked.nextAt)
        assertTrue(saved.isEnabled)
        assertEquals("", controller.getState("alarm/${ReminderScheduler.REGULAR}"))
        shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val granted = ReminderScheduler.reconcile(context, controller)
        assertTrue(granted.notifications)
        assertNotNull(granted.nextAt)
        assertEquals(saved, controller.getPrimaryReminder())
    }
    @Test fun channelBlockedCancelsAlarmAndGrantRestoresConfiguration() {
        configure()
        val r = controller.getPrimaryReminder()
        val channel = manager.getNotificationChannel(ReminderScheduler.CHANNEL)
        instrumentation.uiAutomation.adoptShellPermissionIdentity()
        try {
            val method = NotificationManager::class.java.getMethod("updateNotificationChannel", String::class.java, Int::class.javaPrimitiveType, android.app.NotificationChannel::class.java)
            channel.importance = NotificationManager.IMPORTANCE_NONE
            method.invoke(manager, context.packageName, context.applicationInfo.uid, channel)
            assertFalse(ReminderScheduler.permitted(context))
            val blocked = ReminderScheduler.reconcile(context, controller)
            assertFalse(blocked.notifications)
            assertNull(blocked.nextAt)
            assertEquals(r, controller.getPrimaryReminder())
            channel.importance = NotificationManager.IMPORTANCE_DEFAULT
            method.invoke(manager, context.packageName, context.applicationInfo.uid, channel)
            val allowed = ReminderScheduler.reconcile(context, controller)
            assertTrue(allowed.notifications)
            assertNotNull(allowed.nextAt)
            assertEquals(r, controller.getPrimaryReminder())
        } finally {
            channel.importance = NotificationManager.IMPORTANCE_DEFAULT
            runCatching {
                NotificationManager::class.java.getMethod("updateNotificationChannel", String::class.java, Int::class.javaPrimitiveType, android.app.NotificationChannel::class.java)
                    .invoke(manager, context.packageName, context.applicationInfo.uid, channel)
            }
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }
}
