package com.reprush.app.ui.member.notifications

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.data.repository.NotificationItem
import com.reprush.app.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val onClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(a: NotificationItem, b: NotificationItem) = a.id == b.id
        override fun areContentsTheSame(a: NotificationItem, b: NotificationItem) = a == b
    }

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationItem) {
            binding.textViewNotificationTitle.text = item.title
            binding.textViewNotificationBody.text = item.body
            binding.textViewNotificationTime.text = formatDate(item.createdAt)

            val typeface = if (item.isRead) Typeface.NORMAL else Typeface.BOLD
            binding.textViewNotificationTitle.setTypeface(null, typeface)

            binding.root.alpha = if (item.isRead) 0.7f else 1.0f

            binding.root.setOnClickListener { onClick(item) }
        }

        private fun formatDate(millis: Long): String {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            return sdf.format(Date(millis))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
