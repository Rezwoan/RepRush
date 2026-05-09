package com.reprush.app.ui.admin.packages

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.data.model.MembershipPackage
import com.reprush.app.databinding.ItemPackageBinding

class PackageAdapter(
    private val onItemClick: (MembershipPackage) -> Unit
) : ListAdapter<MembershipPackage, PackageAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPackageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPackageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pkg: MembershipPackage) {
            binding.textViewPackageName.text = pkg.name
            binding.textViewPackagePrice.text = "৳ %.2f".format(pkg.price)
            binding.textViewPackageDuration.text = "${pkg.durationDays} days"

            if (pkg.description.isNullOrBlank()) {
                binding.textViewPackageDescription.visibility = View.GONE
            } else {
                binding.textViewPackageDescription.visibility = View.VISIBLE
                binding.textViewPackageDescription.text = pkg.description
            }

            if (pkg.isActive) {
                binding.chipPackageStatus.text = "Active"
                binding.chipPackageStatus.chipBackgroundColor =
                    ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                binding.chipPackageStatus.setTextColor(Color.WHITE)
            } else {
                binding.chipPackageStatus.text = "Inactive"
                binding.chipPackageStatus.chipBackgroundColor = null
                val attrs = intArrayOf(com.google.android.material.R.attr.colorOnSurfaceVariant)
                val ta = binding.root.context.obtainStyledAttributes(attrs)
                binding.chipPackageStatus.setTextColor(ta.getColor(0, Color.GRAY))
                ta.recycle()
            }

            binding.root.setOnClickListener { onItemClick(pkg) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MembershipPackage>() {
        override fun areItemsTheSame(oldItem: MembershipPackage, newItem: MembershipPackage) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MembershipPackage, newItem: MembershipPackage) =
            oldItem == newItem
    }
}
