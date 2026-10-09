package com.example.fitnessapp.xmlui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.R
import com.example.fitnessapp.model.DraftEntry
import com.example.fitnessapp.model.FitnessRules
import com.example.fitnessapp.model.displayDate
import com.example.fitnessapp.model.durationLabel
import com.example.fitnessapp.model.muscleLabel
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Activity điều khiển toàn bộ luồng Ghi nhận buổi tập bằng XML thuần.
 * Quản lý 3 màn hình chuyển đổi theo stack:
 *  - "record": Phiếu ghi nhận (Ảnh 2 khi trống, Ảnh 5 khi có dữ liệu)
 *  - "select": Chọn bài tập theo nhóm cơ (Ảnh 3)
 *  - "result/<id>": Nhập kết quả bài tập (Ảnh 4)
 */
class RecordWorkoutXmlActivity : ComponentActivity() {
    private lateinit var model: RecordWorkoutXmlModel
    private lateinit var content: FrameLayout
    private lateinit var footer: FrameLayout
    private var busyDialog: AlertDialog? = null
    private var activeDialog: Dialog? = null

    // Quản lý thông báo banner popup khi xóa bài tập khỏi phiếu (Ảnh 1 & Ảnh 2)
    private var bannerDeleteMessage: String? = null
    private val bannerHandler = Handler(Looper.getMainLooper())
    private val hideBannerRunnable = Runnable {
        hideToast()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val dark = intent.getBooleanExtra("dark", false)
        setTheme(if (dark) R.style.Theme_ExerciseXml_Dark else R.style.Theme_ExerciseXml)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_record_workout_xml)

        content = findViewById(R.id.record_content)
        footer = findViewById(R.id.record_footer)

