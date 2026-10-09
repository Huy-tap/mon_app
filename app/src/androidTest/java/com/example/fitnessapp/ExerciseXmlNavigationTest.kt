package com.example.fitnessapp

import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.model.FitnessRules
import com.example.fitnessapp.xmlui.HomeXmlModel
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

/** Điều hướng XML, chặn module chưa phát triển và bảo toàn dữ liệu/trạng thái Trang chủ. */
class ExerciseXmlNavigationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun waitFor(id: Int) {
        val end = System.currentTimeMillis() + 10000
        while (true) {
            try { onView(withId(id)).check(matches(isDisplayed())); return }
            catch (e: AssertionError) { if (System.currentTimeMillis() >= end) throw e; Thread.sleep(100) }
            catch (e: androidx.test.espresso.NoMatchingViewException) { if (System.currentTimeMillis() >= end) throw e; Thread.sleep(100) }
        }
    }
    private fun settleScroll() {
        onView(isRoot()).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints() = isRoot()
            override fun getDescription() = "Đợi ScrollView kết thúc cuộn trước khi bấm/đo vị trí"
            override fun perform(ui: androidx.test.espresso.UiController, view: android.view.View) { ui.loopMainThreadForAtLeast(400) }
        })
    }
    private fun blocked(id: Int, scroll: Boolean = false) {
        if (scroll) { onView(withId(id)).perform(scrollTo()); settleScroll() }
        onView(withId(id)).perform(click())
        onView(withText("Trang này chưa phát triển")).check(matches(isDisplayed()))
        onView(withText("Đóng")).perform(click())
    }
    @Test fun homeAndExerciseTabCanBeOpenedRepeatedly() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor(R.id.home_record)
            onView(withId(R.id.ex_tab_EXERCISES)).perform(click()); waitFor(R.id.ex_add)
            onView(withId(R.id.ex_tab_HOME)).perform(click()); waitFor(R.id.home_record)
            onView(withId(R.id.ex_tab_EXERCISES)).perform(click()); waitFor(R.id.ex_add)
            androidx.test.espresso.Espresso.pressBack(); waitFor(R.id.home_record)
        }
    }
    @Test fun everyUnfinishedEntryIsBlockedWithoutChangingWorkoutsOrDraft() {
        val controller = FitnessController(context)
        val beforeWorkouts = controller.getAllWorkouts()
        val beforeDraft = controller.getDraft()
        val beforeReminder = controller.getPrimaryReminder()
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor(R.id.home_record)
            listOf(R.id.home_record, R.id.home_history, R.id.home_recent).forEach { blocked(it, true) }
            listOf(R.id.ex_tab_HISTORY).forEach { blocked(it) }
            onView(withId(R.id.ex_tab_EXERCISES)).perform(click()); waitFor(R.id.ex_add)
            listOf(R.id.ex_tab_HISTORY).forEach { blocked(it) }
            onView(withId(R.id.ex_add)).check(matches(isDisplayed()))
        }
        assertEquals(beforeWorkouts, controller.getAllWorkouts())
        assertEquals(beforeDraft, controller.getDraft())
        assertEquals(beforeReminder, controller.getPrimaryReminder())
    }
    @Test fun homeShowsDatabaseTotalsAndKeepsScrollAfterRecreation() {
        val expected = FitnessRules.monthStats(FitnessController(context).getAllWorkouts(), YearMonth.now())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor(R.id.home_record)
            onView(withId(R.id.home_workouts)).check(matches(withText(expected.totalWorkouts.toString().padStart(2, '0'))))
            onView(withId(R.id.home_minutes)).check(matches(withText(expected.totalMinutes.toString().padStart(2, '0'))))
            onView(withId(R.id.home_completed)).check(matches(withText(expected.completedSets.toString().padStart(2, '0'))))
            onView(withId(R.id.home_reminder_edit)).perform(scrollTo()); settleScroll()
            var position = 0
            scenario.onActivity { position = it.findViewById<ScrollView>(R.id.home_scroll).scrollY }
            scenario.recreate()
            waitFor(R.id.home_scroll)
            scenario.onActivity {
                assertEquals(position, it.findViewById<ScrollView>(R.id.home_scroll).scrollY)
                assertEquals(expected.totalWorkouts.toString().padStart(2, '0'), it.findViewById<TextView>(R.id.home_workouts).text.toString())
                assertNotNull(ViewModelProvider(it)[HomeXmlModel::class.java].state.value.data)
            }
            onView(withId(R.id.ex_tab_EXERCISES)).perform(click()); waitFor(R.id.ex_add)
            androidx.test.espresso.Espresso.pressBack(); waitFor(R.id.home_scroll)
            scenario.onActivity { assertEquals(position, it.findViewById<ScrollView>(R.id.home_scroll).scrollY) }
        }
    }

    @Test fun statisticsTabConnectsHomeAndExercisesAndKeepsUnfinishedTabsLocked() {
        val controller = FitnessController(context)
        val before = controller.getAllWorkouts()
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor(R.id.home_record)
            onView(withId(R.id.ex_tab_STATS)).perform(click()); waitFor(R.id.month)
            blocked(R.id.ex_tab_HISTORY)
            onView(withId(R.id.ex_tab_EXERCISES)).perform(click()); waitFor(R.id.ex_add)
            onView(withId(R.id.ex_tab_STATS)).perform(click()); waitFor(R.id.month)
            onView(withId(R.id.ex_tab_HOME)).perform(click()); waitFor(R.id.home_record)
        }
        assertEquals(before, controller.getAllWorkouts())
    }

    @Test fun reminderOpensFromHomeAndNotificationIntentAndReturnsToHome() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor(R.id.home_record)
            onView(withId(R.id.home_reminder_edit)).perform(scrollTo()); settleScroll()
            onView(withId(R.id.home_reminder_edit)).perform(click()); waitFor(R.id.back)
            onView(withId(R.id.back)).perform(click()); waitFor(R.id.home_scroll)
        }
        val intent = android.content.Intent(context, MainActivity::class.java)
            .putExtra("reminder", true).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            waitFor(R.id.back)
            androidx.test.espresso.Espresso.pressBack(); waitFor(R.id.home_record)
        }
    }
}
