package com.reprush.app.ui.admin.payments

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.data.repository.PaymentRecord
import com.reprush.app.databinding.ItemPaymentBinding

class PaymentAdapter(
    private val onReceiptClick: (PaymentRecord) -> Unit,
    private val onLongClick: ((PaymentRecord) -> Unit)? = null
) : ListAdapter<PaymentRecord, PaymentAdapter.PaymentViewHolder>(PaymentDiffCallback()) {

    inner class PaymentViewHolder(
        private val binding: ItemPaymentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(payment: PaymentRecord) {
            binding.textViewPaymentMemberName.text = payment.memberName.ifEmpty { payment.memberId }
            binding.textViewPaymentAmount.text = "৳${"%.2f".format(payment.amount)}"
            binding.textViewPaymentMethod.text = payment.paymentMethod.replace("_", " ")
                .replaceFirstChar { it.uppercase() }
            binding.textViewPaymentDate.text = payment.paymentDate

            if (payment.isVoided) {
                binding.textViewVoidedLabel.visibility = View.VISIBLE
                binding.root.alpha = 0.6f
            } else {
                binding.textViewVoidedLabel.visibility = View.GONE
                binding.root.alpha = 1.0f
            }

            binding.imageButtonReceipt.setOnClickListener { onReceiptClick(payment) }

            if (onLongClick != null) {
                binding.root.setOnLongClickListener {
                    onLongClick.invoke(payment)
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PaymentDiffCallback : DiffUtil.ItemCallback<PaymentRecord>() {
    override fun areItemsTheSame(oldItem: PaymentRecord, newItem: PaymentRecord): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: PaymentRecord, newItem: PaymentRecord): Boolean =
        oldItem == newItem
}
