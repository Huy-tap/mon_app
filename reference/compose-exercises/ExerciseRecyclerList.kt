package com.example.fitnessapp.ui

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.model.Exercise

@Composable
internal fun ExerciseRecyclerList(exercises: List<Exercise>, onExercise: (Long) -> Unit, modifier: Modifier = Modifier) {
    val composition = rememberCompositionContext()
    val adapter = remember(composition) { ExerciseAdapter(composition) }
    AndroidView(modifier = modifier.clipToBounds(), factory = { context ->
        RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
            clipToPadding = true
            contentDescription = "Danh sách bài tập"
        }
    }, update = {
        adapter.onExercise = onExercise
        adapter.submitList(exercises.toList())
    })
}

private class ExerciseAdapter(private val composition: CompositionContext) : ListAdapter<Exercise, ExerciseAdapter.Holder>(object : DiffUtil.ItemCallback<Exercise>() {
    override fun areItemsTheSame(old: Exercise, new: Exercise) = old.id == new.id
    override fun areContentsTheSame(old: Exercise, new: Exercise) = old == new
}) {
    var onExercise: (Long) -> Unit = {}
    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = getItem(position).id
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = ComposeView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setParentCompositionContext(composition)
        }
        return Holder(view).also { holder ->
            view.setContent {
                holder.exercise?.let { exercise ->
                    Box(Modifier.fillMaxWidth().padding(bottom = 16.dp)) { ExerciseRow(exercise) { onExercise(exercise.id) } }
                }
            }
        }
    }
    override fun onBindViewHolder(holder: Holder, position: Int) { holder.exercise = getItem(position) }
    override fun onViewRecycled(holder: Holder) { holder.exercise = null }
    class Holder(view: ComposeView) : RecyclerView.ViewHolder(view) { var exercise by mutableStateOf<Exercise?>(null) }
}
