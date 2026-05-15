package com.reprush.app.ui.member.plan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.data.local.entity.WorkoutPlanEntity
import com.reprush.app.databinding.ItemPlanBinding

class PlanListAdapter(
    private val onSetActive: (WorkoutPlanEntity) -> Unit,
    private val onDelete: (WorkoutPlanEntity) -> Unit
) : ListAdapter<WorkoutPlanEntity, PlanListAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemPlanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(plan: WorkoutPlanEntity) {
            binding.textViewPlanName.text = plan.planName
            binding.textViewPlanGoal.text = "${plan.goal} · ${plan.totalWeeks} weeks"
            binding.chipActiveBadge.visibility = if (plan.isActive == 1) View.VISIBLE else View.GONE
            binding.buttonSetActive.visibility = if (plan.isActive == 1) View.GONE else View.VISIBLE
            binding.buttonSetActive.setOnClickListener { onSetActive(plan) }
            binding.buttonDeletePlan.setOnClickListener { onDelete(plan) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<WorkoutPlanEntity>() {
        override fun areItemsTheSame(a: WorkoutPlanEntity, b: WorkoutPlanEntity) = a.id == b.id
        override fun areContentsTheSame(a: WorkoutPlanEntity, b: WorkoutPlanEntity) = a == b
    }
}
