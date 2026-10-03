package com.example.fitnessapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessapp.model.*
import java.time.LocalDate
import java.time.YearMonth

import android.database.sqlite.SQLiteConstraintException
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.ReminderScheduler
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class AppData(val exercises: List<Exercise>, val workouts: List<Workout>, val reminder: Reminder, val dark: Boolean, val draft: WorkoutDraft)
private fun FitnessController.snapshot() = AppData(getAllExercises(), getAllWorkouts(), getPrimaryReminder(), getState("dark_theme") == "true", getDraft())

// Code Compose cũ để tham khảo khi phát triển tiếp. MainActivity hiện mở HomeXmlActivity.
// Các module ghi nhận, lịch sử, thống kê, cài đặt đang khóa: Trang này chưa phát triển.
@Composable fun FitnessMainApp(controllerFactory: ((android.content.Context) -> FitnessController)? = null) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val mutex = remember { Mutex() }
    var controller by remember { mutableStateOf<FitnessController?>(null) }
    var data by remember { mutableStateOf<AppData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var stack by rememberSaveable { mutableStateOf(listOf("HOME")) }
    var historyMonth by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var statsMonth by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val holder = rememberSaveableStateHolder()
    val route = stack.last()
    fun push(destination: String) { stack = stack + destination }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun returnToRecord() {
        val recordIndex = stack.indexOfLast { it == "record" }
        stack = if (recordIndex >= 0) stack.take(recordIndex + 1) else listOf("HOME", "record")
    }
    fun clearForm(destination: String) { holder.removeState(destination) }
    fun tab(value: BottomTab) { stack = listOf(value.name) }
    fun mutate(action: FitnessController.() -> Unit, success: () -> Unit = {}) {
        val c = controller ?: return
        scope.launch {
            mutex.withLock {
                busy = true
                try {
                    data = withContext(Dispatchers.IO) { c.action(); c.snapshot() }
                    success()
                } catch (e: Exception) {
                    error = when {
                        e is SQLiteConstraintException && e.message?.contains("exercises.name") == true -> "Tên bài tập đã tồn tại. Hãy chọn tên khác."
                        e is SQLiteConstraintException -> "Không thể lưu dữ liệu. Hãy kiểm tra lại các trường nhập."
                        else -> e.message ?: "Không thể đọc hoặc lưu dữ liệu. Vui lòng thử lại."
                    }
                } finally { busy = false }
            }
        }
    }
    LaunchedEffect(retry) {
        try { data = withContext(Dispatchers.IO) {
            val c = controllerFactory?.invoke(context) ?: FitnessController(context); controller = c
            c.snapshot().also { ReminderScheduler.schedule(context, it.reminder) }
        } } catch (e: Exception) { error = "Không mở được SQLite: ${e.message}" }
    }
    val lifecycle = LocalLifecycleOwner.current
    val latestReminder by rememberUpdatedState(data?.reminder)
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) latestReminder?.let { ReminderScheduler.schedule(context, it) }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    FitnessAppTheme(darkTheme = data?.dark ?: false) {
        val state = data
        BackHandler(enabled = stack.size > 1 || route != "HOME") { if (stack.size > 1) pop() else tab(BottomTab.HOME) }
        if (state == null) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
            if (error == null) CircularProgressIndicator() else TextButton({ error = null; retry++ }) { Text("Thử tải lại dữ liệu") }
        } else holder.SaveableStateProvider(route) {
            val parts = route.split("/"); val id = parts.getOrNull(1)?.toLongOrNull()
            val exercise = state.exercises.find { it.id == id }
            fun showExercise() { if (id != null) push("detail/$id") }
            when (parts[0]) {
                "HOME" -> HomeScreen(state.workouts, state.reminder, ::tab, { clearForm("record"); push("record") }, { push("workout/$it") }, { push("reminder") })
                "EXERCISES" -> ExerciseXmlRoute("list", state.dark) { destination ->
                    mutate({}, {
                        tab(BottomTab.entries.find { it.name == destination } ?: BottomTab.HOME)
                        clearForm(route)
                    })
                }
                "HISTORY" -> WorkoutHistoryScreen(state.workouts, YearMonth.parse(historyMonth), { historyMonth = it.toString() }, { push("record") }, { push("workout/$it") }, ::tab)
                "STATS" -> StatsScreen(state.workouts, YearMonth.parse(statsMonth), { statsMonth = it.toString() }, ::tab, { push("reminder") })
                "SETTINGS" -> SettingsScreen(state.reminder, state.dark, { mutate({ setState("dark_theme", it.toString()) }) }, { push("reminder") }, ::tab)
                "reminder" -> ReminderSettingsScreen(state.reminder, ::pop) { r -> mutate({ saveReminder(r); ReminderScheduler.schedule(context, getPrimaryReminder()) }, { pop(); clearForm(route) }) }
                "add", "edit", "detail", "image", "video" -> ExerciseXmlRoute(route, state.dark) {
                    var removed = false
                    mutate({ removed = id != null && getAllExercises().none { it.id == id } }, {
                        if (removed) {
                            stack = stack.filterNot { it == "detail/$id" || it == "result/$id" || it == "edit/$id" || it == "image/$id" || it == "video/$id" }
                            if (stack.isEmpty()) tab(BottomTab.EXERCISES)
                        } else pop()
                        clearForm(route)
                    })
                }
                "record" -> RecordWorkoutScreen(state.draft, { draft -> mutate({ saveDraft(draft) }) }, ::pop, { push("select") }, {
                    clearForm("result/$it"); push("result/$it")
                }, {
                    var savedId = 0L
                    mutate({ savedId = saveWorkout(state.draft) }, {
                        historyMonth = state.draft.date.take(7)
                        stack = listOf("HISTORY", "workout/$savedId")
                        clearForm("record")
                    })
                })
                "select" -> SelectExerciseScreen(state.exercises, ::pop, { clearForm("result/$it"); push("result/$it") }, { clearForm("add"); push("add") })
                "result" -> if (exercise != null) EnterWorkoutResultScreen(exercise, state.draft.entries.find { it.exerciseId == id }, ::returnToRecord, ::showExercise) { entry ->
                    val exists = state.draft.entries.any { it.exerciseId == id }
                    val entries = if (exists) state.draft.entries.map { if (it.exerciseId == id) entry else it } else state.draft.entries + entry
                    mutate({ saveDraft(state.draft.copy(entries = entries)) }, {
                        returnToRecord()
                        clearForm(route)
                    })
                }
                "workout" -> state.workouts.find { it.id == id }?.let { WorkoutDetailScreen(it, ::pop) }
            }
        }
        if (busy) androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp)); Label("Đang lưu…") } }
        }
        error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Label("Chưa hoàn tất", 20, true) }, text = { Label(message) }, confirmButton = { TextButton({ error = null }) { Text("Đóng") } }) }
    }
}
