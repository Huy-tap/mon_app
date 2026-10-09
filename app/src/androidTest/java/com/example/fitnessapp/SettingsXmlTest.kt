package com.example.fitnessapp

import android.graphics.Bitmap
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.xmlui.SettingsXmlActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SettingsXmlTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun waitFor(id: Int) {
        val end = System.currentTimeMillis() + 10000
        while (true) {
            try { onView(withId(id)).check(matches(isDisplayed())).check(matches(isEnabled())); return }
            catch (e: Throwable) { if (System.currentTimeMillis() >= end) throw e; Thread.sleep(100) }
        }
    }
    private fun waitSelected(id: Int) {
        val end = System.currentTimeMillis() + 10000
        while (true) {
            try { onView(withId(id)).check(matches(isSelected())).check(matches(isEnabled())); return }
            catch (e: Throwable) { if (System.currentTimeMillis() >= end) throw e; Thread.sleep(100) }
        }
    }
    private fun assertTheme(id: Int, dark: Boolean) {
        onView(withId(id)).check { view, error ->
            if (error != null) throw error
            val night = view.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            assertEquals(if (dark) android.content.res.Configuration.UI_MODE_NIGHT_YES else android.content.res.Configuration.UI_MODE_NIGHT_NO, night)
            val expected = android.graphics.Color.parseColor(if (dark) "#1C1B1F" else "#F8FAFC")
            assertEquals(expected, view.context.getColor(R.color.surface))
            val value = android.util.TypedValue()
            view.context.theme.resolveAttribute(R.attr.exBackground, value, true)
            assertEquals(expected, value.data)
        }
    }
    private fun capture(name: String) {
        onView(isRoot()).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints() = isRoot()
            override fun getDescription() = "Wait for layout before screenshot"
            override fun perform(ui: androidx.test.espresso.UiController, view: View) { ui.loopMainThreadForAtLeast(350) }
        })
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun settingsNavigationFromBothTabs() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor(R.id.home_record)
            onView(withId(R.id.ex_tab_SETTINGS)).perform(click()); waitFor(R.id.settings_setup)
            onView(withId(R.id.ex_tab_EXERCISES)).perform(click()); waitFor(R.id.ex_add)
            onView(withId(R.id.ex_tab_SETTINGS)).perform(click()); waitFor(R.id.settings_setup)
            onView(withId(R.id.ex_tab_STATS)).perform(click()); waitFor(R.id.month)
            onView(withId(R.id.ex_tab_SETTINGS)).perform(click()); waitFor(R.id.settings_setup)
            onView(withId(R.id.ex_tab_HOME)).perform(click()); waitFor(R.id.home_record)
        }
    }
    @Test fun reminderRetainsDraftAndSavesDatabase() {
        val controller = FitnessController(context)
        val before = controller.getPrimaryReminder()

        ActivityScenario.launch(SettingsXmlActivity::class.java).use { scenario ->
            waitFor(R.id.settings_setup); capture("settings-light")
            onView(withId(R.id.settings_dark)).perform(click()); waitSelected(R.id.settings_dark)
            onView(withId(R.id.settings_dark)).check(matches(isSelected())); capture("settings-dark")
            scenario.recreate(); waitFor(R.id.settings_setup)
            onView(withId(R.id.settings_dark)).check(matches(isSelected()))
            onView(withId(R.id.settings_light)).perform(click()); waitSelected(R.id.settings_light)
            onView(withId(R.id.settings_setup)).perform(click())
            onView(withId(R.id.reminder_daily)).perform(scrollTo(), click()); capture("settings-daily")
            onView(withId(R.id.reminder_weekly)).perform(scrollTo(), click())
            onView(withId(R.id.reminder_edit)).perform(scrollTo(), click())
            onView(withId(R.id.picker_hour)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("25"))
            onView(withId(R.id.picker_confirm)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click())
            onView(withId(R.id.picker_hour)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).check(matches(hasErrorText("Nhập giờ từ 00 đến 23")))
            onView(withId(R.id.picker_hour)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("07"))
            onView(withId(R.id.picker_minute)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("35"), androidx.test.espresso.action.ViewActions.closeSoftKeyboard())
            capture("settings-time-picker")
            scenario.recreate()
            onView(withId(R.id.picker_minute)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).check(matches(withText("35")))
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            onView(isRoot()).perform(object : androidx.test.espresso.ViewAction {
                override fun getConstraints() = isRoot()
                override fun getDescription() = "Đợi bảng chọn giờ ổn định sau khi khôi phục"
                override fun perform(ui: androidx.test.espresso.UiController, view: android.view.View) { ui.loopMainThreadForAtLeast(500) }
            })
            onView(withId(R.id.picker_confirm)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click())
            onView(withId(R.id.reminder_time)).check(matches(withText("07:35")))
            onView(withId(R.id.reminder_day_1)).perform(scrollTo(), click())
            capture("settings-weekly")
            scenario.recreate()
            onView(withId(R.id.reminder_weekly)).check(matches(isSelected()))
            onView(withId(R.id.reminder_time)).check(matches(withText("07:35")))
            assertEquals(before, controller.getPrimaryReminder())
            // Save disabled to avoid opening notification permission dialogs during this form test.
            if (controller.getPrimaryReminder().isEnabled) onView(withId(R.id.reminder_toggle)).perform(scrollTo(), click())
            onView(withId(R.id.reminder_save)).perform(click())
            waitFor(R.id.settings_setup)
            capture("settings-preview-saved")
        }
        val saved = controller.getPrimaryReminder()
        assertEquals("07:35:00", saved.reminderTime)
        assertEquals("WEEKLY", saved.repeatType)
        assertEquals(false, saved.isEnabled)
        com.example.fitnessapp.data.FitnessRepository(com.example.fitnessapp.data.FitnessDatabase.open(context)).saveReminder(before)
        com.example.fitnessapp.data.ReminderScheduler.restore(context)
        assertEquals("false", controller.getState("dark_theme"))
    }
    @Test fun themeAppliesAcrossAppAndAfterRelaunch() {
        val controller = FitnessController(context)
        val original = controller.getState("dark_theme") == "true"
        try {
            for (dark in listOf(true, false)) {
                ActivityScenario.launch(MainActivity::class.java).use {
                    waitFor(R.id.home_record)
                    onView(withId(R.id.ex_tab_SETTINGS)).perform(click()); waitFor(R.id.settings_setup)
                    val choice = if (dark) R.id.settings_dark else R.id.settings_light
                    onView(withId(choice)).perform(click()); waitSelected(choice)
                    assertEquals(dark.toString(), controller.getState("dark_theme"))
                    assertTheme(R.id.settings_root, dark)
                    onView(withId(R.id.ex_tab_EXERCISES)).perform(click()); waitFor(R.id.ex_add)
                    assertTheme(R.id.ex_root, dark); capture("global-exercises-$dark")
                    onView(withId(R.id.ex_tab_STATS)).perform(click()); waitFor(R.id.month)
                    assertTheme(R.id.module_root, dark); capture("global-statistics-$dark")
                    onView(withId(R.id.month)).perform(click())
                    onView(withId(R.id.year)).check(matches(isDisplayed())); capture("global-month-$dark")
                    onView(withId(R.id.cancel)).perform(click())
                    onView(withId(R.id.reminders)).perform(click()); waitFor(R.id.reminder_back)
                    assertTheme(R.id.settings_root, dark); capture("global-reminder-$dark")
                    onView(withId(R.id.reminder_back)).perform(click()); waitFor(R.id.month)
                    onView(withId(R.id.ex_tab_HOME)).perform(click()); waitFor(R.id.home_record)
                    assertTheme(R.id.home_root, dark); capture("global-home-$dark")
                }
                // A fresh activity reads the persisted choice rather than a theme Intent extra.
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    waitFor(R.id.home_record); assertTheme(R.id.home_root, dark)
                    scenario.recreate(); waitFor(R.id.home_record); assertTheme(R.id.home_root, dark)
                }
            }
        } finally { controller.setState("dark_theme", original.toString()) }
    }

}
