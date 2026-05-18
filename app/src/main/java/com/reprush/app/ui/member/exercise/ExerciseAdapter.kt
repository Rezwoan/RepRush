package com.reprush.app.ui.member.exercise

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.data.local.entity.ExerciseEntity
import com.reprush.app.databinding.ItemExerciseBinding
import com.reprush.app.utils.AssetImageLoader

class ExerciseAdapter(
    private val onItemClick: (ExerciseEntity) -> Unit
) : ListAdapter<ExerciseEntity, ExerciseAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: ExerciseEntity) {
            binding.textViewExerciseName.text = exercise.name
            binding.textViewExerciseMuscle.text = exercise.primaryMuscle
            binding.textViewExerciseEquipment.text = exercise.equipment

            AssetImageLoader.loadThumbnail(
                binding.imageViewExerciseThumbnail.context,
                exercise.thumbnailUrl,
                binding.imageViewExerciseThumbnail
            )

            binding.imageViewExerciseThumbnail.contentDescription =
                "${exercise.name} exercise thumbnail"
            binding.root.setOnClickListener { onItemClick(exercise) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<ExerciseEntity>() {
        override fun areItemsTheSame(a: ExerciseEntity, b: ExerciseEntity) = a.id == b.id
        override fun areContentsTheSame(a: ExerciseEntity, b: ExerciseEntity) = a == b
    }
}
