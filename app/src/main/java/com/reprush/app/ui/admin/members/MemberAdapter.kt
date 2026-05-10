package com.reprush.app.ui.admin.members

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.data.repository.Member
import com.reprush.app.databinding.ItemMemberBinding

class MemberAdapter(
    private val onItemClick: (Member) -> Unit
) : ListAdapter<Member, MemberAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMemberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: Member) {
            binding.textViewMemberName.text = member.displayName
            binding.textViewMemberPackage.text = member.packageId ?: "No package assigned"

            // Status chip text and color
            val (statusLabel, chipColor, textColor) = when (member.membershipStatus) {
                "active"    -> Triple("Active",    "#4CAF50", "#FFFFFF")
                "expired"   -> Triple("Expired",   "#F44336", "#FFFFFF")
                "suspended" -> Triple("Suspended", "#FF9800", "#FFFFFF")
                "pending"   -> Triple("Pending",   "#2196F3", "#FFFFFF")
                "rejected"  -> Triple("Rejected",  "#757575", "#FFFFFF")
                else        -> Triple("Unknown",   "#9E9E9E", "#FFFFFF")
            }
            binding.chipMemberStatus.text = statusLabel
            binding.chipMemberStatus.setChipBackgroundColorResource(android.R.color.transparent)
            binding.chipMemberStatus.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(Color.parseColor(chipColor))
            binding.chipMemberStatus.setTextColor(Color.parseColor(textColor))

            binding.root.setOnClickListener { onItemClick(member) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Member>() {
        override fun areItemsTheSame(oldItem: Member, newItem: Member) =
            oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: Member, newItem: Member) =
            oldItem == newItem
    }
}