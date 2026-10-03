package com.example.fitnessapp

import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.test.espresso.Espresso
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.FitnessDatabase
import com.example.fitnessapp.model.Exercise
import com.example.fitnessapp.ui.FitnessMainApp
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FitnessFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: SQLiteDatabase
    private lateinit var file: File
    private lateinit var controller: FitnessController
    @Before fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File.createTempFile("fitness-flow-", ".db", context.cacheDir)
        context.assets.open("fitness_app.db").use { input -> file.outputStream().use { input.copyTo(it) } }
        db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
        db.setForeignKeyConstraintsEnabled(true); FitnessDatabase.migrate(db)
        db.execSQL("DELETE FROM workouts"); db.execSQL("DELETE FROM exercises"); db.execSQL("DELETE FROM reminders")
        controller = FitnessController(db)
        controller.insertExercise(Exercise(name = "Flow Push", muscleGroup = "Ngực", defaultSets = 3, defaultReps = 12))
        compose.setContent { FitnessMainApp { controller } }
        waitText("Ghi nhận buổi tập")
    }
    @After fun close() { compose.waitForIdle(); db.close(); file.delete() }
    private fun waitText(text: String) {
        try { compose.waitUntil(10000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
        catch (failure: AssertionError) { compose.onRoot().printToLog("FitnessFlowFailure"); throw failure }
    }
    private fun click(text: String) {
        // The native IME animates outside Compose's idle clock. Finish it before
        // tapping a footer whose position follows the keyboard inset.
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onAllNodesWithText(text).onLast().performClick()
        compose.waitForIdle()
    }
    private fun field(tag: String, text: String) { compose.onNodeWithTag(tag).performScrollTo().performTextReplacement(text) }

    @Test fun recordEditGuideSaveAndHistoryUseActualInput() {
        click("Ghi nhận buổi tập"); click("＋ Chọn bài tập"); click("Flow Push")
        field("set/1", "17"); field("set/2", "13"); field("set/3", "9")
        field("field/Phút", "2"); field("field/Giây", "31"); field("field/Ghi chú", "Actual note")
        compose.onNodeWithText("▷ Xem hướng dẫn").performScrollTo().performClick()
        waitText("Chi tiết bài tập")
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithTag("set/1").assertTextEquals("17")
        click("Thêm vào phiếu"); waitText("17 / 13 / 9 lần · 2 phút 31 giây")
        click("Sửa"); field("set/2", "14")
        compose.onNodeWithTag("set/2").assertTextEquals("14")
        click("Lưu thay đổi")
        waitText("17 / 14 / 9 lần · 2 phút 31 giây")
        click("Lưu buổi tập"); waitText("Chi tiết buổi tập")
        compose.runOnIdle {
            val workout = controller.getAllWorkouts().single()
            assertEquals(listOf(17, 14, 9), workout.items.single().sets.map { it.reps })
            assertEquals(151, workout.items.single().durationSeconds)
            assertEquals("Actual note", workout.items.single().notes)
            assertTrue(controller.getDraft().entries.isEmpty())
        }
        compose.onNodeWithText("Hiệp 2: 14 lần").assertExists()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithContentDescription("Tháng trước").performClick()
        waitText("Tháng này chưa có buổi tập")
    }
    @Test fun removeEntryStaysRemovedAfterChoosingAgain() {
        click("Ghi nhận buổi tập"); click("＋ Chọn bài tập"); click("Flow Push")
        field("field/Phút", "1"); click("Thêm vào phiếu"); waitText("Sửa")
        click("×"); click("Bỏ bài"); waitText("Chưa có bài tập trong phiếu")
        click("＋ Chọn bài tập"); compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithText("Chưa có bài tập trong phiếu").assertExists()
        compose.onNodeWithText("Lưu buổi tập").assertIsNotEnabled()
        compose.runOnIdle { assertTrue(controller.getDraft().entries.isEmpty()) }
    }
    @Test fun addingExerciseStartsCleanAndDuplicateShowsFailure() {
        click("Bài tập"); click("＋ Thêm bài tập")
        compose.onNodeWithTag("field/Tên bài tập *").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        field("field/Tên bài tập *", "Flow Push")
        click("Chọn nhóm cơ"); click("Ngực"); click("Lưu bài tập")
        waitText("Tên bài tập đã tồn tại. Hãy chọn tên khác.")
        click("Đóng")
        field("field/Tên bài tập *", "Flow Squat"); click("Lưu bài tập")
        waitText("Flow Squat")
        compose.runOnIdle { assertEquals(2, controller.getAllExercises().size) }
    }
}
