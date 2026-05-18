package com.reprush.app.ui.member.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.R

class AchievementAdapter(
    private val onItemClick: (AchievementDisplayItem) -> Unit
) : ListAdapter<AchievementDisplayItem, AchievementAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.textView_badgeIcon)
        val lockOverlay: TextView = view.findViewById(R.id.textView_lockOverlay)
        val name: TextView = view.findViewById(R.id.textView_badgeName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.icon.text = item.badge.icon
        holder.name.text = item.badge.name

        if (item.isUnlocked) {
            holder.icon.alpha = 1.0f
            holder.lockOverlay.visibility = View.GONE
        } else {
            holder.icon.alpha = 0.3f
            holder.lockOverlay.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AchievementDisplayItem>() {
        override fun areItemsTheSame(a: AchievementDisplayItem, b: AchievementDisplayItem) =
            a.badge.id == b.badge.id
        override fun areContentsTheSame(a: AchievementDisplayItem, b: AchievementDisplayItem) =
            a == b
    }
}
