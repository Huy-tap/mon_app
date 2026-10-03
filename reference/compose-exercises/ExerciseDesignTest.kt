package com.example.fitnessapp

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fitnessapp.model.Exercise
import com.example.fitnessapp.ui.*
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDesignTest {
    @get:Rule val compose = createComposeRule()

    @Test fun muscleMenuOffersDesignGroupsAndPersistsSelectedValue() {
        var saved: Exercise? = null
        compose.setContent { FitnessAppTheme { AddExerciseScreen({}, { saved = it }) } }
        compose.onNodeWithTag("field/Tên bài tập *").performTextInput("Bài kiểm tra giao diện")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("exercise/muscle-picker").performClick()
        listOf("Ngực", "Lưng", "Chân", "Tay", "Bụng", "Vai", "Toàn thân").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Tay trước").assertDoesNotExist()
        compose.onNodeWithText("Tim mạch").assertDoesNotExist()
        compose.onNodeWithText("Tay").performClick()
        compose.onNodeWithText("Lưu bài tập").performClick()
        compose.runOnIdle { assertEquals("Tay", saved?.muscleGroup) }
    }

    @Test fun cancelSourceSheetsPreservesTheForm() {
        compose.setContent { FitnessAppTheme { AddExerciseScreen({}, {}) } }
        compose.onNodeWithTag("field/Tên bài tập *").performTextInput("Giữ nội dung")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("＋ Thêm ảnh").performScrollTo().performClick()
        compose.onNodeWithText("Chụp ảnh bằng camera").assertIsDisplayed()
        compose.onNodeWithText("Chọn ảnh từ thư viện").assertIsDisplayed()
        compose.onNodeWithText("Hủy").performClick()
        compose.onNodeWithText("＋ Thêm video").performScrollTo().performClick()
        compose.onNodeWithText("Quay video bằng camera").assertIsDisplayed()
        compose.onNodeWithText("Chọn video từ thư viện").assertIsDisplayed()
        compose.onNodeWithText("Hủy").performClick()
        compose.onNodeWithTag("field/Tên bài tập *").performScrollTo().assertTextEquals("Giữ nội dung")
    }

    @Test fun deleteDialogKeepsTheExerciseUntilConfirmed() {
        var deleted = false
        compose.setContent { FitnessAppTheme {
            ExerciseDetailScreen(Exercise(id = 99, name = "Hít đất", muscleGroup = "Ngực"), {}, {}, { deleted = true }, {})
        } }
        compose.onNodeWithText("Xóa").performClick()
        compose.onNodeWithText("Giữ lại").performClick()
        compose.runOnIdle { assertFalse(deleted) }
        compose.onNodeWithText("Xóa").performClick()
        compose.onNodeWithText("Xóa bài tập").performClick()
        compose.runOnIdle { assertTrue(deleted) }
    }
}
