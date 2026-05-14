package com.reprush.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.R
import com.reprush.app.data.repository.PaymentRecord
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentPaymentLogBinding
import com.reprush.app.ui.admin.payments.PaymentAdapter
import com.reprush.app.ui.admin.payments.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

@AndroidEntryPoint
class PaymentLogFragment : Fragment() {

    private var _binding: FragmentPaymentLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PaymentViewModel by activityViewModels()

    private lateinit var adapter: PaymentAdapter
    private var filterStartDate: String? = null
    private var filterEndDate: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PaymentAdapter(
            onReceiptClick = { payment ->
                val bundle = Bundle().apply { putString("paymentId", payment.id) }
                findNavController().navigate(R.id.action_paymentLogFragment_to_receiptViewFragment, bundle)
            },
            onLongClick = { payment ->
                if (!payment.isVoided) showVoidDialog(payment)
            }
        )

        binding.recyclerViewPayments.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPayments.adapter = adapter

        binding.editTextStartDate.setOnClickListener { showDatePicker(true) }
        binding.layoutStartDate.setEndIconOnClickListener { showDatePicker(true) }
        binding.editTextEndDate.setOnClickListener { showDatePicker(false) }
        binding.layoutEndDate.setEndIconOnClickListener { showDatePicker(false) }

        binding.fabRecordPayment.setOnClickListener {
            findNavController().navigate(R.id.action_paymentLogFragment_to_recordPaymentFragment)
        }

        viewModel.paymentLog.observe(viewLifecycleOwner) { payments ->
            adapter.submitList(payments)
            binding.layoutEmptyPayments.visibility = if (payments.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewPayments.visibility = if (payments.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarPaymentLog.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.voidResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            viewModel.clearVoidResult()
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Payment voided", Snackbar.LENGTH_SHORT).show()
                    loadPayments()
                }
                is Result.Error -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        loadPayments()
    }

    private fun loadPayments() {
        viewModel.loadPaymentLog(filterStartDate, filterEndDate)
    }

    private fun showDatePicker(isStart: Boolean) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStart) "Filter from date" else "Filter to date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            val date = LocalDate.ofEpochDay(millis / 86_400_000)
            if (isStart) {
                filterStartDate = date.toString()
                binding.editTextStartDate.setText(date.toString())
            } else {
                filterEndDate = date.toString()
                binding.editTextEndDate.setText(date.toString())
            }
            loadPayments()
        }
        picker.show(parentFragmentManager, if (isStart) "start_date" else "end_date")
    }

    private fun showVoidDialog(payment: PaymentRecord) {
        val input = EditText(requireContext()).apply {
            hint = "Reason for voiding"
            setPadding(48, 32, 48, 16)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Void Payment")
            .setMessage("Void payment of ৳${"%.2f".format(payment.amount)} for ${payment.memberName}?")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Void") { _, _ ->
                val reason = input.text?.toString()?.trim() ?: "No reason provided"
                viewModel.voidPayment(payment.id, reason)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
