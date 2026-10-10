package com.example.fitnessapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.MediaStorage
import com.example.fitnessapp.model.*
import com.example.fitnessapp.xmlui.ExerciseXmlActivity
import com.example.fitnessapp.xmlui.ExerciseXmlModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseXmlTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun launch(route: String = "add"): ActivityScenario<ExerciseXmlActivity> {
        val scenario = ActivityScenario.launch<ExerciseXmlActivity>(Intent(context, ExerciseXmlActivity::class.java).putExtra("route", route))
        waitReady(scenario)
        return scenario
    }
    private fun waitReady(scenario: ActivityScenario<ExerciseXmlActivity>) {
        val end = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < end) {
            var ready = false
            scenario.onActivity { val model = ViewModelProvider(it)[ExerciseXmlModel::class.java]; ready = model.ready && !model.busy }
            if (ready) { InstrumentationRegistry.getInstrumentation().waitForIdleSync(); return }
            Thread.sleep(50)
        }
        fail("XML screen did not finish loading")
    }
    @Test fun cameraResultDuringProcessRestorationIsNotLost() {
        val file = MediaStorage.newFile(context, "jpg")
        file.outputStream().use { Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 80, it) }
        val saved = android.os.Bundle().apply {
            putStringArrayList("stack", arrayListOf("add"))
            putString("cameraPath", file.absolutePath)
            putBundle("form", android.os.Bundle().apply {
                putString("name", "Ảnh sau khôi phục"); putString("muscle", "Ngực"); putString("sets", "3"); putString("reps", "12")
            })
        }
        lateinit var model: ExerciseXmlModel
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            model = ExerciseXmlModel(context.applicationContext as android.app.Application)
            model.start("add", saved)
            model.captured(true)
        }
        val end = System.currentTimeMillis() + 10000
        var restored = false
        while (System.currentTimeMillis() < end && !restored) {
            instrumentation.runOnMainSync { restored = model.form.getString("image") == file.absolutePath && !model.busy }
            Thread.sleep(50)
        }
        assertTrue("Pending photo result was discarded while restoring SQLite", restored)
        assertEquals("Ảnh sau khôi phục", model.form.getString("name"))
        file.delete(); file.parentFile?.delete()
    }
    @Test fun formAndSelectionSurviveActivityRecreation() {
        launch().use { scenario ->
            onView(withId(R.id.ex_name_input)).perform(replaceText("XML giữ trạng thái"), closeSoftKeyboard())
            onView(withId(R.id.ex_sets_input)).perform(replaceText("5"), closeSoftKeyboard())
            onView(withId(R.id.ex_muscle)).perform(click())
            onView(withText("Tay")).perform(click())
            scenario.recreate(); waitReady(scenario)
            onView(withId(R.id.ex_name_input)).check(matches(withText("XML giữ trạng thái")))
            onView(withId(R.id.ex_sets_input)).check(matches(withText("5")))
            onView(withId(R.id.ex_muscle_value)).check(matches(withText("Tay")))
        }
    }
    @Test fun cancelledPhotoAndVideoSheetsKeepInput() {
        launch().use {
            onView(withId(R.id.ex_name_input)).perform(replaceText("XML hủy chọn media"), closeSoftKeyboard())
            onView(withId(R.id.ex_image_add)).perform(scrollTo(), click())
            onView(withId(R.id.ex_source_camera)).check(matches(withText("Chụp ảnh bằng camera")))
            onView(withId(R.id.ex_source_cancel)).perform(click())
            onView(withId(R.id.ex_video_add)).perform(scrollTo(), click())
            onView(withId(R.id.ex_source_camera)).check(matches(withText("Quay video bằng camera")))
            onView(withId(R.id.ex_source_gallery)).check(matches(withText("Chọn video từ thư viện")))
            onView(withId(R.id.ex_source_cancel)).perform(click())
            onView(withId(R.id.ex_name_input)).perform(scrollTo()).check(matches(withText("XML hủy chọn media")))
        }
    }
    @Test fun addValidExercisePersistsToTheExistingDatabase() {
        val name = "XML add ${System.nanoTime()}"
        launch("list").use { scenario ->
            onView(withId(R.id.ex_add)).perform(click())
            onView(withId(R.id.ex_name_input)).perform(replaceText(name), closeSoftKeyboard())
            onView(withId(R.id.ex_muscle)).perform(click())
            onView(withText("Vai")).perform(click())
            onView(withId(R.id.ex_save)).perform(click())
            waitReady(scenario)
            onView(withId(R.id.ex_list)).check(matches(isDisplayed()))
            val saved = FitnessController(context).getAllExercises().single { it.name == name }
            assertEquals("Vai", saved.muscleGroup)
            assertEquals(3, saved.defaultSets)
            assertEquals(12, saved.defaultReps)
        }
    }
    @Test fun updatePersistsAndDoesNotRenameWorkoutHistory() {
        val controller = FitnessController(context)
        val name = "XML edit ${System.nanoTime()}"
        val id = controller.insertExercise(Exercise(name = name, muscleGroup = "Ngực"))
        val workout = controller.saveWorkout(WorkoutDraft(entries = listOf(DraftEntry(id, name, "Ngực", listOf(10), 60))))
        launch("detail/$id").use { scenario ->
            onView(withId(R.id.ex_save)).perform(click())
            onView(withId(R.id.ex_name_input)).perform(replaceText("$name updated"), closeSoftKeyboard())
            onView(withId(R.id.ex_save)).perform(click())
            waitReady(scenario)
            onView(withId(R.id.ex_detail_name)).check(matches(withText("$name updated")))
            assertEquals("$name updated", controller.getExerciseById(id)?.name)
            assertEquals(name, controller.getWorkoutDetail(workout)?.items?.first()?.exerciseName)
        }
    }
    @Test fun deleteRequiresConfirmationAndPreservesWorkout() {
        val controller = FitnessController(context)
        val name = "XML delete ${System.nanoTime()}"
        val id = controller.insertExercise(Exercise(name = name, muscleGroup = "Ngực"))
        val workout = controller.saveWorkout(WorkoutDraft(entries = listOf(DraftEntry(id, name, "Ngực", listOf(10), 60))))
        launch("detail/$id").use {
            onView(withId(R.id.ex_delete)).perform(click())
            onView(withId(R.id.ex_dialog_cancel)).perform(click())
            assertTrue(controller.getAllExercises().any { it.id == id })
            onView(withId(R.id.ex_delete)).perform(click())
            onView(withId(R.id.ex_dialog_confirm)).perform(click())
            val end = System.currentTimeMillis() + 5000
            while (controller.getAllExercises().any { it.id == id } && System.currentTimeMillis() < end) Thread.sleep(50)
            assertFalse(controller.getAllExercises().any { it.id == id })
            assertEquals(name, controller.getWorkoutDetail(workout)?.items?.first()?.exerciseName)
        }
    }
    @Test fun scrollingNeverPaintsOverAddButton() {
        launch("list").use { scenario ->
            var before: Bitmap? = null
            fun pixels(activity: ExerciseXmlActivity): Bitmap {
                val root = activity.findViewById<View>(R.id.ex_root)
                val button = activity.findViewById<View>(R.id.ex_add)
                val rootXY = IntArray(2); val buttonXY = IntArray(2)
                root.getLocationOnScreen(rootXY); button.getLocationOnScreen(buttonXY)
                val frame = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                root.draw(Canvas(frame))
                return Bitmap.createBitmap(frame, buttonXY[0] - rootXY[0], buttonXY[1] - rootXY[1], button.width, button.height)
            }
            scenario.onActivity { before = pixels(it); it.findViewById<RecyclerView>(R.id.ex_list).scrollBy(0, 550) }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { assertTrue("Exercise rows painted over add button", before!!.sameAs(pixels(it))) }
            onView(withId(R.id.ex_add)).perform(click())
            onView(withId(R.id.ex_name_input)).check(matches(isDisplayed()))
        }
    }
    @Test fun deleteThenAddSameNameThroughUiKeepsOldWorkout() {
        val controller = FitnessController(context)
        val name = "XML thêm lại ${System.nanoTime()}"
        val oldId = controller.insertExercise(Exercise(name = name, muscleGroup = "Ngực"))
        val workout = controller.saveWorkout(WorkoutDraft(entries = listOf(DraftEntry(oldId, name, "Ngực", listOf(10), 60))))
        val before = controller.getWorkoutDetail(workout)
        launch("detail/$oldId").use {
            onView(withId(R.id.ex_delete)).perform(click())
            onView(withId(R.id.ex_dialog_confirm)).perform(click())
            val end = System.currentTimeMillis() + 5000
            while (controller.getAllExercises().any { it.id == oldId } && System.currentTimeMillis() < end) Thread.sleep(50)
            assertFalse(controller.getAllExercises().any { it.id == oldId })
        }
        launch("list").use { scenario ->
            onView(withId(R.id.ex_add)).perform(click())
            onView(withId(R.id.ex_name_input)).perform(replaceText(name), closeSoftKeyboard())
            onView(withId(R.id.ex_muscle)).perform(click())
            onView(withText("Ngực")).perform(click())
            onView(withId(R.id.ex_save)).perform(click())
            waitReady(scenario)
            onView(withId(R.id.ex_list)).check(matches(isDisplayed()))
            val newExercise = controller.getAllExercises().single { it.name == name }
            assertTrue(newExercise.id > oldId)
            assertEquals(before, controller.getWorkoutDetail(workout))
        }
    }
}
