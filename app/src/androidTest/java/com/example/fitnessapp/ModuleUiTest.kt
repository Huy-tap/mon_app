package com.example.fitnessapp

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
import com.example.fitnessapp.data.ReminderAvailability
import com.example.fitnessapp.model.*
import com.example.fitnessapp.ui.*
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class ModuleUiTest {
    @get:Rule val compose = createComposeRule()
    private val month = YearMonth.of(2026, 9)
    private val reminder = Reminder(id = 1, isEnabled = true, reminderTime = "18:30:00")
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // System bar appearance animates separately from Compose's idle clock.
        android.os.SystemClock.sleep(300)
        val directory = InstrumentationRegistry.getArguments().getString("screenshotDir") ?: "module-qa"
        val file = File(context.getExternalFilesDir(null), "$directory/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun statsMonthCancelConfirmAndRotation() {
        val restoration = StateRestorationTester(compose)
        var confirmed = month
        restoration.setContent { FitnessAppTheme {
            var selected by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(month.toString()) }
            val m = YearMonth.parse(selected)
            StatsScreen(StatsUiState.Success(m, MonthlyStats(12, 525, 36), listOf(3, 4, 2, 3)), m,
                { selected = it.toString(); confirmed = it }, {}, {}, {}, {})
        } }
        listOf("Buổi tập", "Phút tập", "Lượt bài xong").forEach { title ->
            listOf("value", "label").forEach { kind ->
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                compose.onNodeWithTag("stats/$kind/$title", useUnmergedTree = true)
                    .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertEquals(1, layouts.single().lineCount)
                assertFalse("$kind/$title must fit its card", layouts.single().hasVisualOverflow)
            }
        }
        capture("stats-data")
        compose.onNodeWithText("Tháng 09/2026").performClick()
        capture("stats-month-sheet")
        compose.onNodeWithTag("month/10").performScrollTo().performClick()
        compose.onNodeWithText("Hủy").performScrollTo().performClick()
        assertEquals(month, confirmed)
        compose.onNodeWithText("Tháng 09/2026").performClick()
        compose.onNodeWithContentDescription("Năm sau").performClick()
        compose.onNodeWithTag("month/10").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Năm 2027").assertExists()
        compose.onNodeWithText("Xác nhận").performScrollTo().performClick()
        assertEquals(YearMonth.of(2027, 10), confirmed)
        compose.onNodeWithText("Tháng 10/2027").assertExists()
    }
    @Test fun statsLoadingErrorRetryAndEmptyRecordAreDistinct() {
        var state by mutableStateOf<StatsUiState>(StatsUiState.Loading)
        var retries = 0
        var record = false
        var bell = false
        compose.setContent { FitnessAppTheme {
            StatsScreen(state, month, {}, {}, { bell = true }, { record = true }, { retries++ })
        } }
        compose.onNodeWithTag("stats/loading").assertExists()
        compose.onNodeWithText("Buổi tập").assertDoesNotExist()
        compose.runOnIdle { state = StatsUiState.Error("Không thể tải dữ liệu. Vui lòng thử lại.") }
        capture("stats-error")
        compose.onNodeWithText("Thử lại").performScrollTo().performClick()
        assertEquals(1, retries)
        compose.runOnIdle { state = StatsUiState.Success(month, MonthlyStats(), listOf(0, 0, 0, 0)) }
        capture("stats-empty")
        compose.onNodeWithText("Bắt đầu tập luyện").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Nhắc nhở tập luyện").performClick()
        assertTrue(record); assertTrue(bell)
    }
    @Test fun settingsLightDarkUnconfiguredAndBlocked() {
        var dark by mutableStateOf(false)
        var r by mutableStateOf(reminder)
        var allowed by mutableStateOf(true)
        var opened = false
        compose.setContent { FitnessAppTheme(dark) {
            SettingsScreen(r, dark, { dark = it }, { opened = true }, {}, ReminderAvailability(allowed, true, ReminderRules.next(r)))
        } }
        capture("settings-light")
        compose.onNodeWithText("🌙 Tối").performClick()
        capture("settings-dark")
        assertTrue(dark)
        compose.runOnIdle { dark = false; r = Reminder() }
        compose.onNodeWithText("Chưa thiết lập").assertExists()
        capture("settings-unconfigured")
        compose.runOnIdle { r = reminder; allowed = false }
        compose.onNodeWithText("Chưa hoạt động").assertExists()
        capture("settings-blocked")
        compose.onNodeWithText("Thiết lập lịch nhắc").performScrollTo().performClick()
        assertTrue(opened)
    }
    @Test fun reminderTimeValidationCancelConfirmDaysSaveAndRestore() {
        val restoration = StateRestorationTester(compose)
        var saved: Reminder? = null
        var persisted by mutableStateOf(reminder)
        restoration.setContent { FitnessAppTheme {
            ReminderSettingsScreen(persisted, {}, ReminderAvailability(nextAt = ReminderRules.next(persisted)), onSave = { saved = it; persisted = it })
        } }
        capture("reminder-daily")
        compose.onNodeWithText("Thay đổi").performScrollTo().performClick()
        capture("reminder-time-sheet")
        compose.onNodeWithTag("time/Giờ").performTextReplacement("25")
        compose.onNodeWithText("Xác nhận").assertIsNotEnabled()
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Hủy").performScrollTo().performClick()
        compose.onNodeWithTag("reminder/time").assertTextEquals("18:30")
        compose.onNodeWithText("Thay đổi").performScrollTo().performClick()
        compose.onNodeWithTag("time/Giờ").performTextReplacement("07")
        compose.onNodeWithTag("time/Phút").performTextReplacement("00")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Xác nhận").performScrollTo().performClick()
        compose.onNodeWithText("Theo tuần").performScrollTo().performClick()
        compose.onNodeWithText("Lưu cài đặt").performClick()
        compose.onNodeWithText("Chọn ít nhất một ngày trong tuần.").assertExists()
        assertNull(saved)
        listOf(1, 2, 4, 6).forEach { compose.onNodeWithTag("day/$it").performScrollTo().performClick() }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("day/1").assertIsSelected()
        compose.onNodeWithTag("day/3").assertIsNotSelected()
        compose.onNodeWithTag("reminder/time").assertTextEquals("07:00")
        compose.onNodeWithText("Lưu cài đặt").performClick()
        assertEquals("07:00:00", saved!!.reminderTime)
        assertEquals("1,2,4,6", saved!!.repeatDays)
        capture("reminder-weekly")
    }
    @Test fun blockedFormRetainsEnabledConfigurationAndSaveFailure() {
        var saved: Reminder? = null
        compose.setContent { FitnessAppTheme {
            ReminderSettingsScreen(reminder, {}, ReminderAvailability(notifications = false), saveError = "Không thể lưu lịch", onSave = { saved = it })
        } }
        compose.onNodeWithText("Mở cài đặt hệ thống").assertExists()
        compose.onNodeWithTag("reminder/enabled").assertIsOn()
        capture("reminder-blocked")
        compose.onNodeWithText("Lưu cài đặt").performClick()
        assertTrue(saved!!.isEnabled)
        compose.onNodeWithText("Không thể lưu lịch").assertExists()
    }
}
