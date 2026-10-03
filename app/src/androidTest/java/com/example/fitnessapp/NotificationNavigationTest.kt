package com.example.fitnessapp

import android.app.ActivityOptions
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.ReminderScheduler
import com.example.fitnessapp.model.Reminder
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.FileInputStream

@RunWith(AndroidJUnit4::class)
class NotificationNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private lateinit var c: FitnessController
    private lateinit var original: Reminder
    private fun waitText(text: String) = compose.waitUntil(10000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    @Before fun prepare() {
        check(context.packageName.endsWith(".qa"))
        instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS").use { FileInputStream(it.fileDescriptor).readBytes() }
        c = FitnessController(context); original = c.getPrimaryReminder()
    }
    @After fun restore() {
        synchronized(ReminderScheduler.lock) { c.saveReminder(original); ReminderScheduler.reconcile(context, c) }
        manager.cancelAll()
    }
    private fun notification(): android.app.Notification = synchronized(ReminderScheduler.lock) {
        c.saveReminder(Reminder(isEnabled = true, reminderTime = "23:59:00"))
        ReminderScheduler.reconcile(context, c)
        val payload = JSONObject(c.getState("alarm/${ReminderScheduler.REGULAR}")!!).put("at", System.currentTimeMillis() - 1000)
        c.setState("alarm/${ReminderScheduler.REGULAR}", payload.toString())
        ReminderScheduler.receive(context, c, Intent(ReminderScheduler.REGULAR).putExtra("payload", payload.toString()))
        manager.activeNotifications.single { it.id == 41 }.notification
    }
    @Test fun notificationStartOpensRecordOnColdAndExistingActivityAndSnoozeIsReal() {
        val n = notification()
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= 34) options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        n.actions[0].actionIntent.send(context, 0, null, null, null, null, options.toBundle())
        waitText("Ghi nhận buổi tập")
        compose.onNodeWithContentDescription("Quay lại").performClick()
        waitText("Sẵn sàng vận động?")
        // The receiver may have a newer database revision than the running UI snapshot.
        val second = notification()
        second.actions[0].actionIntent.send(context, 0, null, null, null, null, options.toBundle())
        waitText("Ghi nhận buổi tập")
        val third = notification()
        val regular = c.getState("alarm/${ReminderScheduler.REGULAR}")
        third.actions[1].actionIntent.send()
        compose.waitUntil(10000) {
            !c.getState("alarm/${ReminderScheduler.SNOOZE}").isNullOrBlank() && manager.activeNotifications.none { it.id == 41 }
        }
        assertEquals(regular, c.getState("alarm/${ReminderScheduler.REGULAR}"))
        assertTrue(manager.activeNotifications.none { it.id == 41 })
    }
}
