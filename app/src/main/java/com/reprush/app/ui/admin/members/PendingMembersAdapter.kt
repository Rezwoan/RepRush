package com.reprush.app.ui.admin.members

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.data.repository.PendingMember
import com.reprush.app.databinding.ItemPendingMemberBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PendingMembersAdapter(
    private val onApproveClick: (PendingMember) -> Unit,
    private val onRejectClick: (PendingMember) -> Unit
) : ListAdapter<PendingMember, PendingMembersAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPendingMemberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemPendingMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: PendingMember) {
            binding.textViewPendingName.text = member.displayName
            binding.textViewPendingEmail.text = member.email

            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val dateText = if (member.createdAt > 0) {
                "Applied: ${dateFormat.format(Date(member.createdAt))}"
            } else {
                "Applied: Unknown"
            }
            binding.textViewPendingDate.text = dateText

            binding.buttonApprove.setOnClickListener { onApproveClick(member) }
            binding.buttonReject.setOnClickListener { onRejectClick(member) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PendingMember>() {
        override fun areItemsTheSame(oldItem: PendingMember, newItem: PendingMember): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: PendingMember, newItem: PendingMember): Boolean {
            return oldItem == newItem
        }
    }
}