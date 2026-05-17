package com.reprush.app.ui.member.progress

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.databinding.ItemWorkoutHistoryBinding
import java.util.Locale

class WorkoutHistoryAdapter(
    private val onClick: (String) -> Unit
) : ListAdapter<WorkoutHistoryItem, WorkoutHistoryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkoutHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemWorkoutHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WorkoutHistoryItem) {
            binding.textHistoryDate.text = item.dateFormatted
            binding.textHistoryDayLabel.text = item.dayLabel
            binding.textHistoryDuration.text = item.durationText
            binding.textHistoryVolume.text = String.format(Locale.getDefault(), "%.0f kg", item.volumeKg)
            binding.textHistoryPoints.text = "${item.totalPoints} pts"
            binding.root.setOnClickListener { onClick(item.sessionId) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WorkoutHistoryItem>() {
            override fun areItemsTheSame(a: WorkoutHistoryItem, b: WorkoutHistoryItem) =
                a.sessionId == b.sessionId
            override fun areContentsTheSame(a: WorkoutHistoryItem, b: WorkoutHistoryItem) = a == b
        }
    }
}
