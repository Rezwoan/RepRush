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
            val ctx = binding.root.context

            // Medal emoji for top 3, number for the rest
            binding.textViewRank.text = when (item.rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "#${item.rank}"
            }

            // Rank text color for top 3
            binding.textViewRank.setTextColor(
                when (item.rank) {
                    1 -> ctx.getColor(R.color.rank_gold)
                    2 -> ctx.getColor(R.color.rank_silver)
                    3 -> ctx.getColor(R.color.rank_bronze)
                    else -> ctx.getColor(R.color.on_surface_variant)
                }
            )

            // Card background: medal colors for top 3, current-user highlight, or default
            val bgColor = when {
                item.rank == 1 -> ctx.getColor(R.color.leaderboard_gold)
                item.rank == 2 -> ctx.getColor(R.color.leaderboard_silver)
                item.rank == 3 -> ctx.getColor(R.color.leaderboard_bronze)
                item.isCurrentUser -> ctx.getColor(R.color.primary_container)
                else -> ctx.getColor(R.color.surface_variant)
            }
            binding.root.setCardBackgroundColor(bgColor)

            // Stroke to highlight current user even when they're in top 3
            binding.root.strokeWidth = if (item.isCurrentUser) 2 else 0
            binding.root.strokeColor = ctx.getColor(R.color.primary)

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
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLeaderboardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RankedEntry>() {
            override fun areItemsTheSame(a: RankedEntry, b: RankedEntry) = a.entry.uid == b.entry.uid
            override fun areContentsTheSame(a: RankedEntry, b: RankedEntry) = a == b
        }
    }
}
