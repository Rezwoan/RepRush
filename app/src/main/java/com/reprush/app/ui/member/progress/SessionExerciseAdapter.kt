package com.reprush.app.ui.member.progress

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.databinding.ItemSessionDetailExerciseBinding
import java.util.Locale

data class ExerciseSetGroup(
    val exerciseName: String,
    val sets: List<SetDisplay>
)

data class SetDisplay(
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isWarmup: Boolean,
    val isPR: Boolean
)

class SessionExerciseAdapter :
    ListAdapter<ExerciseSetGroup, SessionExerciseAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSessionDetailExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSessionDetailExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: ExerciseSetGroup) {
            binding.textDetailExerciseName.text = group.exerciseName
            binding.containerDetailSets.removeAllViews()

            val inflater = LayoutInflater.from(binding.root.context)
            for (set in group.sets) {
                val row = inflater.inflate(android.R.layout.simple_list_item_1, binding.containerDetailSets, false) as TextView
                val badge = when {
                    set.isPR -> " 🏅 PR"
                    set.isWarmup -> " (warmup)"
                    else -> ""
                }
                row.text = "Set ${set.setNumber}: ${String.format(Locale.getDefault(), "%.1f", set.weight)} kg × ${set.reps}$badge"
                row.textSize = 13f
                row.setTextColor(Color.parseColor("#E2E2E6"))
                binding.containerDetailSets.addView(row)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ExerciseSetGroup>() {
            override fun areItemsTheSame(a: ExerciseSetGroup, b: ExerciseSetGroup) =
                a.exerciseName == b.exerciseName
            override fun areContentsTheSame(a: ExerciseSetGroup, b: ExerciseSetGroup) = a == b
        }
    }
}
