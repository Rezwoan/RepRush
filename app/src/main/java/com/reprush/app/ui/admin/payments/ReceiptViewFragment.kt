package com.reprush.app.ui.admin.payments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.reprush.app.databinding.FragmentReceiptBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReceiptViewFragment : Fragment() {

    private var _binding: FragmentReceiptBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PaymentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarReceipt.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val paymentId = arguments?.getString("paymentId") ?: return

        binding.progressBarReceipt.visibility = View.VISIBLE
        viewModel.loadReceipt(paymentId)

        viewModel.receipt.observe(viewLifecycleOwner) { payment ->
            payment ?: return@observe
            binding.progressBarReceipt.visibility = View.GONE

            binding.textViewReceiptNumber.text = "#${payment.id}"
            binding.textViewReceiptMemberName.text = payment.memberName.ifEmpty { payment.memberId }
            binding.textViewReceiptPackageName.text = payment.packageName.ifEmpty { payment.packageId }
            binding.textViewReceiptAmount.text = "৳${"%.2f".format(payment.amount)}"
            binding.textViewReceiptMethod.text = payment.paymentMethod.replace("_", " ")
                .replaceFirstChar { it.uppercase() }
            binding.textViewReceiptDate.text = payment.paymentDate
            binding.textViewReceiptCoverage.text = "${payment.periodStart} to ${payment.periodEnd}"

            if (payment.isVoided) {
                binding.textViewReceiptVoided.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
