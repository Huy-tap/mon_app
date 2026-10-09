package com.example.fitnessapp.xmlui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.model.Reminder
import com.example.fitnessapp.model.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeXmlData(val workouts: List<Workout>, val reminder: Reminder, val dark: Boolean)
data class HomeXmlState(val data: HomeXmlData? = null, val error: String? = null)

/** Chỉ đọc dữ liệu tổng quan; không tạo/sửa buổi tập khi các module đang khóa. */
class HomeXmlModel(application: Application) : AndroidViewModel(application) {
    private val mutableState = MutableStateFlow(HomeXmlState())
    val state = mutableState.asStateFlow()
    private var loading = false

    fun refresh() {
        if (loading) return
        loading = true
        mutableState.value = mutableState.value.copy(error = null)
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val controller = FitnessController(context)
                    HomeXmlData(controller.getAllWorkouts(), controller.getPrimaryReminder(), controller.getState("dark_theme") == "true")
                }
                mutableState.value = HomeXmlState(data)
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(error = "Không tải được dữ liệu. Vui lòng thử lại.")
            } finally { loading = false }
        }
    }
}
