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
import com.example.fitnessapp.data.ReminderAvailability
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import androidx.lifecycle.repeatOnLifecycle

private data class AppData(val exercises: List<Exercise>, val workouts: List<Workout>, val reminder: Reminder, val dark: Boolean, val draft: WorkoutDraft)
private fun FitnessController.snapshot() = AppData(getAllExercises(), getAllWorkouts(), getPrimaryReminder(), getState("dark_theme") == "true", getDraft())

@Composable fun FitnessMainApp(recordRequest: String? = null, onRecordRequestHandled: () -> Unit = {},
    controllerFactory: ((android.content.Context) -> FitnessController)? = null) {
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
    var statsState by remember { mutableStateOf<StatsUiState>(StatsUiState.Loading) }
    var statsRetry by remember { mutableIntStateOf(0) }
    var permissionRefresh by remember { mutableIntStateOf(0) }
    var availability by remember { mutableStateOf(ReminderAvailability()) }
    var reminderSaving by remember { mutableStateOf(false) }
    var reminderSaveError by remember { mutableStateOf<String?>(null) }
    var reminderSaved by remember { mutableStateOf(false) }
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
        try {
            val initial = withContext(Dispatchers.IO) {
                val c = controllerFactory?.invoke(context) ?: FitnessController(context)
                val snapshot = c.snapshot()
                val status = if (controllerFactory == null) ReminderScheduler.reconcile(context, c, force = true)
                    else ReminderAvailability(nextAt = ReminderRules.next(snapshot.reminder))
                Triple(c, snapshot, status)
            }
            controller = initial.first; data = initial.second; availability = initial.third
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { error = "Không mở được SQLite: ${e.message}" }
    }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRefresh++
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lifecycle, route) {
        if (route !in listOf("HOME", "SETTINGS", "reminder")) return@LaunchedEffect
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Also observe an alarm delivered while the app remains open.
            while (true) { delay(5000); permissionRefresh++ }
        }
    }
    LaunchedEffect(data?.reminder, permissionRefresh) {
        val c = controller ?: return@LaunchedEffect
        val r = data?.reminder ?: return@LaunchedEffect
        mutex.withLock {
            availability = withContext(Dispatchers.IO) {
                if (controllerFactory == null) ReminderScheduler.reconcile(context, c)
                else ReminderAvailability(nextAt = ReminderRules.next(r))
            }
        }
    }
    LaunchedEffect(route, statsMonth, statsRetry, data?.workouts) {
        if (route != "STATS") return@LaunchedEffect
        val c = controller ?: return@LaunchedEffect
        statsState = StatsUiState.Loading
        mutex.withLock {
            statsState = try {
                withContext(Dispatchers.IO) {
                    val month = YearMonth.parse(statsMonth)
                    val workouts = c.getAllWorkouts()
                    StatsUiState.Success(month, FitnessRules.monthStats(workouts, month), FitnessRules.weeklyCounts(workouts, month))
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { StatsUiState.Error("Không thể tải dữ liệu. Vui lòng thử lại.") }
        }
    }
    LaunchedEffect(recordRequest, controller) {
        val c = controller ?: return@LaunchedEffect
        if (recordRequest == null) return@LaunchedEffect
        try {
            val r = mutex.withLock { withContext(Dispatchers.IO) { c.getPrimaryReminder() } }
            if (r.isEnabled && recordRequest == "${r.id}:${r.revision}") {
                clearForm("record")
                stack = listOf("HOME", "record")
                context.getSystemService(android.app.NotificationManager::class.java).cancel(41)
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { error = "Không thể mở buổi tập. Vui lòng thử lại." }
        onRecordRequestHandled()
    }
    LaunchedEffect(route) { reminderSaved = false; reminderSaveError = null }
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
                "EXERCISES" -> ExerciseScreen(state.exercises, { clearForm("add"); push("add") }, { push("detail/$it") }, ::tab)
                "HISTORY" -> WorkoutHistoryScreen(state.workouts, YearMonth.parse(historyMonth), { historyMonth = it.toString() }, { push("record") }, { push("workout/$it") }, ::tab)
                "STATS" -> StatsScreen(statsState, YearMonth.parse(statsMonth), { statsMonth = it.toString() }, ::tab, { push("reminder") }, { clearForm("record"); push("record") }, { statsRetry++ })
                "SETTINGS" -> SettingsScreen(state.reminder, state.dark, { mutate({ setState("dark_theme", it.toString()) }) }, { push("reminder") }, ::tab, availability)
                "reminder" -> ReminderSettingsScreen(state.reminder, ::pop, availability, reminderSaving, reminderSaveError, reminderSaved,
                    onPermissionChanged = { permissionRefresh++ }) { r ->
                    if (!reminderSaving) scope.launch {
                        mutex.withLock {
                            reminderSaving = true; reminderSaveError = null; reminderSaved = false
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    synchronized(ReminderScheduler.lock) {
                                        val c = checkNotNull(controller)
                                        c.saveReminder(r)
                                        val status = if (controllerFactory == null) ReminderScheduler.reconcile(context, c)
                                            else ReminderAvailability(nextAt = ReminderRules.next(c.getPrimaryReminder()))
                                        c.snapshot() to status
                                    }
                                }
                                data = result.first; availability = result.second; reminderSaved = true
                            } catch (e: Exception) { reminderSaveError = e.message ?: "Không thể lưu lịch. Vui lòng thử lại." }
                            finally { reminderSaving = false }
                        }
                    }
                }
                "add" -> AddExerciseScreen(::pop) { e -> mutate({ insertExercise(e) }, { pop(); clearForm(route) }) }
                "edit" -> if (exercise != null) EditExerciseScreen(exercise, ::pop) { e -> mutate({ check(updateExercise(e)) }, { pop(); clearForm(route) }) }
                "detail" -> if (exercise != null) ExerciseDetailScreen(exercise, ::pop, { clearForm("edit/$id"); push("edit/$id") }, {
                    mutate({ deleteExercise(exercise.id); saveDraft(state.draft.copy(entries = state.draft.entries.filter { it.exerciseId != exercise.id })) }, {
                        stack = stack.filterNot { it == "detail/$id" || it == "result/$id" || it == "edit/$id" }; if (stack.isEmpty()) tab(BottomTab.EXERCISES)
                    })
                }, { video -> push("${if (video) "video" else "image"}/$id") })
                "image" -> ExerciseImageScreen(exercise?.instructionImage, ::pop)
                "video" -> ExerciseVideoScreen(exercise?.instructionVideo, ::pop)
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
