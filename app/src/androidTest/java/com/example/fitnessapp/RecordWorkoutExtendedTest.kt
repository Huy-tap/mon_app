package com.example.fitnessapp

import android.widget.EditText
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.FitnessDatabase
import com.example.fitnessapp.xmlui.RecordWorkoutXmlActivity
import org.junit.*

class RecordWorkoutExtendedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var controller: FitnessController
    private var oldDraft: String? = null
    private var oldIds = emptySet<Long>()
    @Before fun setup() {
        controller = FitnessController(context)
        oldDraft = controller.getState("workout_draft")
        oldIds = controller.getAllWorkouts().map { it.id }.toSet()
        controller.setState("workout_draft", "")
    }
    @After fun restore() {
        val db = FitnessDatabase.open(context)
        controller.getAllWorkouts().filter { it.id !in oldIds }.forEach {
            db.delete("workouts", "workout_id=?", arrayOf(it.id.toString()))
        }
        oldDraft?.let { controller.setState("workout_draft", it) }
            ?: db.delete("app_state", "state_key=?", arrayOf("workout_draft"))
    }
    private fun ready(id: Int) {
        val end = System.currentTimeMillis() + 10000
        while (true) {
            try { onView(withId(id)).check(matches(isDisplayed())); return }
            catch (e: Throwable) { if (System.currentTimeMillis() >= end) throw e; Thread.sleep(100) }
        }
    }
    private fun select(name: String) {
        ready(R.id.record_btn_add_exercise)
        onView(withId(R.id.record_btn_add_exercise)).perform(click())
        ready(R.id.select_exercise_recycler)
        val position = controller.getAllExercises().indexOfFirst { it.name == name }
        onView(withId(R.id.select_exercise_recycler)).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints() = isDisplayed()
            override fun getDescription() = "Cuộn đến bài cần kiểm thử"
            override fun perform(ui: androidx.test.espresso.UiController, view: android.view.View) {
                (view as androidx.recyclerview.widget.RecyclerView).scrollToPosition(position)
                ui.loopMainThreadForAtLeast(400)
            }
        })
        onView(withText(name)).perform(click())
        ready(R.id.result_exercise_name)
    }
    @Test fun individualSetValuesAndNoteSurviveRecreation() {
        ActivityScenario.launch(RecordWorkoutXmlActivity::class.java).use { scenario ->
            select("Bench Press")
            onView(withId(R.id.result_sets_count)).perform(replaceText("2"), closeSoftKeyboard())
            scenario.onActivity {
                val rows = it.findViewById<LinearLayout>(R.id.result_sets_container)
                rows.getChildAt(0).findViewById<EditText>(R.id.set_input_value).setText("8")
                rows.getChildAt(1).findViewById<EditText>(R.id.set_input_value).setText("6")
            }
            onView(withId(R.id.result_input_minutes)).perform(scrollTo(), replaceText("3"), closeSoftKeyboard())
            onView(withId(R.id.result_input_seconds)).perform(replaceText("15"), closeSoftKeyboard())
            onView(withId(R.id.result_input_note)).perform(scrollTo(), replaceText("Giữ kết quả từng hiệp"), closeSoftKeyboard())
            scenario.recreate(); ready(R.id.result_exercise_name)
            onView(withId(R.id.result_input_minutes)).check(matches(withText("3")))
            onView(withId(R.id.result_input_seconds)).check(matches(withText("15")))
            onView(withId(R.id.result_input_note)).check(matches(withText("Giữ kết quả từng hiệp")))
            scenario.onActivity {
                val rows = it.findViewById<LinearLayout>(R.id.result_sets_container)
                Assert.assertEquals("8", rows.getChildAt(0).findViewById<EditText>(R.id.set_input_value).text.toString())
                Assert.assertEquals("6", rows.getChildAt(1).findViewById<EditText>(R.id.set_input_value).text.toString())
            }
        }
    }
    @Test fun timeExerciseAndInvalidDurationAreHandled() {
        ActivityScenario.launch(RecordWorkoutXmlActivity::class.java).use { scenario ->
            select("Plank")
            onView(withId(R.id.result_label_reps_title)).check(matches(withText("Số giây từng hiệp")))
            onView(withId(R.id.result_input_seconds)).perform(scrollTo(), replaceText("60"), closeSoftKeyboard())
            onView(withId(R.id.result_btn_submit)).perform(scrollTo(), click())
            onView(withId(R.id.result_error_text)).check(matches(withText("Số giây phải từ 0 đến 59.")))
            onView(withId(R.id.result_sets_count)).perform(scrollTo(), replaceText("2"), closeSoftKeyboard())
            scenario.onActivity {
                val rows = it.findViewById<LinearLayout>(R.id.result_sets_container)
                rows.getChildAt(0).findViewById<EditText>(R.id.set_input_value).setText("45")
                rows.getChildAt(1).findViewById<EditText>(R.id.set_input_value).setText("60")
            }
            onView(withId(R.id.result_input_minutes)).perform(scrollTo(), replaceText("2"), closeSoftKeyboard())
            onView(withId(R.id.result_input_seconds)).perform(replaceText("00"), closeSoftKeyboard())
            onView(withId(R.id.result_btn_submit)).perform(scrollTo(), click()); ready(R.id.entry_name)
            onView(withId(R.id.record_btn_save_footer)).perform(click())
            val end = System.currentTimeMillis() + 10000
            while (controller.getAllWorkouts().none { it.id !in oldIds } && System.currentTimeMillis() < end) Thread.sleep(100)
            val item = controller.getAllWorkouts().first { it.id !in oldIds }.items.single()
            Assert.assertEquals("Plank", item.exerciseName)
            Assert.assertEquals(120, item.durationSeconds)
            Assert.assertEquals(listOf(45, 60), item.sets.map { it.durationSeconds })
        }
    }
    @Test fun removingEntryRequiresConfirmationAndDoesNotCreateWorkout() {
        ActivityScenario.launch(RecordWorkoutXmlActivity::class.java).use {
            select("Bench Press")
            onView(withId(R.id.result_input_minutes)).perform(scrollTo(), replaceText("1"), closeSoftKeyboard())
            onView(withId(R.id.result_btn_submit)).perform(scrollTo(), click()); ready(R.id.entry_name)
            onView(withId(R.id.entry_btn_remove)).perform(click())
            onView(withId(R.id.dialog_btn_continue)).perform(click()); ready(R.id.entry_name)
            onView(withId(R.id.entry_btn_remove)).perform(click())
            onView(withId(R.id.dialog_btn_discard)).perform(click()); ready(R.id.record_empty_card)
            Assert.assertEquals(oldIds, controller.getAllWorkouts().map { it.id }.toSet())
        }
    }
}
