package com.example.fitnessapp.xmlui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.R
import com.example.fitnessapp.model.Exercise
import com.example.fitnessapp.model.muscleLabel
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun decodeExercisePhoto(path: String?) = runCatching {
    if (path == null) null else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = (maxOf(bounds.outWidth, bounds.outHeight) / 1400).coerceAtLeast(1) })
    }
}.getOrNull()

internal class ExerciseXmlAdapter(private val scope: LifecycleCoroutineScope, private val onClick: (Exercise) -> Unit) :
    ListAdapter<Exercise, ExerciseXmlAdapter.Holder>(object : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(old: Exercise, new: Exercise) = old.id == new.id
        override fun areContentsTheSame(old: Exercise, new: Exercise) = old == new
    }) {
    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = getItem(position).id
    class Holder(view: View) : RecyclerView.ViewHolder(view) { var photoJob: Job? = null }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_exercise_xml, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val e = getItem(position)
        holder.itemView.findViewById<TextView>(R.id.ex_name).text = e.name
        holder.itemView.findViewById<TextView>(R.id.ex_summary).text = "${muscleLabel(e.muscleGroup)} · ${e.defaultSets} hiệp × ${if (e.trackingType == "TIME") "${e.defaultDurationSeconds ?: 0} giây" else "${e.defaultReps} lần"}"
        holder.itemView.findViewById<View>(R.id.ex_card).setOnClickListener { onClick(e) }
        val image = holder.itemView.findViewById<ImageView>(R.id.ex_thumb)
        val placeholder = holder.itemView.findViewById<View>(R.id.ex_placeholder)
        holder.photoJob?.cancel(); image.setImageDrawable(null); placeholder.visibility = View.VISIBLE
        holder.photoJob = scope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeExercisePhoto(e.instructionImage) }
            image.setImageBitmap(bitmap); placeholder.visibility = if (bitmap == null) View.VISIBLE else View.GONE
        }
    }
    override fun onViewRecycled(holder: Holder) { holder.photoJob?.cancel(); super.onViewRecycled(holder) }
}