        val root = findViewById<View>(R.id.record_root)
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            findViewById<View>(R.id.record_bottom_inset).apply {
                layoutParams = layoutParams.apply { height = maxOf(bars.bottom, keyboard.bottom) }
            }
            insets
        }

        model = ViewModelProvider(this)[RecordWorkoutXmlModel::class.java]

        findViewById<View>(R.id.record_back).setOnClickListener { handleBack() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleBack()
        })

        lifecycleScope.launch {
            model.revision.collect { render() }
        }

        model.start(savedInstanceState?.getBundle("recordState"))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle("recordState", model.saveState())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        bannerHandler.removeCallbacks(hideBannerRunnable)
        activeDialog?.dismiss()
        activeDialog = null
        busyDialog?.dismiss()
        busyDialog = null
        super.onDestroy()
    }

    private fun handleBack() {
        val currentScreen = model.screen
        when {
            currentScreen == "record" -> {
                if (model.draft.entries.isNotEmpty()) {
                    showDiscardWorkoutDialog()
                } else {
                    finish()
                }
            }
            currentScreen.startsWith("result/") -> {
                // Người dùng muốn thoát khỏi màn hình Nhập kết quả -> Hiển thị popup Ảnh 5
                showDiscardResultDialog()
            }
            else -> {
                model.back()
            }
        }
    }

    /** Popup Bỏ cả phiếu (Ảnh 4 chuẩn Figma, đếm số bài động) */
    private fun showDiscardWorkoutDialog() {
        activeDialog?.dismiss()
        val count = model.draft.entries.size
        val dialog = Dialog(this)
        activeDialog = dialog
        val view = layoutInflater.inflate(R.layout.dialog_discard_workout_xml, null)
        view.findViewById<TextView>(R.id.dialog_title).text = "Bỏ cả phiếu?"
        view.findViewById<TextView>(R.id.dialog_message).text =
            "Phiếu chưa được lưu vào lịch sử. Nếu thoát, toàn bộ $count bài đã nhập sẽ bị bỏ."

        // Nút Bỏ phiếu (Primary tối phía trên)
        view.findViewById<View>(R.id.dialog_btn_discard).setOnClickListener {
            dialog.dismiss()
            activeDialog = null
            model.clearDraft()
            finish()
        }

        // Nút Tiếp tục nhập (Secondary sáng phía dưới)
        view.findViewById<View>(R.id.dialog_btn_continue).setOnClickListener {
            dialog.dismiss()
            activeDialog = null
        }

        dialog.setOnDismissListener { if (activeDialog == dialog) activeDialog = null }
        dialog.setContentView(view)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.45f)
        }
        dialog.show()
        dialog.window?.setLayout(
            minOf(resources.displayMetrics.widthPixels - dp(48), dp(400)),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /** Popup Bỏ thay đổi khi thoát khỏi màn hình B03 Nhập kết quả (Ảnh 5 chuẩn Figma) */
    private fun showDiscardResultDialog() {
        activeDialog?.dismiss()
        val dialog = Dialog(this)
        activeDialog = dialog
        val view = layoutInflater.inflate(R.layout.dialog_discard_result_xml, null)
        view.findViewById<TextView>(R.id.dialog_title).text = "Bỏ thay đổi?"
        view.findViewById<TextView>(R.id.dialog_message).text =
            "Kết quả bài này chưa được thêm vào phiếu. Nếu thoát, những gì bạn vừa nhập sẽ bị bỏ."

        // Nút Bỏ thay đổi (Primary tối phía trên)
        view.findViewById<View>(R.id.dialog_btn_discard).setOnClickListener {
            dialog.dismiss()
            activeDialog = null
            model.back()
        }

        // Nút Tiếp tục nhập (Secondary sáng phía dưới)
        view.findViewById<View>(R.id.dialog_btn_continue).setOnClickListener {
            dialog.dismiss()
            activeDialog = null
        }

        dialog.setOnDismissListener { if (activeDialog == dialog) activeDialog = null }
        dialog.setContentView(view)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.45f)
        }
        dialog.show()
        dialog.window?.setLayout(
            minOf(resources.displayMetrics.widthPixels - dp(48), dp(400)),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /** Popup xác nhận bỏ từng bài tập khỏi phiếu (Đồng bộ thiết kế chuẩn) */
    private fun showRemoveEntryDialog(entry: DraftEntry) {
        activeDialog?.dismiss()
        val dialog = Dialog(this)
        activeDialog = dialog
        val view = layoutInflater.inflate(R.layout.dialog_discard_entry_xml, null)
        view.findViewById<TextView>(R.id.dialog_title).text = "Bỏ bài khỏi phiếu?"
        view.findViewById<TextView>(R.id.dialog_message).text =
            "Bài tập “${entry.name}” sẽ bị bỏ khỏi phiếu tập. Bài tập vẫn được giữ trong danh mục."

        // Nút Bỏ bài
        view.findViewById<View>(R.id.dialog_btn_discard).setOnClickListener {
            dialog.dismiss()
            activeDialog = null
            val removedName = entry.name

            // 1. Xóa bài khỏi phiếu
            model.removeEntry(entry.exerciseId)

            // 2. Đếm số bài còn lại chính xác
            val remainingCount = model.draft.entries.size
            val msg = "Đã bỏ “$removedName” khỏi phiếu · Còn $remainingCount bài"

            // 3. Hiển thị popup nhỏ phía dưới xuất hiện từ dưới lên trên (Ảnh 1 & Ảnh 2)
            showDeleteToast(msg)
        }

        // Nút Giữ lại
        view.findViewById<View>(R.id.dialog_btn_continue).setOnClickListener {
            dialog.dismiss()
            activeDialog = null
        }

        dialog.setOnDismissListener { if (activeDialog == dialog) activeDialog = null }
        dialog.setContentView(view)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.45f)
        }
        dialog.show()
        dialog.window?.setLayout(
            minOf(resources.displayMetrics.widthPixels - dp(48), dp(400)),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /** Hiển thị popup nho nhỏ phía dưới trượt từ dưới lên trên (Ảnh 1 & Ảnh 2) */
    private fun showDeleteToast(message: String) {
        val toastView = findViewById<LinearLayout>(R.id.record_floating_toast) ?: return
        val textView = findViewById<TextView>(R.id.record_floating_toast_text) ?: return

        textView.text = message
        bannerHandler.removeCallbacks(hideBannerRunnable)

        // Căn lề dưới: Đặt lơ lửng ngay phía trên nút "Lưu buổi tập" (cách nút Lưu khoảng 14dp); nếu hết bài thì cách 24dp
        val bottomMarginDp = if (model.draft.entries.isNotEmpty()) 88 else 24
        val params = toastView.layoutParams as? FrameLayout.LayoutParams
        if (params != null) {
            params.bottomMargin = dp(bottomMarginDp)
            toastView.layoutParams = params
        }

        // Huỷ animation cũ và chuẩn bị xuất phát điểm từ dưới màn hình
        toastView.animate().cancel()
        toastView.visibility = View.VISIBLE
        toastView.alpha = 0f
        toastView.translationY = dp(70).toFloat()

        // Hiệu ứng trượt từ dưới lên trên (Slide Up) mượt mà
        toastView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(360)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Bấm vào popup thì ẩn ngay
        toastView.setOnClickListener {
            bannerHandler.removeCallbacks(hideBannerRunnable)
            hideToast()
        }

        // Tự động trượt xuống và biến mất sau 3.5 giây
        bannerHandler.postDelayed(hideBannerRunnable, 3500)
    }

    private fun hideToast() {
        val toastView = findViewById<LinearLayout?>(R.id.record_floating_toast) ?: return
        toastView.animate().cancel()
        toastView.animate()
            .alpha(0f)
            .translationY(dp(50).toFloat())
            .setDuration(280)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                toastView.visibility = View.GONE
            }
            .start()
    }

    private fun render() {
        if (!::model.isInitialized || isFinishing) return

        if (model.busy) {
            if (busyDialog == null) {
                busyDialog = AlertDialog.Builder(this)
                    .setMessage("Đang xử lý…")
                    .setCancelable(false)
                    .create().also { it.show() }
            }
            return
        }
        busyDialog?.dismiss()
        busyDialog = null

        if (model.finished) {
            setResult(RESULT_OK)
            finish()
            return
        }

        content.removeAllViews()
        footer.visibility = View.GONE

        val titleView = findViewById<TextView>(R.id.record_title)
        val currentScreen = model.screen

        if (currentScreen != "record") {
            findViewById<View?>(R.id.record_floating_toast)?.visibility = View.GONE
            bannerHandler.removeCallbacks(hideBannerRunnable)
        }

        when {
            currentScreen == "record" -> {
                titleView.text = "Ghi nhận buổi tập"
                renderRecordScreen()
            }
            currentScreen == "select" -> {
                titleView.text = "Chọn bài tập"
                renderSelectScreen()
            }
            currentScreen.startsWith("result/") -> {
                titleView.text = "Nhập kết quả"
                val exerciseId = currentScreen.substringAfter('/').toLongOrNull() ?: 0L
                renderResultScreen(exerciseId)
            }
        }
    }

    /** 1. Màn hình Ghi nhận buổi tập (Ảnh 2 khi trống, Ảnh 5 khi có dữ liệu) */
    private fun renderRecordScreen() {
        val view = layoutInflater.inflate(R.layout.screen_record_workout_xml, content, false)
        content.addView(view)

        // Card Ngày tập
        val dateText = view.findViewById<TextView>(R.id.record_date_text)
        dateText.text = displayDate(model.draft.date)
        view.findViewById<View>(R.id.record_date_card).setOnClickListener {
            val current = try { LocalDate.parse(model.draft.date) } catch (_: Exception) { LocalDate.now() }
            DatePickerDialog(this, { _, year, month, day ->
                val chosen = LocalDate.of(year, month + 1, day)
                model.updateDate(chosen.toString())
            }, current.year, current.monthValue - 1, current.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis()
                show()
            }
        }

        val emptyCard = view.findViewById<View>(R.id.record_empty_card)
        val entriesRecycler = view.findViewById<RecyclerView>(R.id.record_entries_list)
        val btnAdd = view.findViewById<TextView>(R.id.record_btn_add_exercise)
        val summaryText = view.findViewById<TextView>(R.id.record_summary_text)
        val summarySubtext = view.findViewById<View>(R.id.record_summary_subtext)
        val btnSaveScroll = view.findViewById<View>(R.id.record_btn_save_workout)
        val btnSaveFooter = findViewById<View>(R.id.record_btn_save_footer)

        val entries = model.draft.entries
        if (entries.isEmpty()) {
            // Ảnh 2: Chưa có bài tập
            emptyCard.visibility = View.VISIBLE
            entriesRecycler.visibility = View.GONE
            btnAdd.text = "＋ Thêm bài tập"
            summaryText.text = "0 bài · 0 phút"
            summarySubtext.visibility = View.GONE
            btnSaveScroll.visibility = View.GONE
            footer.visibility = View.GONE
        } else {
            // Ảnh 5: Đã có bài tập trong phiếu
            emptyCard.visibility = View.GONE
            entriesRecycler.visibility = View.VISIBLE
            btnAdd.text = "＋ Chọn bài tập"
            val totalSeconds = entries.sumOf { it.durationSeconds }
            summaryText.text = "${entries.size} bài · ${durationLabel(totalSeconds)}"
            summarySubtext.visibility = View.VISIBLE
            btnSaveScroll.visibility = View.GONE // Nút cố định ở footer sẽ đảm bảo luôn nhìn thấy
            footer.visibility = View.VISIBLE

            entriesRecycler.layoutManager = LinearLayoutManager(this)
            val adapter = RecordEntryXmlAdapter(
                onEdit = { entry ->
                    bannerDeleteMessage = null
                    bannerHandler.removeCallbacks(hideBannerRunnable)
                    model.navigate("result/${entry.exerciseId}")
                },
                onRemove = { entry -> showRemoveEntryDialog(entry) }
            )
            entriesRecycler.adapter = adapter
            adapter.submitList(entries)
        }

        // Ẩn banner tĩnh trong scrollview vì đã dùng floating toast lơ lửng ở Activity root
        view.findViewById<View?>(R.id.record_delete_banner)?.visibility = View.GONE

        // Bấm nút thêm/chọn bài tập -> chuyển sang Ảnh 3
        btnAdd.setOnClickListener {
            bannerDeleteMessage = null
            bannerHandler.removeCallbacks(hideBannerRunnable)
            model.navigate("select")
        }

        // Bấm nút Lưu buổi tập (cố định ở chân màn hình)
        val onSaveAction = View.OnClickListener {
            hideToast()
            model.saveWorkout(
                onSuccess = {
                    Toast.makeText(this, "Đã lưu buổi tập thành công!", Toast.LENGTH_SHORT).show()
                },
                onError = { message ->
                    AlertDialog.Builder(this)
                        .setTitle("Lỗi lưu buổi tập")
                        .setMessage(message)
                        .setPositiveButton("Đóng", null)
                        .show()
                }
            )
        }
        btnSaveScroll.setOnClickListener(onSaveAction)
        btnSaveFooter?.setOnClickListener(onSaveAction)
    }

    /** 2. Màn hình Chọn bài tập theo nhóm cơ (Ảnh 3) */
    private fun renderSelectScreen() {
        val view = layoutInflater.inflate(R.layout.screen_select_exercise_xml, content, false)
        content.addView(view)

        val chipsContainer = view.findViewById<LinearLayout>(R.id.select_chips_container)
        val recycler = view.findViewById<RecyclerView>(R.id.select_exercise_recycler)
        val emptyView = view.findViewById<View>(R.id.select_exercise_empty)

        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = SelectExerciseXmlAdapter(lifecycleScope) { exercise ->
            // Chọn bài tập -> chuyển sang Ảnh 4
            model.navigate("result/${exercise.id}")
        }
        recycler.adapter = adapter

        // Hàm áp dụng bộ lọc nhóm cơ
        fun applyFilter(selectedCat: String) {
            model.selectedCategory = selectedCat

            // Cập nhật giao diện các chip (chip được chọn -> nền tối chữ trắng, chip khác -> nền sáng)
            for (i in 0 until chipsContainer.childCount) {
                val chipView = chipsContainer.getChildAt(i) as? TextView ?: continue
                val isSelected = chipView.text.toString().trim().equals(selectedCat.trim(), ignoreCase = true)
                chipView.setBackgroundResource(if (isSelected) R.drawable.ex_chip_selected else R.drawable.ex_chip_unselected)
                chipView.setTextColor(color(if (isSelected) R.attr.exOnPrimary else R.attr.exText))
                chipView.setTypeface(chipView.typeface, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }

            // Lọc danh sách bài tập chính xác theo nhóm cơ
            val filtered = if (selectedCat == "Tất cả") {
                model.exercises
            } else {
                model.exercises.filter {
                    muscleLabel(it.muscleGroup).trim().equals(selectedCat.trim(), ignoreCase = true)
                }
            }

            // Cập nhật hiển thị danh sách
            if (filtered.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recycler.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                recycler.visibility = View.VISIBLE
            }
            adapter.submitList(filtered)
        }

        // Lấy danh sách các nhóm cơ có sẵn trong DB
        val categories = listOf("Tất cả") + model.exercises.map { muscleLabel(it.muscleGroup).trim() }.distinct()

        // Xây dựng các chip lọc
        chipsContainer.removeAllViews()
        categories.forEach { cat ->
            val chip = TextView(this).apply {
                text = cat
                textSize = 13f
                gravity = android.view.Gravity.CENTER
                setPadding(dp(18), 0, dp(18), 0)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(36)
                ).apply { setMargins(0, 0, dp(8), 0) }

                setOnClickListener {
                    applyFilter(cat)
                }
            }
            chipsContainer.addView(chip)
        }

        // Áp dụng filter ban đầu
        applyFilter(model.selectedCategory)
    }

    /** 3. Màn hình Nhập kết quả bài tập (Ảnh 4) */
    private fun renderResultScreen(exerciseId: Long) {
        val exercise = model.findExercise(exerciseId)
        if (exercise == null) {
            model.back()
            return
        }

        val existingEntry = model.findDraftEntry(exerciseId)
        val view = layoutInflater.inflate(R.layout.screen_enter_result_xml, content, false)
        content.addView(view)

        view.findViewById<TextView>(R.id.result_exercise_name).text = exercise.name
        view.findViewById<TextView>(R.id.result_exercise_muscle).text = muscleLabel(exercise.muscleGroup)

        // Nút xem hướng dẫn
        view.findViewById<View>(R.id.result_btn_guide).setOnClickListener {
            val desc = exercise.description.orEmpty().ifBlank { "Bài tập ${exercise.name} rèn luyện nhóm cơ ${muscleLabel(exercise.muscleGroup)}." }
            AlertDialog.Builder(this)
                .setTitle(exercise.name)
                .setMessage(desc)
                .setPositiveButton("Đóng", null)
                .show()
        }

        val setsCountInput = view.findViewById<EditText>(R.id.result_sets_count)
        val repsLabel = view.findViewById<TextView>(R.id.result_label_reps_title)
        repsLabel.text = if (exercise.trackingType == "TIME") "Số giây từng hiệp" else "Số lần từng hiệp"

        val setsContainer = view.findViewById<LinearLayout>(R.id.result_sets_container)
        val inputMinutes = view.findViewById<EditText>(R.id.result_input_minutes)
        val inputSeconds = view.findViewById<EditText>(R.id.result_input_seconds)
        val inputNote = view.findViewById<EditText>(R.id.result_input_note)
        val errorText = view.findViewById<TextView>(R.id.result_error_text)

        // Khởi tạo giá trị ban đầu
        val defaultSets = existingEntry?.values?.size ?: exercise.defaultSets
        setsCountInput.setText(defaultSets.toString())

        var currentValues = existingEntry?.values ?: List(defaultSets) {
            if (exercise.trackingType == "TIME") exercise.defaultDurationSeconds ?: 30 else exercise.defaultReps
        }

        val totalDuration = existingEntry?.durationSeconds ?: 0
        inputMinutes.setText((totalDuration / 60).toString())
        inputSeconds.setText((totalDuration % 60).toString().padStart(2, '0'))
        inputNote.setText(existingEntry?.note ?: "")

        // Hàm vẽ lại danh sách ô nhập từng hiệp
        fun rebuildSetRows(count: Int) {
            setsContainer.removeAllViews()
            val safeCount = count.coerceIn(1, 100)
            val updatedValues = ArrayList<Int>()
            for (i in 0 until safeCount) {
                val row = layoutInflater.inflate(R.layout.item_set_input_xml, setsContainer, false)
                row.findViewById<TextView>(R.id.set_label).text = "Hiệp ${i + 1}"
                row.findViewById<TextView>(R.id.set_unit).text = if (exercise.trackingType == "TIME") "giây" else "lần"
                val editValue = row.findViewById<EditText>(R.id.set_input_value)

                val prevVal = currentValues.getOrNull(i) ?: if (exercise.trackingType == "TIME") exercise.defaultDurationSeconds ?: 30 else exercise.defaultReps
                editValue.setText(prevVal.toString())
                updatedValues.add(prevVal)

                val setIndex = i
                editValue.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val num = s?.toString()?.toIntOrNull() ?: 0
                        if (setIndex < currentValues.size) {
                            val list = currentValues.toMutableList()
                            list[setIndex] = num
                            currentValues = list
                        }
                    }
                })
                setsContainer.addView(row)
            }
            currentValues = updatedValues
        }

        rebuildSetRows(defaultSets)

        // Lắng nghe khi người dùng đổi số hiệp
        setsCountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val count = s?.toString()?.toIntOrNull() ?: 0
                if (count in 1..100) {
                    rebuildSetRows(count)
                }
            }
        })

        // Bấm nút "Thêm vào phiếu" -> chuyển sang Ảnh 5
        view.findViewById<View>(R.id.result_btn_submit).setOnClickListener {
            val count = setsCountInput.text.toString().toIntOrNull() ?: 0
            val minutes = inputMinutes.text.toString().toIntOrNull() ?: 0
            val seconds = inputSeconds.text.toString().toIntOrNull() ?: 0
            val note = inputNote.text.toString().trim()

            // Đọc lại giá trị từng hiệp trực tiếp từ các ô EditText
            val finalValues = ArrayList<Int>()
            for (i in 0 until setsContainer.childCount) {
                val row = setsContainer.getChildAt(i)
                val rowInput = row.findViewById<EditText>(R.id.set_input_value)
                val v = rowInput.text.toString().toIntOrNull() ?: 0
                finalValues.add(v)
            }

            val totalSec = minutes * 60 + seconds
            val entry = DraftEntry(
                exerciseId = exercise.id,
                name = exercise.name,
                muscle = exercise.muscleGroup,
                values = finalValues,
                durationSeconds = totalSec,
                note = note,
                trackingType = exercise.trackingType
            )

            val error = when {
                count !in 1..100 -> "Số hiệp phải từ 1 đến 100."
                seconds !in 0..59 -> "Số giây phải từ 0 đến 59."
                minutes !in 0..1440 -> "Số phút phải từ 0 đến 1440."
                else -> FitnessRules.entryError(entry)
            }

            if (error != null) {
                errorText.visibility = View.VISIBLE
                errorText.text = error
            } else {
                errorText.visibility = View.GONE
                model.addOrUpdateEntry(entry)
            }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun color(attr: Int): Int = TypedValue().also { theme.resolveAttribute(attr, it, true) }.data
}
