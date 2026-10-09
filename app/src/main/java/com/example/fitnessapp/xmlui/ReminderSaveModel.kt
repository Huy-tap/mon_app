package com.example.fitnessapp.xmlui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.data.FitnessDatabase
import com.example.fitnessapp.data.FitnessRepository
import com.example.fitnessapp.data.ReminderScheduler
import com.example.fitnessapp.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReminderSaveState(
    val busy: Boolean = false,
    val saved: Reminder? = null,
    val alarmScheduled: Boolean = true,
    val error: String? = null
)

/** Giữ thao tác lưu khi Activity được tạo lại; dùng chung bộ đặt báo thức hiện có. */
class ReminderSaveModel(application: Application) : AndroidViewModel(application) {
    private val mutableState = MutableStateFlow(ReminderSaveState())
    val state = mutableState.asStateFlow()

    fun save(value: Reminder) {
        if (mutableState.value.busy) return
        mutableState.value = ReminderSaveState(busy = true)
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    ReminderScheduler.executor.submit<Pair<Reminder, Boolean>> {
                        val saved = FitnessRepository(FitnessDatabase.open(context)).saveReminder(value)
                        saved to runCatching { ReminderScheduler.schedule(context, saved) }.isSuccess
                    }.get()
                }
                mutableState.value = ReminderSaveState(saved = result.first, alarmScheduled = result.second)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = ReminderSaveState(error = "Không lưu được lịch nhắc. Vui lòng thử lại.")
            }
        }
    }

    fun consumeResult() { mutableState.value = ReminderSaveState() }
}
