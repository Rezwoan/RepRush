package com.reprush.app.ui.admin.payments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.R
import com.reprush.app.data.model.MembershipPackage
import com.reprush.app.data.repository.Member
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentRecordPaymentBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class RecordPaymentFragment : Fragment() {

    private var _binding: FragmentRecordPaymentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PaymentViewModel by activityViewModels()

    private var selectedMember: Member? = null
    private var selectedPackage: MembershipPackage? = null
    private var selectedDate: LocalDate = LocalDate.now()
    private val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private var isSubmitting = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarRecordPayment.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val prefilledUid = arguments?.getString("memberUid")
        val prefilledName = arguments?.getString("memberName")

        binding.editTextPaymentDate.setText(displayFormat.format(Date()))

        viewModel.loadMembers()
        viewModel.loadActivePackages()

        viewModel.members.observe(viewLifecycleOwner) { members ->
            val memberNames = members.map { it.displayName }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, memberNames)
            binding.editTextSelectMember.setAdapter(adapter)
            binding.editTextSelectMember.setOnItemClickListener { _, _, position, _ ->
                selectedMember = members[position]
                binding.layoutSelectMember.error = null
            }

            if (prefilledUid != null) {
                selectedMember = members.find { it.uid == prefilledUid }
                if (selectedMember != null) {
                    binding.editTextSelectMember.setText(selectedMember!!.displayName, false)
                } else if (prefilledName != null) {
                    binding.editTextSelectMember.setText(prefilledName, false)
                }
            }
        }

        viewModel.activePackages.observe(viewLifecycleOwner) { packages ->
            val packageNames = packages.map { "${it.name} — ৳${it.price} (${it.durationDays} days)" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, packageNames)
            binding.editTextSelectPackage.setAdapter(adapter)
            binding.editTextSelectPackage.setOnItemClickListener { _, _, position, _ ->
                selectedPackage = packages[position]
                binding.editTextPaymentAmount.setText(packages[position].price.toString())
                binding.layoutSelectPackage.error = null
                updateCoveragePreview()
            }
        }

        binding.editTextPaymentDate.setOnClickListener { showDatePicker() }
        binding.layoutPaymentDate.setEndIconOnClickListener { showDatePicker() }

        binding.buttonRecordPayment.setOnClickListener { attemptRecordPayment() }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (!isSubmitting) return@observe
            isSubmitting = false
            when (result) {
                is Result.Success -> {
                    viewModel.clearOperationResult()
                    val paymentId = result.data
                    val bundle = Bundle().apply { putString("paymentId", paymentId) }
                    findNavController().navigate(R.id.action_recordPaymentFragment_to_receiptViewFragment, bundle)
                }
                is Result.Error -> {
                    binding.progressBarRecordPayment.visibility = View.GONE
                    binding.buttonRecordPayment.isEnabled = true
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun attemptRecordPayment() {
        var valid = true

        if (selectedMember == null) {
            binding.layoutSelectMember.error = "Please select a member"
            valid = false
        }
        if (selectedPackage == null) {
            binding.layoutSelectPackage.error = "Please select a package"
            valid = false
        }

        val amountText = binding.editTextPaymentAmount.text?.toString()?.trim() ?: ""
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.layoutPaymentAmount.error = "Amount must be greater than 0"
            valid = false
        } else {
            binding.layoutPaymentAmount.error = null
        }

        if (!valid) return

        val member = selectedMember!!
        val pkg = selectedPackage!!
        val periodStart: String
        val periodEnd: String

        val currentEndDate = member.membershipEndDate
        if (currentEndDate != null) {
            try {
                val endDate = LocalDate.parse(currentEndDate)
                if (endDate.isAfter(LocalDate.now())) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Overlapping Period")
                        .setMessage("This member's current membership does not expire until $currentEndDate. Proceeding will extend their period. Continue?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Continue") { _, _ ->
                            val start = endDate.toString()
                            val end = endDate.plusDays(pkg.durationDays.toLong()).toString()
                            submitPayment(member, pkg, amount!!, start, end)
                        }
                        .show()
                    return
                }
            } catch (_: Exception) { }
        }

        periodStart = selectedDate.toString()
        periodEnd = selectedDate.plusDays(pkg.durationDays.toLong()).toString()
        submitPayment(member, pkg, amount!!, periodStart, periodEnd)
    }

    private fun submitPayment(member: Member, pkg: MembershipPackage, amount: Double, periodStart: String, periodEnd: String) {
        val method = when (binding.radioGroupPaymentMethod.checkedRadioButtonId) {
            R.id.radio_cash -> "cash"
            R.id.radio_bankTransfer -> "bank_transfer"
            R.id.radio_mobileBanking -> "mobile_banking"
            else -> "cash"
        }

        binding.progressBarRecordPayment.visibility = View.VISIBLE
        binding.buttonRecordPayment.isEnabled = false
        isSubmitting = true

        viewModel.recordPayment(
            memberId = member.uid,
            memberName = member.displayName,
            packageId = pkg.id,
            packageName = pkg.name,
            amount = amount,
            paymentMethod = method,
            paymentDate = selectedDate.toString(),
            periodStart = periodStart,
            periodEnd = periodEnd
        )
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select payment date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            selectedDate = LocalDate.ofEpochDay(millis / 86_400_000)
            binding.editTextPaymentDate.setText(displayFormat.format(Date(millis)))
            updateCoveragePreview()
        }
        picker.show(parentFragmentManager, "payment_date_picker")
    }

    private fun updateCoveragePreview() {
        val pkg = selectedPackage ?: return
        val endDate = selectedDate.plusDays(pkg.durationDays.toLong())
        binding.textViewCoveragePeriod.text = "Coverage: $selectedDate to $endDate (${pkg.durationDays} days)"
        binding.textViewCoveragePeriod.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
