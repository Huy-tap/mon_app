package com.example.fitnessapp.xmlui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.R
import com.example.fitnessapp.model.Workout
import com.example.fitnessapp.model.displayDate

sealed class HistoryItem {
    data class DateHeader(val date: String) : HistoryItem()
    data class WorkoutCard(val workout: Workout) : HistoryItem()
}

/**
 * Adapter hiển thị danh sách lịch sử tập luyện theo ngày (Hình 2).
 */
class WorkoutHistoryAdapter(
    private val onWorkoutClick: (Long) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_WORKOUT_CARD = 1
    }

    private val items = ArrayList<HistoryItem>()

    fun submitItems(newItems: List<HistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HistoryItem.DateHeader -> TYPE_DATE_HEADER
            is HistoryItem.WorkoutCard -> TYPE_WORKOUT_CARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val view = inflater.inflate(R.layout.item_history_date_header_xml, parent, false)
                DateHeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_history_workout_card_xml, parent, false)
                WorkoutCardViewHolder(view, onWorkoutClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is HistoryItem.WorkoutCard -> (holder as WorkoutCardViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textHeader: TextView = view.findViewById(R.id.history_item_date_header)
        fun bind(item: HistoryItem.DateHeader) {
            textHeader.text = displayDate(item.date)
        }
    }

    class WorkoutCardViewHolder(
        view: View,
        private val onWorkoutClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val textSummary: TextView = view.findViewById(R.id.history_item_summary)
        private val exercisesContainer: LinearLayout = view.findViewById(R.id.history_item_exercises_container)

        fun bind(item: HistoryItem.WorkoutCard) {
            val workout = item.workout
            textSummary.text = "${workout.items.size} bài · ${workout.durationMinutes} phút"

            exercisesContainer.removeAllViews()
            val context = itemView.context
            workout.items.forEach { ex ->
                val line = TextView(context).apply {
                    text = "${ex.exerciseName} · ${ex.sets.size} hiệp"
                    textSize = 13f
                    setPadding(0, 2, 0, 2)
                    setTextColor(android.util.TypedValue().also {
                        context.theme.resolveAttribute(R.attr.exMuted, it, true)
                    }.data)
                }
                exercisesContainer.addView(line)
            }

            itemView.setOnClickListener {
                onWorkoutClick(workout.id)
            }
        }
    }
}
