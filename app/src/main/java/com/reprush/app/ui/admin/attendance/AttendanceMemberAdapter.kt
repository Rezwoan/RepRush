package com.reprush.app.ui.admin.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.data.repository.Member
import com.reprush.app.databinding.ItemAttendanceMemberBinding

class AttendanceMemberAdapter(
    private val onMarkPresent: (Member) -> Unit
) : ListAdapter<AttendanceMemberItem, AttendanceMemberAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemAttendanceMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttendanceMemberItem) {
            binding.textViewAttendanceMemberName.text = item.member.displayName
            binding.textViewAttendanceEmail.text = item.member.email

            if (item.checkedIn) {
                binding.switchMarkPresent.isChecked = true
                binding.switchMarkPresent.isEnabled = false
                binding.textViewAttendanceStatus.text = "Already checked in today"
            } else {
                binding.switchMarkPresent.isChecked = false
                binding.switchMarkPresent.isEnabled = true
                binding.textViewAttendanceStatus.text = "Not checked in"
            }

            binding.switchMarkPresent.setOnCheckedChangeListener(null)
            binding.switchMarkPresent.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !item.checkedIn) {
                    onMarkPresent(item.member)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceMemberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

data class AttendanceMemberItem(
    val member: Member,
    val checkedIn: Boolean
)

class DiffCallback : DiffUtil.ItemCallback<AttendanceMemberItem>() {
    override fun areItemsTheSame(old: AttendanceMemberItem, new: AttendanceMemberItem) =
        old.member.uid == new.member.uid

    override fun areContentsTheSame(old: AttendanceMemberItem, new: AttendanceMemberItem) =
        old == new
}
