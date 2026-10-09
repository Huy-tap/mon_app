package com.example.fitnessapp.xmlui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputFilter
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.R
import com.example.fitnessapp.data.MediaStorage
import com.example.fitnessapp.model.FitnessRules
import com.example.fitnessapp.model.muscleLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Exercise management uses ordinary XML layouts and Android Views, with no Compose UI. */
class ExerciseXmlActivity : ComponentActivity() {
    private lateinit var model: ExerciseXmlModel
    private lateinit var content: FrameLayout
    private lateinit var footer: FrameLayout
    private var shownScreen = ""
    private var sourceDialog: Dialog? = null
    private var popup: PopupWindow? = null
    private var busyDialog: Dialog? = null
    private var changingView = false
    private var player: VideoView? = null
    private var zoomImage: ZoomImageView? = null
    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { model.importMedia(it, false) }
    private val videoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { model.importMedia(it, true) }
    private val photoCamera = registerForActivityResult(ActivityResultContracts.TakePicture()) { model.captured(it) }
    private val videoCamera = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { model.captured(it) }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppTheme.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val dark = AppTheme.isDark(this)
        setTheme(if (dark) R.style.Theme_ExerciseXml_Dark else R.style.Theme_ExerciseXml)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_exercise_xml)
        content = findViewById(R.id.ex_content); footer = findViewById(R.id.ex_footer)
        val root = findViewById<View>(R.id.ex_root)
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            findViewById<View>(R.id.ex_bottom_inset).layoutParams = findViewById<View>(R.id.ex_bottom_inset).layoutParams.apply { height = maxOf(bars.bottom, keyboard.bottom) }
            insets
        }
        model = ViewModelProvider(this)[ExerciseXmlModel::class.java]
        findViewById<View>(R.id.ex_back).setOnClickListener { back() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = back()
        })
        lifecycleScope.launch { model.revision.collect { render() } }
        model.start(intent.getStringExtra("route") ?: "list", savedInstanceState?.getBundle("exerciseState"))
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun color(attr: Int): Int = TypedValue().also { theme.resolveAttribute(attr, it, true) }.data
    private fun text(id: Int): TextView = findViewById(id)
    private fun click(id: Int, action: () -> Unit) { findViewById<View>(id).setOnClickListener { if (!model.busy) action() } }
    private fun inflate(layout: Int, parent: ViewGroup): View = layoutInflater.inflate(layout, parent, false).also(parent::addView)
    private fun hideKeyboard() { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(window.decorView.windowToken, 0); currentFocus?.clearFocus() }

    private fun capturePosition() {
        findViewById<RecyclerView?>(R.id.ex_list)?.let { list ->
            val layout = list.layoutManager as LinearLayoutManager
            model.listPosition = layout.findFirstVisibleItemPosition().coerceAtLeast(0)
            model.listOffset = layout.findViewByPosition(model.listPosition)?.top ?: 0
        }
        findViewById<ScrollView?>(R.id.ex_scroll)?.let {
            if (shownScreen == "add" || shownScreen.startsWith("edit/")) model.formScroll = it.scrollY
            else if (shownScreen.startsWith("detail/")) model.detailScroll = it.scrollY
        }
        player?.let { model.playerPosition = it.currentPosition; model.playerPlaying = it.isPlaying }
        zoomImage?.let { model.zoomScale = it.zoom; model.zoomX = it.panX; model.zoomY = it.panY }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        capturePosition(); outState.putBundle("exerciseState", model.saveState()); super.onSaveInstanceState(outState)
    }
    override fun onPause() { capturePosition(); player?.pause(); super.onPause() }
    override fun onResume() { super.onResume(); if (AppTheme.needsRefresh(this)) { recreate(); return }; if (::model.isInitialized && model.playerPlaying) player?.start() }
    override fun onDestroy() {
        changingView = true; sourceDialog?.dismiss(); popup?.dismiss(); busyDialog?.dismiss(); player?.stopPlayback()
        super.onDestroy()
    }
    private fun render() {
        if (!::model.isInitialized || isFinishing) return
        if (model.busy) {
            if (model.mediaBusy && findViewById<View?>(R.id.ex_hint) != null) {
                content.alpha = 0.45f
                text(R.id.ex_hint).text = "Đang sao chép ảnh/video…"
                text(R.id.ex_save).apply { text = "Đang sao chép media…"; isEnabled = false }
                listOf(R.id.ex_name_input, R.id.ex_sets_input, R.id.ex_reps_input).forEach { findViewById<View>(it).isEnabled = false }
                findViewById<View>(R.id.ex_copy_progress).visibility = View.VISIBLE
            } else if (busyDialog == null) busyDialog = AlertDialog.Builder(this).setMessage(if (!model.ready) "Đang tải…" else "Đang lưu…").setCancelable(false).create().also { it.show() }
            return
        }
        busyDialog?.dismiss(); busyDialog = null
        content.alpha = 1f
        if (model.finished) { close(); return }
        if (!model.ready) {
            model.error?.let { message ->
                model.error = null
                AlertDialog.Builder(this).setTitle("Chưa hoàn tất").setMessage(message).setPositiveButton("Thử lại") { _, _ -> model.start(intent.getStringExtra("route") ?: "list", null) }.setNegativeButton("Quay lại") { _, _ -> close() }.show()
            }
            return
        }
        val rememberedSheet = model.sheet
        changingView = true; sourceDialog?.dismiss(); sourceDialog = null; popup?.dismiss(); popup = null
        changingView = false
        player?.stopPlayback(); player = null; zoomImage = null
        content.removeAllViews(); footer.removeAllViews()
        shownScreen = model.screen
        findViewById<View>(R.id.ex_bottom_inset).setBackgroundColor(color(if (shownScreen == "list") R.attr.exSurface else R.attr.exBackground))
        val title = when {
            shownScreen == "list" -> "Bài tập"
            shownScreen == "add" -> "Thêm bài tập"
            shownScreen.startsWith("edit/") -> "Sửa bài tập"
            shownScreen.startsWith("detail/") -> "Chi tiết bài tập"
            else -> "Hướng dẫn bài tập"
        }
        text(R.id.ex_title).apply { this.text = title; textSize = if (shownScreen == "add" || shownScreen.startsWith("edit/")) 22f else 24f }
        findViewById<View>(R.id.ex_back).visibility = if (shownScreen == "list") View.GONE else View.VISIBLE
        when {
            shownScreen == "list" -> renderList()
            shownScreen == "add" || shownScreen.startsWith("edit/") -> renderForm()
            shownScreen.startsWith("detail/") -> renderDetail()
            else -> renderMedia()
        }
        if (rememberedSheet != null && (shownScreen == "add" || shownScreen.startsWith("edit/"))) content.post {
            if (!isFinishing) if (rememberedSheet == "muscle") showMuscles() else showSources(rememberedSheet == "video")
        }
        model.error?.let { message ->
            model.error = null
            AlertDialog.Builder(this).setTitle("Chưa hoàn tất").setMessage(message).setPositiveButton("Đóng", null).show()
        }
    }
    private fun renderList() {
        inflate(R.layout.screen_exercise_list_xml, content)
        text(R.id.ex_count).text = "Danh mục của bạn · ${model.exercises.size} bài tập"
        click(R.id.ex_add) { capturePosition(); model.navigate("add") }
        val list = findViewById<RecyclerView>(R.id.ex_list)
        val manager = LinearLayoutManager(this)
        list.layoutManager = manager
        list.clipToPadding = true; list.clipChildren = true
        list.adapter = ExerciseXmlAdapter(lifecycleScope) { exercise -> capturePosition(); model.navigate("detail/${exercise.id}") }.also {
            it.submitList(model.exercises) { manager.scrollToPositionWithOffset(model.listPosition, model.listOffset) }
        }
        list.visibility = if (model.exercises.isEmpty()) View.GONE else View.VISIBLE
        findViewById<View>(R.id.ex_empty).visibility = if (model.exercises.isEmpty()) View.VISIBLE else View.GONE
        inflate(R.layout.exercise_nav_xml, footer)
        XmlNavigation.bind(this, "EXERCISES", model.dark) { tab -> if (!model.busy) close(tab) }
    }
    private fun renderForm() {
        inflate(R.layout.screen_exercise_form_xml, content)
        inflate(R.layout.exercise_footer_xml, footer)
        val timeBased = model.original?.trackingType == "TIME"
        text(R.id.ex_hint).text = when {
            model.attempted && FitnessRules.exerciseError(model.draft()) != null -> "Chưa lưu. Vui lòng kiểm tra thông tin."
            model.original != null -> "* Thông tin bắt buộc"
            else -> "* Tên 1–100 ký tự · Hiệp 1–100 · ${if (timeBased) "Giây 1–86400" else "Lần 1–1000"}"
        }
        if (model.original != null) text(R.id.ex_hint).textSize = 12f
        listOf(R.id.ex_name_input to "name", R.id.ex_sets_input to "sets", R.id.ex_reps_input to "reps").forEach { (id, key) ->
            findViewById<EditText>(id).apply {
                setText(model.form.getString(key, ""))
                if (key != "name") filters = arrayOf(InputFilter.LengthFilter(if (key == "sets") 3 else 5))
                doAfterTextChanged { model.form.putString(key, it.toString()) }
            }
        }
        if (timeBased) text(R.id.ex_reps_input_label).text = "Số giây mỗi hiệp *"
        updateMuscle()
        click(R.id.ex_muscle) { hideKeyboard(); capturePosition(); showMuscles() }
        wireMedia(false); wireMedia(true)
        text(R.id.ex_save).text = if (model.original == null) "Lưu bài tập" else "Lưu thay đổi"
        click(R.id.ex_save) { hideKeyboard(); capturePosition(); model.save() }
        showFieldErrors()
        findViewById<ScrollView>(R.id.ex_scroll).post { findViewById<ScrollView?>(R.id.ex_scroll)?.scrollTo(0, model.formScroll) }
        content.isFocusableInTouchMode = true; content.requestFocus()
    }
    private fun showFieldErrors() {
        val e = model.draft()
        val errors = mapOf(
            R.id.ex_name_input_error to when { e.name.isBlank() -> "Vui lòng nhập tên bài tập."; e.name.length > 100 -> "Tên bài tập tối đa 100 ký tự."; else -> null },
            R.id.ex_sets_input_error to if (e.defaultSets !in 1..100) "Số hiệp phải từ 1 đến 100." else null,
            R.id.ex_reps_input_error to if (e.trackingType == "TIME") { if (e.defaultDurationSeconds !in 1..86400) "Số giây phải từ 1 đến 86400." else null }
                else if (e.defaultReps !in 1..1000) "Số lần phải từ 1 đến 1000." else null,
            R.id.ex_muscle_error to if (e.muscleGroup.isBlank()) "Vui lòng chọn nhóm cơ." else null)
        errors.forEach { (id, message) -> text(id).apply { text = message; visibility = if (model.attempted && message != null) View.VISIBLE else View.GONE } }
    }
    private fun updateMuscle() {
        val muscle = model.form.getString("muscle", "")
        text(R.id.ex_muscle_value).apply { text = if (muscle.isBlank()) "Chọn nhóm cơ" else muscleLabel(muscle); setTextColor(color(if (muscle.isBlank()) R.attr.exMuted else R.attr.exText)) }
    }
    private fun showMuscles() {
        model.sheet = "muscle"
        val anchor = findViewById<View>(R.id.ex_muscle)
        anchor.setBackgroundResource(R.drawable.ex_focus)
        val view = layoutInflater.inflate(R.layout.exercise_muscle_popup_xml, null)
        val rows = view.findViewById<LinearLayout>(R.id.ex_options)
        val value = model.form.getString("muscle", "")
        listOf("Ngực", "Lưng", "Chân", "Tay", "Bụng", "Vai", "Toàn thân").forEach { group ->
            val option = layoutInflater.inflate(R.layout.exercise_muscle_option_xml, rows, false) as TextView
            option.text = group
            if (muscleLabel(value) == group || value.isBlank() && group == "Ngực") option.setBackgroundColor(color(R.attr.exPale))
            option.setOnClickListener { model.form.putString("muscle", group); popup?.dismiss(); updateMuscle(); showFieldErrors() }
            rows.addView(option)
        }
        popup = PopupWindow(view, anchor.width, dp(308), true).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)); elevation = dp(6).toFloat(); isOutsideTouchable = true
            setOnDismissListener { anchor.setBackgroundResource(R.drawable.ex_input); if (!changingView) model.sheet = null }
            showAsDropDown(anchor, 0, dp(4))
        }
    }
    private fun wireMedia(video: Boolean) {
        val add = if (video) R.id.ex_video_add else R.id.ex_image_add
        val selected = if (video) R.id.ex_video_selected else R.id.ex_image_selected
        val preview = if (video) R.id.ex_video_preview else R.id.ex_image_preview
        val change = if (video) R.id.ex_video_change else R.id.ex_image_change
        val remove = if (video) R.id.ex_video_remove else R.id.ex_image_remove
        val path = model.form.getString(if (video) "video" else "image")
        findViewById<View>(add).visibility = if (path.isNullOrBlank()) View.VISIBLE else View.GONE
        findViewById<View>(selected).visibility = if (path.isNullOrBlank()) View.GONE else View.VISIBLE
        text(preview).apply {
            text = if (!MediaStorage.exists(path)) "Tệp không có trên thiết bị" else if (video) File(path!!).name else "Ảnh đã chọn"
            contentDescription = if (video) "Xem trước video" else "Xem trước ảnh"
        }
        click(add) { hideKeyboard(); capturePosition(); showSources(video) }
        click(change) { hideKeyboard(); capturePosition(); showSources(video) }
        click(remove) { model.form.remove(if (video) "video" else "image"); wireMedia(video) }
        click(preview) { hideKeyboard(); capturePosition(); model.preview(path, video) }
    }
    private fun showSources(video: Boolean) {
        model.sheet = if (video) "video" else "image"
        val view = layoutInflater.inflate(R.layout.exercise_source_sheet_xml, null)
        val dialog = Dialog(this)
        dialog.setContentView(view)
        view.findViewById<TextView>(R.id.ex_source_camera).text = if (video) "Quay video bằng camera" else "Chụp ảnh bằng camera"
        view.findViewById<TextView>(R.id.ex_source_gallery).text = if (video) "Chọn video từ thư viện" else "Chọn ảnh từ thư viện"
        view.findViewById<View>(R.id.ex_source_camera).setOnClickListener { dialog.dismiss(); openCamera(video) }
        view.findViewById<View>(R.id.ex_source_gallery).setOnClickListener {
            dialog.dismiss()
            runCatching {
                if (video) videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                else photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }.onFailure { Toast.makeText(this, "Không mở được thư viện trên thiết bị này.", Toast.LENGTH_LONG).show() }
        }
        view.findViewById<View>(R.id.ex_source_cancel).setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { if (!changingView) model.sheet = null }
        dialog.window?.apply { setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)); addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); setDimAmount(0.35f) }
        dialog.show()
        dialog.window?.apply { setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); setGravity(Gravity.BOTTOM) }
        sourceDialog = dialog
    }
    private fun openCamera(video: Boolean) {
        try {
            val file = MediaStorage.newFile(this, if (video) "mp4" else "jpg")
            model.cameraPath = file.absolutePath; model.videoCapture = video
            val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
            if (video) videoCamera.launch(uri) else photoCamera.launch(uri)
        } catch (_: Exception) {
            model.cameraPath?.let { File(it).delete(); File(it).parentFile?.delete() }; model.cameraPath = null
            Toast.makeText(this, "Không mở được camera. Bạn có thể chọn ${if (video) "video" else "ảnh"} từ thư viện.", Toast.LENGTH_LONG).show()
        }
    }
    private fun renderDetail() {
        val e = model.selected() ?: run { model.back(); return }
        inflate(R.layout.screen_exercise_detail_xml, content); inflate(R.layout.exercise_footer_xml, footer)
        val image = findViewById<ImageView>(R.id.ex_detail_image)
        image.visibility = if (MediaStorage.exists(e.instructionImage)) View.VISIBLE else View.GONE
        findViewById<View>(R.id.ex_detail_image_gap).visibility = image.visibility
        if (image.visibility == View.VISIBLE) lifecycleScope.launch { image.setImageBitmap(withContext(Dispatchers.IO) { decodeExercisePhoto(e.instructionImage) }) }
        text(R.id.ex_detail_name).apply { text = e.name; textSize = if (image.visibility == View.VISIBLE) 24f else 28f }
        text(R.id.ex_detail_muscle).text = "Nhóm cơ: ${muscleLabel(e.muscleGroup)}"
        text(R.id.ex_detail_defaults).text = "${e.defaultSets} hiệp   ×   ${if (e.trackingType == "TIME") "${e.defaultDurationSeconds} giây" else "${e.defaultReps} lần"} / hiệp"
        val hasVideo = MediaStorage.exists(e.instructionVideo)
        findViewById<View>(R.id.ex_detail_video_group).visibility = if (hasVideo) View.VISIBLE else View.GONE
        findViewById<View>(R.id.ex_detail_missing).visibility = if (image.visibility == View.GONE && !hasVideo) View.VISIBLE else View.GONE
        val videoLabel = text(R.id.ex_detail_video)
        if (hasVideo) lifecycleScope.launch {
            val duration = withContext(Dispatchers.IO) { MediaStorage.videoDuration(e.instructionVideo) }
            videoLabel.text = "▷  Phát video" + (duration?.let { " · $it" } ?: "")
        }
        text(R.id.ex_detail_description).apply { text = e.description; visibility = if (e.description.isNullOrBlank()) View.GONE else View.VISIBLE }
        click(R.id.ex_detail_image) { capturePosition(); model.preview(e.instructionImage, false) }
        click(R.id.ex_detail_video) { capturePosition(); model.preview(e.instructionVideo, true) }
        text(R.id.ex_save).text = "Sửa bài tập"
        click(R.id.ex_save) { capturePosition(); model.navigate("edit/${e.id}") }
        findViewById<View>(R.id.ex_delete).visibility = View.VISIBLE
        click(R.id.ex_delete) {
            val dialog = Dialog(this)
            val view = layoutInflater.inflate(R.layout.exercise_delete_dialog_xml, null)
            view.findViewById<TextView>(R.id.ex_dialog_title).text = "Xóa bài “${e.name}”?"
            view.findViewById<View>(R.id.ex_dialog_cancel).setOnClickListener { dialog.dismiss() }
            view.findViewById<View>(R.id.ex_dialog_confirm).setOnClickListener { dialog.dismiss(); model.delete(e) }
            dialog.setContentView(view); dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show(); dialog.window?.setLayout(minOf(resources.displayMetrics.widthPixels - dp(48), dp(420)), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        findViewById<ScrollView>(R.id.ex_scroll).post { findViewById<ScrollView?>(R.id.ex_scroll)?.scrollTo(0, model.detailScroll) }
    }
    private fun renderMedia() {
        inflate(R.layout.screen_exercise_media_xml, content)
        val video = shownScreen == "video"
        val image = findViewById<ZoomImageView>(R.id.ex_full_image)
        val videoView = findViewById<VideoView>(R.id.ex_player)
        image.visibility = if (video) View.GONE else View.VISIBLE
        videoView.visibility = if (video) View.VISIBLE else View.GONE
        text(R.id.ex_media_caption).text = if (video) "Video hướng dẫn" else "Ảnh hướng dẫn · Chụm hai ngón để phóng to"
        if (!video) {
            zoomImage = image; image.zoom = model.zoomScale; image.panX = model.zoomX; image.panY = model.zoomY
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) { decodeExercisePhoto(model.previewPath) }
                image.setImageBitmap(bitmap)
                findViewById<View?>(R.id.ex_media_error)?.visibility = if (bitmap == null) View.VISIBLE else View.GONE
            }
        } else {
            player = videoView
            fun error() { text(R.id.ex_media_error).apply { text = "Không phát được video\nTệp không tồn tại hoặc định dạng video không được hỗ trợ."; visibility = View.VISIBLE } }
            if (!MediaStorage.exists(model.previewPath)) { error(); return }
            val controls = MediaController(this); controls.setAnchorView(videoView); videoView.setMediaController(controls)
            videoView.setOnErrorListener { _, _, _ -> error(); true }
            videoView.setOnPreparedListener { videoView.seekTo(model.playerPosition); if (model.playerPlaying) videoView.start() }
            videoView.setVideoPath(model.previewPath)
        }
    }
    private fun back() {
        if (model.busy) return
        capturePosition(); hideKeyboard()
        if ((shownScreen == "add" || shownScreen.startsWith("edit/")) && model.dirty()) {
            AlertDialog.Builder(this).setTitle("Bỏ thay đổi?").setMessage("Các thay đổi chưa lưu sẽ bị bỏ.")
                .setPositiveButton("Bỏ thay đổi") { _, _ -> model.back() }.setNegativeButton("Tiếp tục nhập", null).show()
        } else model.back()
    }
    private fun close(tab: String? = null) {
        setResult(RESULT_OK, Intent().putExtra("tab", tab)); finish(); overridePendingTransition(0, 0)
    }
}
