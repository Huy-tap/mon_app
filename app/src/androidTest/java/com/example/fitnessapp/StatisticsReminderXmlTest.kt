package com.example.fitnessapp

import com.example.fitnessapp.xmlui.StatisticsReminderXmlActivity

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
class StatisticsReminderXmlTest {
    private fun ready(s: ActivityScenario<StatisticsReminderXmlActivity>,id: Int,predicate: (String)->Boolean) {
        val deadline=System.currentTimeMillis()+10000
        while(System.currentTimeMillis()<deadline) {
            var good=false
            s.onActivity { good=it.findViewById<TextView>(id)?.text?.toString()?.let(predicate)==true }
            if(good) return
            Thread.sleep(50)
        }
        fail("UI did not become ready: $id")
    }
    private fun sheetReady(s: ActivityScenario<StatisticsReminderXmlActivity>, tag: String) {
        val deadline=System.currentTimeMillis()+10000
        while(System.currentTimeMillis()<deadline) {
            var good=false
            s.onActivity { activity ->
                val parent=activity.supportFragmentManager.findFragmentById(R.id.container)
                val sheet=parent?.childFragmentManager?.findFragmentByTag(tag) as? androidx.fragment.app.DialogFragment
                good=sheet?.dialog?.window?.decorView?.hasWindowFocus()==true && sheet.view?.isLaidOut==true
            }
            if(good) {
                androidx.test.espresso.Espresso.closeSoftKeyboard()
                onView(isRoot()).perform(object : androidx.test.espresso.ViewAction {
                    override fun getConstraints() = isRoot()
                    override fun getDescription() = "Đợi bảng chọn khôi phục hoàn tất animation và vị trí cửa sổ"
                    override fun perform(ui: androidx.test.espresso.UiController, view: android.view.View) {
                        ui.loopMainThreadForAtLeast(500)
                    }
                })
                return
            }
            Thread.sleep(50)
        }
        fail("Restored sheet did not receive focus: $tag")
    }
    private fun sheetClosed(s: ActivityScenario<StatisticsReminderXmlActivity>, tag: String) {
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
        captureStatistics("failed-sheet-$tag.png")
        fail("Sheet did not close: $tag")
    }
    @Test fun monthCancelConfirmRotateAndBack() {
        ActivityScenario.launch(StatisticsReminderXmlActivity::class.java).use { s ->
            ready(s,R.id.sessions) { it!="—" }
            var old="";s.onActivity { old=it.findViewById<TextView>(R.id.month).text.toString() }
            onView(withId(R.id.month)).perform(click())
            sheetReady(s,"month")
            onView(withId(R.id.month9)).perform(click())
            onView(withId(R.id.cancel)).perform(click())
            onView(withId(R.id.month)).check(matches(withText(old)))
            onView(withId(R.id.month)).perform(click())
            sheetReady(s,"month")
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
            waitForReminder()
            androidx.test.espresso.Espresso.pressBack()
            onView(withId(R.id.month)).check(matches(withText(selected)))
        }
    }
    @Test fun statisticsChartAndEmptyState() {
        ActivityScenario.launch(StatisticsReminderXmlActivity::class.java).use { scenario ->
            ready(scenario,R.id.sessions) { it!="—" }
            onView(withId(R.id.month)).perform(click())
            sheetReady(scenario,"month")
            onView(withId(R.id.month9)).perform(click())
            onView(withId(R.id.confirm)).perform(click())
            sheetClosed(scenario,"month")
            ready(scenario,R.id.sessions) { it == "11" }
            onView(withId(R.id.chartCard)).check(matches(isDisplayed()))
            onView(withId(R.id.stateCard)).check(matches(org.hamcrest.Matchers.not(isDisplayed())))
            captureStatistics("statistics-september-updated.png")
            // Chọn tháng trống trong dữ liệu mẫu; test CRUD có thể tạo buổi trong tháng hiện tại.
            onView(withId(R.id.month)).perform(click())
            sheetReady(scenario,"month")
            onView(withId(R.id.previousYear)).perform(click())
            onView(withId(R.id.month10)).perform(click())
            onView(withId(R.id.confirm)).perform(click())
            sheetClosed(scenario,"month")
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
    @Test fun statisticsErrorDoesNotBecomeZeroAndRetryRecovers() {
        val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario.launch(StatisticsReminderXmlActivity::class.java).use { scenario ->
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
    private fun waitForReminder() {
        val deadline = System.currentTimeMillis() + 10000
        while (true) {
            try { onView(withId(R.id.reminder_save)).check(matches(isDisplayed())).check(matches(isEnabled())); return }
            catch (e: Throwable) { if (System.currentTimeMillis() >= deadline) throw e; Thread.sleep(100) }
        }
    }
}
