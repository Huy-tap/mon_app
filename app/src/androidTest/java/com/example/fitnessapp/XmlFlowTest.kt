package com.example.fitnessapp

import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class XmlFlowTest {
    private fun ready(s: ActivityScenario<MainActivity>,id: Int,predicate: (String)->Boolean) {
        val deadline=System.currentTimeMillis()+10000
        while(System.currentTimeMillis()<deadline) {
            var good=false
            s.onActivity { good=it.findViewById<TextView>(id)?.text?.toString()?.let(predicate)==true }
            if(good) return
            Thread.sleep(50)
        }
        fail("UI did not become ready: $id")
    }
    private fun sheetReady(s: ActivityScenario<MainActivity>, tag: String) {
        val deadline=System.currentTimeMillis()+10000
        while(System.currentTimeMillis()<deadline) {
            var good=false
            s.onActivity { activity ->
                val parent=activity.supportFragmentManager.findFragmentById(R.id.container)
                val sheet=parent?.childFragmentManager?.findFragmentByTag(tag) as? androidx.fragment.app.DialogFragment
                good=sheet?.dialog?.window?.decorView?.hasWindowFocus()==true && sheet.view?.isLaidOut==true
            }
            if(good) return
            Thread.sleep(50)
        }
        fail("Restored sheet did not receive focus: $tag")
    }
    private fun sheetClosed(s: ActivityScenario<MainActivity>, tag: String) {
        val deadline=System.currentTimeMillis()+10000
        while(System.currentTimeMillis()<deadline) {
            var good=false
            s.onActivity { activity ->
                val parent=activity.supportFragmentManager.findFragmentById(R.id.container)
                good=parent?.childFragmentManager?.findFragmentByTag(tag)==null && activity.window.decorView.hasWindowFocus()
            }
            if(good) return
            Thread.sleep(50)
        }
        fail("Sheet did not close: $tag")
    }
    @Test fun monthCancelConfirmRotateAndBack() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            ready(s,R.id.sessions) { it!="—" }
            var old="";s.onActivity { old=it.findViewById<TextView>(R.id.month).text.toString() }
            onView(withId(R.id.month)).perform(click())
            onView(withId(R.id.month9)).perform(click())
            onView(withId(R.id.cancel)).perform(click())
            onView(withId(R.id.month)).check(matches(withText(old)))
            onView(withId(R.id.month)).perform(click())
            onView(withId(R.id.month9)).perform(click())
            s.recreate()
            sheetReady(s,"month")
            onView(withId(R.id.confirm)).perform(click())
            sheetClosed(s,"month")
            ready(s,R.id.sessions) { it!="—" }
            var selected="";s.onActivity { selected=it.findViewById<TextView>(R.id.month).text.toString() }
            assertTrue(selected.contains("09/"))
            s.recreate()
            onView(withId(R.id.month)).check(matches(withText(selected)))
            onView(withId(R.id.reminders)).perform(click())
            ready(s,R.id.save) { it=="Lưu cài đặt" }
            androidx.test.espresso.Espresso.pressBack()
            onView(withId(R.id.month)).check(matches(withText(selected)))
        }
    }
    @Test fun statisticsChartAndEmptyState() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.month)).perform(click())
            onView(withId(R.id.month9)).perform(click())
            onView(withId(R.id.confirm)).perform(click())
            sheetClosed(scenario,"month")
            ready(scenario,R.id.sessions) { it == "11" }
            onView(withId(R.id.chartCard)).check(matches(isDisplayed()))
            onView(withId(R.id.stateCard)).check(matches(org.hamcrest.Matchers.not(isDisplayed())))
            captureStatistics("statistics-september-updated.png")
            onView(withId(R.id.next)).perform(click())
            ready(scenario,R.id.sessions) { it == "0" }
            onView(withId(R.id.chartCard)).check(matches(org.hamcrest.Matchers.not(isDisplayed())))
            onView(withId(R.id.startWorkout)).check(matches(isDisplayed()))
            captureStatistics("statistics-empty-updated.png")
        }
    }
    private fun captureStatistics(name: String) {
        val instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(300) // Let the rendered frame settle before visual QA.
        val bitmap=instrumentation.uiAutomation.takeScreenshot()
        java.io.File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
        bitmap.recycle()
    }
    @Test fun timeCancelValidationWeekDaysSaveAndReopen() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            onView(withId(R.id.reminders)).perform(click());ready(s,R.id.save) { it=="Lưu cài đặt" }
            var old="";s.onActivity { old=it.findViewById<TextView>(R.id.time).text.toString() }
            onView(withId(R.id.changeTime)).perform(scrollTo(),click())
            onView(withId(R.id.hour)).perform(replaceText("23"));androidx.test.espresso.Espresso.closeSoftKeyboard()
            onView(withId(R.id.cancel)).perform(click())
            onView(withId(R.id.time)).check(matches(withText(old)))
            onView(withId(R.id.changeTime)).perform(scrollTo(),click())
            onView(withId(R.id.hour)).perform(replaceText("25"));androidx.test.espresso.Espresso.closeSoftKeyboard()
            onView(withId(R.id.confirm)).perform(click())
            onView(withId(R.id.hour)).check(matches(hasErrorText("Giờ từ 00 đến 23")))
            onView(withId(R.id.hour)).perform(replaceText("23"))
            onView(withId(R.id.minute)).perform(replaceText("59"));androidx.test.espresso.Espresso.closeSoftKeyboard()
            s.recreate();sheetReady(s,"time")
            onView(withId(R.id.hour)).check(matches(withText("23")))
            onView(withId(R.id.minute)).check(matches(withText("59")))
            onView(withId(R.id.confirm)).perform(click());sheetClosed(s,"time")
            onView(withId(R.id.weekly)).perform(scrollTo(),click())
            onView(withId(R.id.day7)).perform(scrollTo(),click())
            s.recreate()
            onView(withId(R.id.time)).check(matches(withText("23:59")))
            onView(withId(R.id.day7)).check(matches(isChecked()))
            // Keep disabled so tests never create alarms or notification permission dialogs.
            s.onActivity { it.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.enabled).isChecked=false }
            onView(withId(R.id.save)).perform(scrollTo(),click())
            ready(s,R.id.feedback) { it.startsWith("Đã lưu") }
            androidx.test.espresso.Espresso.pressBack();onView(withId(R.id.reminders)).perform(click());ready(s,R.id.save) { it=="Lưu cài đặt" }
            onView(withId(R.id.time)).check(matches(withText("23:59")))
            onView(withId(R.id.day7)).check(matches(isChecked()))
        }
    }
    @Test fun statisticsErrorDoesNotBecomeZeroAndRetryRecovers() {
        val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ready(scenario,R.id.sessions) { it!="—" }
            val db=com.example.fitnessapp.data.FitnessDatabase.open(context)
            db.execSQL("ALTER TABLE workouts RENAME TO temporarily_unavailable_workouts")
            try {
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
                ready(scenario,R.id.stateTitle) { it=="Lỗi tải dữ liệu" }
                onView(withId(R.id.cards)).check(matches(org.hamcrest.Matchers.not(isDisplayed())))
                onView(withId(R.id.retry)).check(matches(isDisplayed()))
                captureStatistics("statistics-error.png")
            } finally { db.execSQL("ALTER TABLE temporarily_unavailable_workouts RENAME TO workouts") }
            onView(withId(R.id.retry)).perform(click())
            ready(scenario,R.id.stateTitle) { it.isEmpty() }
            onView(withId(R.id.cards)).check(matches(isDisplayed()))
        }
    }
    @Test fun blockedPermissionStateKeepsFormUsable() {
        val instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        assertFalse("Run this suite with POST_NOTIFICATIONS denied on the QA emulator",com.example.fitnessapp.data.ReminderScheduler.permitted(instrumentation.targetContext))
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            onView(withId(R.id.reminders)).perform(click());ready(s,R.id.save) { it=="Lưu cài đặt" }
            onView(withId(R.id.warning)).check(matches(isDisplayed()))
            onView(withId(R.id.save)).check(matches(isEnabled()))
        }
    }
}
