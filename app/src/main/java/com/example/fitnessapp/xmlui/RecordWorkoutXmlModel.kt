package com.example.fitnessapp.xmlui

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.model.DraftEntry
import com.example.fitnessapp.model.Exercise
import com.example.fitnessapp.model.WorkoutDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * ViewModel quản lý dữ liệu và điều hướng route cho module Ghi nhận buổi tập.
 * Kiến trúc stack navigation tương tự ExerciseXmlModel.
 */
class RecordWorkoutXmlModel(application: Application) : AndroidViewModel(application) {
    val revision = MutableStateFlow(0)
    var ready = false
    var busy = false
    var error: String? = null
    var finished = false
    var dark = false

    lateinit var controller: FitnessController
    var exercises = emptyList<Exercise>()

    // Ngăn xếp màn hình: "record" (Ảnh 2 & 5), "select" (Ảnh 3), "result/<id>" (Ảnh 4)
    val stack = arrayListOf<String>()
    val screen get() = stack.lastOrNull() ?: "record"

    var draft = WorkoutDraft(date = LocalDate.now().toString())
    var selectedCategory = "Tất cả"

    fun changed() { revision.value++ }

    fun start(saved: Bundle?) {
        if (ready || busy) return
        if (saved != null) {
            val savedStack = saved.getStringArrayList("stack")
            if (!savedStack.isNullOrEmpty()) {
                stack.clear()
                stack.addAll(savedStack)
            }
            selectedCategory = saved.getString("selectedCategory", "Tất cả")
        }
        if (stack.isEmpty()) {
            stack.add("record")
        }

        busy = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    controller = FitnessController(getApplication<Application>())
                    exercises = controller.getAllExercises()
                    dark = controller.getState("dark_theme") == "true"
                    // Nạp bản nháp đã lưu dở nếu có
                    val savedDraft = controller.getDraft()
                    if (savedDraft.entries.isNotEmpty()) {
                        draft = savedDraft
                    }
                }
                ready = true
            } catch (e: Exception) {
                error = "Không tải được danh mục bài tập."
            } finally {
                busy = false
                changed()
            }
        }
    }

    fun saveState() = Bundle().apply {
        putStringArrayList("stack", ArrayList(stack))
        putString("selectedCategory", selectedCategory)
    }

    fun navigate(route: String) {
        stack.add(route)
        changed()
    }

    fun back() {
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        } else {
            finished = true
        }
        changed()
    }

    fun backToRecord() {
        while (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        }
        changed()
    }

    fun updateDate(newDate: String) {
        draft = draft.copy(date = newDate)
        changed()
    }

    fun addOrUpdateEntry(entry: DraftEntry) {
        val existingIndex = draft.entries.indexOfFirst { it.exerciseId == entry.exerciseId }
        val newEntries = if (existingIndex >= 0) {
            draft.entries.toMutableList().apply { set(existingIndex, entry) }
        } else {
            draft.entries + entry
        }
        draft = draft.copy(entries = newEntries)

        // Lưu bản nháp vào SQLite để không mất dữ liệu khi thoát đột ngột
        viewModelScope.launch(Dispatchers.IO) {
            controller.saveDraft(draft)
        }
        backToRecord()
    }

    fun removeEntry(exerciseId: Long) {
        draft = draft.copy(entries = draft.entries.filter { it.exerciseId != exerciseId })
        viewModelScope.launch(Dispatchers.IO) {
            controller.saveDraft(draft)
        }
        changed()
    }

    fun clearDraft() {
        draft = WorkoutDraft(date = LocalDate.now().toString())
        viewModelScope.launch(Dispatchers.IO) {
            controller.setState("workout_draft", "")
        }
    }

    fun findExercise(id: Long): Exercise? = exercises.find { it.id == id }

    fun findDraftEntry(exerciseId: Long): DraftEntry? = draft.entries.find { it.exerciseId == exerciseId }

    fun saveWorkout(onSuccess: (Long) -> Unit, onError: (String) -> Unit) {
        if (draft.entries.isEmpty()) {
            onError("Chưa có bài tập trong phiếu.")
            return
        }
        busy = true
        changed()
        viewModelScope.launch {
            try {
                val workoutId = withContext(Dispatchers.IO) {
                    controller.saveWorkout(draft)
                }
                finished = true
                onSuccess(workoutId)
            } catch (e: Exception) {
                onError(e.message ?: "Không thể lưu buổi tập.")
            } finally {
                busy = false
                changed()
            }
        }
    }
}
