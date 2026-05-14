package com.reprush.app.ui.admin.members

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.data.repository.AttendanceRecord
import com.reprush.app.databinding.ItemMemberAttendanceBinding

class MemberAttendanceAdapter : ListAdapter<AttendanceRecord, MemberAttendanceAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<AttendanceRecord>() {
        override fun areItemsTheSame(a: AttendanceRecord, b: AttendanceRecord) = a.id == b.id
        override fun areContentsTheSame(a: AttendanceRecord, b: AttendanceRecord) = a == b
    }

    inner class ViewHolder(private val binding: ItemMemberAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttendanceRecord) {
            binding.textViewAttendanceDate.text = item.date
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMemberAttendanceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
