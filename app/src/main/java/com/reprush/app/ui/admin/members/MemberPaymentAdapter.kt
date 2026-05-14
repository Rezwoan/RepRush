package com.reprush.app.ui.admin.members

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.data.repository.PaymentRecord
import com.reprush.app.databinding.ItemMemberPaymentBinding

class MemberPaymentAdapter : ListAdapter<PaymentRecord, MemberPaymentAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<PaymentRecord>() {
        override fun areItemsTheSame(a: PaymentRecord, b: PaymentRecord) = a.id == b.id
        override fun areContentsTheSame(a: PaymentRecord, b: PaymentRecord) = a == b
    }

    inner class ViewHolder(private val binding: ItemMemberPaymentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PaymentRecord) {
            binding.textViewPaymentAmount.text = "৳${"%.2f".format(item.amount)}"
            binding.textViewPaymentDate.text = item.paymentDate
            binding.textViewPaymentPeriod.text = "${item.periodStart} — ${item.periodEnd}"
            binding.textViewPaymentMethod.text = item.paymentMethod
            if (item.isVoided) {
                binding.textViewPaymentAmount.alpha = 0.5f
                binding.textViewPaymentVoided.visibility = android.view.View.VISIBLE
            } else {
                binding.textViewPaymentAmount.alpha = 1.0f
                binding.textViewPaymentVoided.visibility = android.view.View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMemberPaymentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
