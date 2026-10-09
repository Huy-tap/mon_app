package com.example.fitnessapp

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.*
import com.example.fitnessapp.model.ReminderRules
import com.example.fitnessapp.xmlui.SettingsXmlActivity
import org.junit.Assert.*
import org.junit.Test

class UnifiedReminderXmlTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun ready(id: Int) {
        val end = System.currentTimeMillis() + 10000
        while (true) {
            try { onView(withId(id)).check(matches(isDisplayed())).check(matches(isEnabled())); return }
            catch (e: Throwable) { if (System.currentTimeMillis() >= end) throw e; Thread.sleep(100) }
        }
    }
    private fun editor() = ActivityScenario.launch<SettingsXmlActivity>(
        Intent(context, SettingsXmlActivity::class.java).putExtra("reminder", true)
    ).also { ready(R.id.reminder_save) }

    @Test fun allEntryPointsShareEditorAndCancelKeepsSavedData() {
        val before = FitnessController(context).getPrimaryReminder()
        ActivityScenario.launch(MainActivity::class.java).use {
            ready(R.id.home_record)
            onView(withId(R.id.home_reminder_edit)).perform(scrollTo(), click()); ready(R.id.reminder_save)
            onView(withId(R.id.reminder_toggle)).perform(scrollTo(), click())
            onView(withId(R.id.reminder_back)).perform(click()); ready(R.id.home_record)
            assertEquals(before, FitnessController(context).getPrimaryReminder())
            onView(withId(R.id.ex_tab_SETTINGS)).perform(click()); ready(R.id.settings_setup)
            onView(withId(R.id.settings_setup)).perform(click()); ready(R.id.reminder_save)
            onView(withId(R.id.reminder_back)).perform(click()); ready(R.id.settings_setup)
            onView(withId(R.id.ex_tab_STATS)).perform(click()); ready(R.id.month)
            onView(withId(R.id.reminders)).perform(click()); ready(R.id.reminder_save)
            androidx.test.espresso.Espresso.pressBack(); ready(R.id.month)
        }
        assertEquals(before, FitnessController(context).getPrimaryReminder())
    }

    @Test fun cancelTimeInvalidMinuteAndEmptyWeekDoNotSave() {
        val before = FitnessController(context).getPrimaryReminder()
        editor().use { scenario ->
            onView(withId(R.id.reminder_edit)).perform(scrollTo(), click())
            onView(withId(R.id.picker_minute)).perform(replaceText("60"), androidx.test.espresso.action.ViewActions.closeSoftKeyboard())
            onView(withId(R.id.picker_confirm)).perform(click())
            onView(withId(R.id.picker_minute)).check(matches(hasErrorText("Nhập phút từ 00 đến 59")))
            onView(withId(R.id.picker_cancel)).perform(click())
            onView(withId(R.id.reminder_time)).check(matches(withText(before.reminderTime.take(5))))
            onView(withId(R.id.reminder_weekly)).perform(scrollTo(), click())
            val days = listOf(R.id.reminder_day_1, R.id.reminder_day_2, R.id.reminder_day_3,
                R.id.reminder_day_4, R.id.reminder_day_5, R.id.reminder_day_6, R.id.reminder_day_7)
            scenario.onActivity { activity -> days.forEach { id ->
                if (activity.findViewById<android.view.View>(id).isSelected) activity.findViewById<android.view.View>(id).performClick()
            } }
            scenario.recreate(); ready(R.id.reminder_save)
            onView(withId(R.id.reminder_save)).perform(click())
            onView(withId(R.id.reminder_save)).check(matches(isDisplayed()))
            assertEquals(before, FitnessController(context).getPrimaryReminder())
        }
    }

    // Run on the QA emulator with notification permission denied.
    @Test fun deniedNotificationPermissionStillAllowsSavingDisabledSchedule() {
        assertFalse(ReminderScheduler.permitted(context))
        val repository = FitnessRepository(FitnessDatabase.open(context))
        val original = repository.getPrimaryReminder()
        context.getSharedPreferences("permission", 0).edit().putBoolean("asked", true).commit()
        try {
            repository.saveReminder(original.copy(isEnabled = true))
            editor().use {
                onView(withId(R.id.reminder_permission)).check(matches(isDisplayed()))
                onView(withId(R.id.reminder_status)).check(matches(withText("Chưa hoạt động")))
                onView(withId(R.id.reminder_toggle)).perform(scrollTo(), click())
                onView(withId(R.id.reminder_save)).perform(click())
                val end = System.currentTimeMillis() + 10000
                while (repository.getPrimaryReminder().isEnabled && System.currentTimeMillis() < end) Thread.sleep(50)
                assertFalse(repository.getPrimaryReminder().isEnabled)
            }
        } finally { repository.saveReminder(original); ReminderScheduler.restore(context) }
    }

    // Run separately with notification permission granted.
    @Test fun enabledScheduleSetsAlarmAndDisablingCancelsIt() {
        assertTrue(ReminderScheduler.permitted(context))
        val repository = FitnessRepository(FitnessDatabase.open(context))
        val original = repository.getPrimaryReminder()
        context.getSharedPreferences("permission", 0).edit().putBoolean("asked", true).commit()
        try {
            repository.saveReminder(original.copy(isEnabled = false))
            editor().use {
                onView(withId(R.id.reminder_daily)).perform(scrollTo(), click())
                onView(withId(R.id.reminder_toggle)).perform(scrollTo(), click())
                val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                if (screenshot != null) java.io.File(context.getExternalFilesDir(null), "unified-reminder-light.png").outputStream().use {
                    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                onView(withId(R.id.reminder_save)).perform(click())
                val end = System.currentTimeMillis() + 10000
                val delivery = context.applicationContext.getSharedPreferences("alarm_delivery", 0)
                while (delivery.getLong("due", 0) == 0L && System.currentTimeMillis() < end) Thread.sleep(50)
                val saved = repository.getPrimaryReminder()
                assertTrue(saved.isEnabled); assertEquals("DAILY", saved.repeatType)
                assertTrue(delivery.getLong("due", 0) > System.currentTimeMillis())
                assertEquals(ReminderRules.signature(saved), delivery.getString("signature", null))
                // Exercise delivery through the actual receiver without waiting until tomorrow.
                val due = System.currentTimeMillis() - 100
                val notifications = context.getSystemService(android.app.NotificationManager::class.java)
                notifications.cancel(41)
                delivery.edit().putLong("due", due).commit()
                context.sendBroadcast(Intent(context, ReminderReceiver::class.java).setAction(ReminderScheduler.FIRE)
                    .putExtra("due", due).putExtra("signature", ReminderRules.signature(saved)))
                val notificationEnd = System.currentTimeMillis() + 10000
                while (notifications.activeNotifications.none { it.id == 41 } && System.currentTimeMillis() < notificationEnd) Thread.sleep(50)
                assertTrue("Receiver did not display the workout notification", notifications.activeNotifications.any { it.id == 41 })
                notifications.cancel(41)
            }
            editor().use {
                onView(withId(R.id.reminder_toggle)).perform(scrollTo(), click())
                onView(withId(R.id.reminder_save)).perform(click())
                val end = System.currentTimeMillis() + 10000
                val delivery = context.applicationContext.getSharedPreferences("alarm_delivery", 0)
                while (delivery.contains("due") && System.currentTimeMillis() < end) Thread.sleep(50)
                assertFalse(repository.getPrimaryReminder().isEnabled)
                assertFalse(delivery.contains("due"))
            }
        } finally { repository.saveReminder(original); ReminderScheduler.restore(context) }
    }
}
