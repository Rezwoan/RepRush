package com.reprush.app.ui.member.leaderboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.reprush.app.R
import com.reprush.app.databinding.ItemLeaderboardBinding

class LeaderboardAdapter : ListAdapter<RankedEntry, LeaderboardAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemLeaderboardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RankedEntry) {
            binding.textViewRank.text = "#${item.rank}"
            binding.textViewDisplayName.text = item.entry.displayName.ifBlank { "Member" }
            binding.textViewPoints.text = "${item.entry.points} pts"
            binding.textViewPrCount.text = "${item.entry.totalPRs} PRs"

            if (!item.entry.photoUrl.isNullOrBlank()) {
                Glide.with(binding.imageViewAvatar)
                    .load(item.entry.photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(binding.imageViewAvatar)
            } else {
                binding.imageViewAvatar.setImageResource(R.drawable.ic_person)
            }

            binding.root.setBackgroundColor(
                if (item.isCurrentUser)
                    binding.root.context.getColor(R.color.primary_container)
                else
                    android.graphics.Color.TRANSPARENT
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLeaderboardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RankedEntry>() {
            override fun areItemsTheSame(a: RankedEntry, b: RankedEntry) =
                a.entry.uid == b.entry.uid
            override fun areContentsTheSame(a: RankedEntry, b: RankedEntry) = a == b
        }
    }
}
