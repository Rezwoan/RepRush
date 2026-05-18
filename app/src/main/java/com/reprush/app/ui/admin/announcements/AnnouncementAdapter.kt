package com.reprush.app.ui.admin.announcements

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.data.repository.Announcement
import com.reprush.app.databinding.ItemAnnouncementBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnnouncementAdapter(
    private val onLongClick: (Announcement) -> Unit
) : ListAdapter<Announcement, AnnouncementAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Announcement>() {
        override fun areItemsTheSame(a: Announcement, b: Announcement) = a.id == b.id
        override fun areContentsTheSame(a: Announcement, b: Announcement) = a == b
    }

    inner class ViewHolder(private val binding: ItemAnnouncementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Announcement) {
            binding.textViewAnnouncementTitle.text = item.title
            binding.textViewAnnouncementBody.text = item.body
            binding.textViewAnnouncementDate.text = formatDate(item.createdAt)
            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }

        private fun formatDate(millis: Long): String {
            val sdf = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())
            return sdf.format(Date(millis))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnnouncementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
