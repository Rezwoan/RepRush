package com.reprush.app.ui.member.plan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.data.repository.ImportedDay
import com.reprush.app.databinding.ItemPlanDayBinding

class PlanDayAdapter : ListAdapter<ImportedDay, PlanDayAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemPlanDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(day: ImportedDay) {
            binding.textViewDayLabel.text = day.dayLabel
            binding.textViewExerciseCount.text = "${day.exercises.size} exercises"

            val exerciseSummary = day.exercises.joinToString("\n") { ex ->
                "• ${ex.exerciseName} — ${ex.sets}×${ex.reps}"
            }
            binding.textViewExerciseList.text = exerciseSummary

            var expanded = false
            binding.root.setOnClickListener {
                expanded = !expanded
                binding.textViewExerciseList.visibility = if (expanded) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlanDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<ImportedDay>() {
        override fun areItemsTheSame(a: ImportedDay, b: ImportedDay) = a.dayLabel == b.dayLabel
        override fun areContentsTheSame(a: ImportedDay, b: ImportedDay) = a == b
    }
}
