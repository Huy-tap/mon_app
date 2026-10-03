package com.example.fitnessapp.xmlui

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.MediaStorage
import com.example.fitnessapp.model.Exercise
import com.example.fitnessapp.model.FitnessRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Holds the XML screens' state across rotation and external camera/gallery activities. */
class ExerciseXmlModel(application: Application) : AndroidViewModel(application) {
    val revision = MutableStateFlow(0)
    var ready = false
    var busy = false
    var mediaBusy = false
    var dark = false
    var error: String? = null
    var exercises = emptyList<Exercise>()
    lateinit var controller: FitnessController
    val stack = arrayListOf<String>()
    var form = Bundle()
    var original: Exercise? = null
    var cameraPath: String? = null
    var videoCapture = false
    var previewPath: String? = null
    var sheet: String? = null
    var attempted = false
    var finished = false
    var listPosition = 0
    var listOffset = 0
    var formScroll = 0
    var detailScroll = 0
    var playerPosition = 0
    var playerPlaying = true
    var zoomScale = 1f
    var zoomX = 0f
    var zoomY = 0f
    val screen get() = stack.lastOrNull() ?: "list"
    fun changed() { revision.value++ }

    fun start(route: String, saved: Bundle?) {
        if (ready || busy) return
        if (stack.isEmpty()) {
            stack.addAll(saved?.getStringArrayList("stack") ?: arrayListOf(route))
            form = saved?.getBundle("form") ?: Bundle()
            cameraPath = saved?.getString("cameraPath")
            videoCapture = saved?.getBoolean("videoCapture") ?: false
            previewPath = saved?.getString("previewPath")
            sheet = saved?.getString("sheet")
            attempted = saved?.getBoolean("attempted") ?: false
            listPosition = saved?.getInt("listPosition") ?: 0
            listOffset = saved?.getInt("listOffset") ?: 0
            formScroll = saved?.getInt("formScroll") ?: 0
            detailScroll = saved?.getInt("detailScroll") ?: 0
            playerPosition = saved?.getInt("playerPosition") ?: 0
            playerPlaying = saved?.getBoolean("playerPlaying", true) ?: true
            zoomScale = saved?.getFloat("zoomScale", 1f) ?: 1f
            zoomX = saved?.getFloat("zoomX") ?: 0f
            zoomY = saved?.getFloat("zoomY") ?: 0f
        }
        task {
            controller = FitnessController(getApplication<Application>())
            reload()
            dark = controller.getState("dark_theme") == "true"
            ready = true
            if (screen.startsWith("image/") || screen.startsWith("video/")) {
                val isVideo = screen.startsWith("video/")
                val e = selected()
                previewPath = if (isVideo) e?.instructionVideo else e?.instructionImage
                stack[stack.lastIndex] = if (isVideo) "video" else "image"
            }
            val formRoute = stack.lastOrNull { it == "add" || it.startsWith("edit/") }
            if (formRoute != null) {
                original = formRoute.substringAfter('/', "").toLongOrNull()?.let(controller::getExerciseById)
                if (form.isEmpty) prepareForm(original)
            }
        }
    }
    fun saveState() = Bundle().apply {
        putStringArrayList("stack", ArrayList(stack)); putBundle("form", Bundle(form))
        putString("cameraPath", cameraPath); putBoolean("videoCapture", videoCapture)
        putString("previewPath", previewPath); putString("sheet", sheet); putBoolean("attempted", attempted)
        putInt("listPosition", listPosition); putInt("listOffset", listOffset)
        putInt("formScroll", formScroll); putInt("detailScroll", detailScroll)
        putInt("playerPosition", playerPosition); putBoolean("playerPlaying", playerPlaying)
        putFloat("zoomScale", zoomScale); putFloat("zoomX", zoomX); putFloat("zoomY", zoomY)
    }
    private fun reload() { exercises = controller.getAllExercises() }
    fun selected(): Exercise? = screen.substringAfter('/', "").toLongOrNull()?.let { id -> exercises.find { it.id == id } }
    fun navigate(route: String) {
        if (route == "add" || route.startsWith("edit/")) {
            original = route.substringAfter('/', "").toLongOrNull()?.let { id -> exercises.find { it.id == id } }
            prepareForm(original)
        }
        if (route.startsWith("detail/")) detailScroll = 0
        stack.add(route); changed()
    }
    fun back() {
        sheet = null
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else finished = true
        changed()
    }
    private fun prepareForm(e: Exercise?) {
        form = Bundle().apply {
            putString("name", e?.name ?: ""); putString("muscle", e?.muscleGroup ?: "")
            putString("sets", (e?.defaultSets ?: 3).toString())
            putString("reps", (if (e?.trackingType == "TIME") e.defaultDurationSeconds ?: 30 else e?.defaultReps ?: 12).toString())
            putString("image", e?.instructionImage); putString("video", e?.instructionVideo)
        }
        attempted = false; formScroll = 0
    }
    fun draft(): Exercise = (original ?: Exercise(name = "", muscleGroup = "")).copy(
        name = form.getString("name", "").trim(), muscleGroup = form.getString("muscle", ""),
        defaultSets = form.getString("sets", "").toIntOrNull() ?: 0,
        defaultReps = if (original?.trackingType == "TIME") 0 else form.getString("reps", "").toIntOrNull() ?: 0,
        defaultDurationSeconds = if (original?.trackingType == "TIME") form.getString("reps", "").toIntOrNull() else null,
        instructionImage = form.getString("image"), instructionVideo = form.getString("video"))
    fun dirty(): Boolean {
        val e = original
        return form.getString("name", "") != (e?.name ?: "") || form.getString("muscle", "") != (e?.muscleGroup ?: "") ||
            form.getString("sets", "") != (e?.defaultSets ?: 3).toString() ||
            form.getString("reps", "") != (if (e?.trackingType == "TIME") e.defaultDurationSeconds ?: 30 else e?.defaultReps ?: 12).toString() ||
            form.getString("image") != e?.instructionImage || form.getString("video") != e?.instructionVideo
    }
    fun save() {
        attempted = true
        val exercise = draft()
        if (FitnessRules.exerciseError(exercise) != null) { changed(); return }
        task {
            if (original == null) controller.insertExercise(exercise) else check(controller.updateExercise(exercise))
            reload()
            if (stack.size > 1) stack.removeAt(stack.lastIndex) else finished = true
            form = Bundle(); original = null
        }
    }
    fun delete(e: Exercise) = task {
        check(controller.deleteExercise(e.id))
        val draft = controller.getDraft()
        controller.saveDraft(draft.copy(entries = draft.entries.filterNot { it.exerciseId == e.id }))
        reload()
        stack.removeAll { it.endsWith("/${e.id}") }
        if (stack.isEmpty()) finished = true
    }
    fun importMedia(uri: Uri?, video: Boolean) {
        if (uri == null) return
        if (!ready || busy) {
            viewModelScope.launch { revision.first { ready && !busy }; importMedia(uri, video) }
            return
        }
        mediaBusy = true
        task { form.putString(if (video) "video" else "image", MediaStorage.import(getApplication(), uri, video)) }
    }
    fun captured(ok: Boolean) {
        // Activity results can arrive during database initialization after process recreation.
        if (!ready || busy) {
            viewModelScope.launch { revision.first { ready && !busy }; captured(ok) }
            return
        }
        val path = cameraPath ?: return
        val video = videoCapture
        cameraPath = null
        mediaBusy = true
        task {
            val file = File(path)
            val valid = ok && file.length() in 1..((if (video) 100L else 10L) * 1024 * 1024) &&
                (!video || MediaStorage.videoDuration(path) != null)
            if (valid) form.putString(if (video) "video" else "image", path)
            else {
                file.delete(); file.parentFile?.delete()
                if (ok) error = if (video) "Video không hợp lệ hoặc vượt giới hạn 100 MiB." else "Ảnh trống hoặc vượt giới hạn 10 MiB."
            }
        }
    }
    fun preview(path: String?, video: Boolean) {
        previewPath = path; playerPosition = 0; playerPlaying = true
        zoomScale = 1f; zoomX = 0f; zoomY = 0f
        navigate(if (video) "video" else "image")
    }
    private fun task(action: () -> Unit) {
        if (busy) return
        busy = true; changed()
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { action() } }
            catch (e: Exception) {
                error = if (e.message?.contains("exercises.name") == true) "Tên bài tập đã tồn tại. Hãy chọn tên khác."
                    else e.message ?: "Không thể đọc hoặc lưu dữ liệu. Vui lòng thử lại."
            } finally { busy = false; mediaBusy = false; changed() }
        }
    }
}
