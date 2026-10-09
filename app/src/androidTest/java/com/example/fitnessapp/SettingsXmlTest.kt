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
    @Test fun previewRetainsDraftAndDoesNotWriteDatabase() {
        val controller = FitnessController(context)
        val before = controller.getPrimaryReminder()
        val theme = controller.getState("dark_theme")
        ActivityScenario.launch(SettingsXmlActivity::class.java).use { scenario ->
            waitFor(R.id.settings_setup); capture("settings-light")
            onView(withId(R.id.settings_dark)).perform(click())
            onView(withId(R.id.settings_dark)).check(matches(isSelected())); capture("settings-dark")
            scenario.recreate(); waitFor(R.id.settings_setup)
            onView(withId(R.id.settings_dark)).check(matches(isSelected()))
            onView(withId(R.id.settings_light)).perform(click())
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
            onView(withId(R.id.picker_confirm)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click())
            onView(withId(R.id.reminder_time)).check(matches(withText("07:35")))
            onView(withId(R.id.reminder_day_1)).perform(scrollTo(), click())
            capture("settings-weekly")
            scenario.recreate()
            onView(withId(R.id.reminder_weekly)).check(matches(isSelected()))
            onView(withId(R.id.reminder_time)).check(matches(withText("07:35")))
            onView(withId(R.id.reminder_save)).perform(click())
            waitFor(R.id.settings_setup)
            capture("settings-preview-saved")
        }
        assertEquals(before, controller.getPrimaryReminder())
        assertEquals(theme, controller.getState("dark_theme"))
    }
}
