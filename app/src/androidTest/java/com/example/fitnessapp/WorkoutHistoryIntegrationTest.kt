package com.example.fitnessapp

import android.content.res.Configuration
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.FitnessDatabase
import com.example.fitnessapp.data.FitnessRepository
import com.example.fitnessapp.xmlui.RecordWorkoutXmlActivity
import com.example.fitnessapp.xmlui.WorkoutHistoryXmlActivity
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

/** Kiểm tra luồng mới cùng database, điều hướng và theme của huyphan. */
class WorkoutHistoryIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun waitFor(id: Int) {
        val end = System.currentTimeMillis() + 15000
        while (true) {
            try { onView(withId(id)).check(matches(isDisplayed())); return }
            catch (e: AssertionError) { if (System.currentTimeMillis() >= end) throw e; Thread.sleep(100) }
            catch (e: androidx.test.espresso.NoMatchingViewException) { if (System.currentTimeMillis() >= end) throw e; Thread.sleep(100) }
        }
    }
    private fun firstHistoryCard() {
        onView(withId(R.id.history_recycler_view)).perform(object : ViewAction {
            override fun getConstraints() = isDisplayed()
            override fun getDescription() = "Mở buổi tập đầu tiên đang hiển thị"
            override fun perform(ui: androidx.test.espresso.UiController, view: View) {
                assertTrue(view.findViewById<View>(R.id.history_item_card).performClick())
                ui.loopMainThreadUntilIdle()
            }
        })
    }
    private fun assertTheme(id: Int, dark: Boolean) {
        onView(withId(id)).check { view, error ->
            if (error != null) throw error
            assertEquals(if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO,
                view.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
            val color = android.util.TypedValue()
            view.context.theme.resolveAttribute(R.attr.exBackground, color, true)
            assertEquals(android.graphics.Color.parseColor(if (dark) "#1C1B1F" else "#F8FAFC"), color.data)
        }
    }
    @Test fun selectedHistoryMonthAndDetailSurviveRecreation() {
        ActivityScenario.launch(WorkoutHistoryXmlActivity::class.java).use { scenario ->
            waitFor(R.id.history_title)
            onView(withId(R.id.history_month_picker_btn)).perform(click())
            onView(withId(R.id.picker_month_9)).perform(click())
            onView(withId(R.id.history_month_text)).check(matches(withText("Tháng 09/2026")))
            scenario.recreate(); waitFor(R.id.history_title)
            onView(withId(R.id.history_month_text)).check(matches(withText("Tháng 09/2026")))
            firstHistoryCard(); waitFor(R.id.detail_title)
            onView(withId(R.id.detail_workout_date)).check(matches(withText("23/09/2026")))
            pressBack(); waitFor(R.id.history_title)
            onView(withId(R.id.history_month_text)).check(matches(withText("Tháng 09/2026")))
        }
    }
    @Test fun newScreensUsePersistedLightAndDarkTheme() {
        val controller = FitnessController(context)
        val original = controller.getState("dark_theme") == "true"
        try {
            for (dark in listOf(true, false)) {
                controller.setState("dark_theme", dark.toString())
                ActivityScenario.launch(WorkoutHistoryXmlActivity::class.java).use {
                    waitFor(R.id.history_title); assertTheme(R.id.history_root, dark)
                    onView(withId(R.id.history_month_picker_btn)).perform(click())
                    onView(withId(R.id.picker_month_9)).perform(click())
                    firstHistoryCard(); waitFor(R.id.detail_title); assertTheme(R.id.detail_root, dark)
                    pressBack()
                }
                ActivityScenario.launch(RecordWorkoutXmlActivity::class.java).use {
                    waitFor(R.id.record_btn_add_exercise); assertTheme(R.id.record_root, dark)
                }
            }
        } finally { controller.setState("dark_theme", original.toString()) }
    }
    @Test fun savedWorkoutReachesHomeHistoryDetailsAndStatistics() {
        val controller = FitnessController(context)
        val db = FitnessDatabase.open(context)
        val oldIds = controller.getAllWorkouts().map { it.id }.toSet()
        val oldDraft = controller.getState("workout_draft")
        val before = FitnessRepository(db).monthStats(YearMonth.now())
        controller.setState("workout_draft", "")
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                waitFor(R.id.home_record)
                onView(withId(R.id.home_record)).perform(click()); waitFor(R.id.record_btn_add_exercise)
                onView(withId(R.id.record_btn_add_exercise)).perform(scrollTo(), click())
                waitFor(R.id.select_exercise_recycler)
                onView(withText("Bench Press")).perform(click()); waitFor(R.id.result_exercise_name)
                onView(withId(R.id.result_input_minutes)).perform(scrollTo(), replaceText("1"), closeSoftKeyboard())
                onView(withId(R.id.result_input_seconds)).perform(scrollTo(), replaceText("30"), closeSoftKeyboard())
                onView(withId(R.id.result_btn_submit)).perform(scrollTo(), click()); waitFor(R.id.record_btn_save_footer)
                onView(withId(R.id.record_btn_save_footer)).perform(click()); waitFor(R.id.home_record)
                val added = controller.getAllWorkouts().single { it.id !in oldIds }
                assertEquals(90, added.items.single().durationSeconds)
                assertEquals("Bench Press", added.items.single().exerciseName)
                onView(withId(R.id.home_workouts)).check(matches(withText((before.sessions + 1).toString().padStart(2, '0'))))
                onView(withId(R.id.ex_tab_HISTORY)).perform(click()); waitFor(R.id.history_title)
                firstHistoryCard(); waitFor(R.id.detail_title)
                onView(withId(R.id.detail_item_name)).check(matches(withText("Bench Press")))
                pressBack(); waitFor(R.id.history_title)
                onView(withId(R.id.ex_tab_STATS)).perform(click()); waitFor(R.id.sessions)
                val after = FitnessRepository(db).monthStats(YearMonth.now())
                assertEquals(before.sessions + 1, after.sessions)
                assertEquals(before.completedExercises + 1, after.completedExercises)
                onView(withId(R.id.sessions)).check(matches(withText(after.sessions.toString())))
                onView(withId(R.id.minutes)).check(matches(withText(after.minutes.toString())))
            }
        } finally {
            controller.getAllWorkouts().filter { it.id !in oldIds }.forEach {
                db.delete("workouts", "workout_id=?", arrayOf(it.id.toString()))
            }
            if (oldDraft == null) db.delete("app_state", "state_key=?", arrayOf("workout_draft"))
            else controller.setState("workout_draft", oldDraft)
        }
    }
}
