package com.example.fitnessapp.xmlui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.R
import com.example.fitnessapp.model.DraftEntry
import com.example.fitnessapp.model.Exercise
import com.example.fitnessapp.model.durationLabel
import com.example.fitnessapp.model.muscleLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Adapter cho danh sách chọn bài tập (Ảnh 3). */
internal class SelectExerciseXmlAdapter(
    private val scope: LifecycleCoroutineScope,
    private val onClick: (Exercise) -> Unit
) : ListAdapter<Exercise, SelectExerciseXmlAdapter.Holder>(object : DiffUtil.ItemCallback<Exercise>() {
    override fun areItemsTheSame(old: Exercise, new: Exercise) = old.id == new.id
    override fun areContentsTheSame(old: Exercise, new: Exercise) = old == new
}) {
    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = getItem(position).id
    class Holder(view: View) : RecyclerView.ViewHolder(view) { var photoJob: Job? = null }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_select_exercise_xml, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val e = getItem(position)
        holder.itemView.findViewById<TextView>(R.id.select_item_name).text = e.name
        val unit = if (e.trackingType == "TIME") "${e.defaultDurationSeconds ?: 0} giây" else "${e.defaultReps} lần"
        holder.itemView.findViewById<TextView>(R.id.select_item_details).text =
            "${muscleLabel(e.muscleGroup)} · ${e.defaultSets} hiệp × $unit"
        holder.itemView.findViewById<View>(R.id.select_item_card).setOnClickListener { onClick(e) }

        val image = holder.itemView.findViewById<ImageView>(R.id.select_item_thumb)
        val placeholder = holder.itemView.findViewById<View>(R.id.select_item_thumb_placeholder)
        holder.photoJob?.cancel()
        image.setImageDrawable(null)
        placeholder.visibility = View.VISIBLE
        holder.photoJob = scope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeExercisePhoto(e.instructionImage) }
            image.setImageBitmap(bitmap)
            placeholder.visibility = if (bitmap == null) View.VISIBLE else View.GONE
        }
    }

    override fun onViewRecycled(holder: Holder) {
        holder.photoJob?.cancel()
        super.onViewRecycled(holder)
    }
}

/** Adapter cho danh sách bài tập đã thêm vào phiếu (Ảnh 5). */
internal class RecordEntryXmlAdapter(
    private val onEdit: (DraftEntry) -> Unit,
    private val onRemove: (DraftEntry) -> Unit
) : ListAdapter<DraftEntry, RecordEntryXmlAdapter.Holder>(object : DiffUtil.ItemCallback<DraftEntry>() {
    override fun areItemsTheSame(old: DraftEntry, new: DraftEntry) = old.exerciseId == new.exerciseId
    override fun areContentsTheSame(old: DraftEntry, new: DraftEntry) = old == new
}) {
    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = getItem(position).exerciseId

    class Holder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_workout_entry_xml, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = getItem(position)
        holder.itemView.findViewById<TextView>(R.id.entry_name).text = entry.name
        holder.itemView.findViewById<TextView>(R.id.entry_subtitle).text =
            "${muscleLabel(entry.muscle)} · ${entry.values.size} hiệp"

        val unit = if (entry.trackingType == "TIME") "giây" else "lần"
        val valuesStr = entry.values.joinToString(" / ")
        holder.itemView.findViewById<TextView>(R.id.entry_details).text =
            "$valuesStr $unit · ${durationLabel(entry.durationSeconds)}"

        val noteView = holder.itemView.findViewById<TextView>(R.id.entry_note)
        if (entry.note.isNotBlank()) {
            noteView.visibility = View.VISIBLE
            noteView.text = "Ghi chú: ${entry.note}"
        } else {
            noteView.visibility = View.GONE
        }

        holder.itemView.findViewById<View>(R.id.entry_btn_edit).setOnClickListener { onEdit(entry) }
        holder.itemView.findViewById<View>(R.id.entry_btn_remove).setOnClickListener { onRemove(entry) }
    }
}
