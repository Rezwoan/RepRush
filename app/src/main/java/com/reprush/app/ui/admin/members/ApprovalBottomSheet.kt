package com.reprush.app.ui.admin.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.data.model.MembershipPackage
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.LayoutApprovalSheetBinding
import com.reprush.app.ui.admin.packages.PackageViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ApprovalBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutApprovalSheetBinding? = null
    private val binding get() = _binding!!

    private val pendingViewModel: PendingMembersViewModel by activityViewModels()
    private val packageViewModel: PackageViewModel by activityViewModels()

    private var selectedPackage: MembershipPackage? = null
    private var selectedStartDate: LocalDate = LocalDate.now()
    private val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private var hasSubmitted = false

    companion object {
        private const val ARG_UID = "uid"
        private const val ARG_NAME = "name"

        fun newInstance(uid: String, displayName: String): ApprovalBottomSheet {
            return ApprovalBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_UID, uid)
                    putString(ARG_NAME, displayName)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = LayoutApprovalSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = arguments?.getString(ARG_UID) ?: return
        val memberName = arguments?.getString(ARG_NAME) ?: ""

        binding.textViewApprovalMemberName.text = "Approving: $memberName"
        binding.editTextStartDate.setText(displayFormat.format(Date()))
        binding.buttonConfirmApproval.isEnabled = false

        // Observer must be registered before the load call so it catches the emission
        packageViewModel.activePackages.observe(viewLifecycleOwner) { packages ->
            android.util.Log.d("ApprovalSheet", "Packages loaded: ${packages?.size ?: 0}")
            if (packages.isNullOrEmpty()) {
                binding.layoutPackageSelector.error = "No packages available. Create a package first."
                binding.buttonConfirmApproval.isEnabled = false
                return@observe
            }
            binding.layoutPackageSelector.error = null

            val packageNames = packages.map { "${it.name} — ${it.durationDays} days" }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                packageNames
            )
            binding.editTextSelectPackage.setAdapter(adapter)
            binding.editTextSelectPackage.setOnItemClickListener { _, _, position, _ ->
                selectedPackage = packages[position]
                binding.layoutPackageSelector.error = null
                updateCoveragePreview()
                binding.buttonConfirmApproval.isEnabled = true
            }
        }

        // Load packages fresh every time the sheet opens (after observer is set up)
        packageViewModel.loadActivePackages()

        // Date picker
        binding.editTextStartDate.setOnClickListener { showDatePicker() }
        binding.layoutStartDate.setEndIconOnClickListener { showDatePicker() }

        // Confirm button
        binding.buttonConfirmApproval.setOnClickListener {
            val pkg = selectedPackage
            if (pkg == null) {
                binding.layoutPackageSelector.error = "Please select a membership package"
                return@setOnClickListener
            }
            binding.layoutPackageSelector.error = null
            binding.progressBarApproval.visibility = View.VISIBLE
            binding.buttonConfirmApproval.isEnabled = false
            hasSubmitted = true
            pendingViewModel.clearOperationResult()
            pendingViewModel.approveMember(uid, pkg.id, selectedStartDate.toString(), pkg.durationDays)
        }

        // Only react to results after user has submitted
        pendingViewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (!hasSubmitted) return@observe
            when (result) {
                is Result.Success -> {
                    pendingViewModel.clearOperationResult()
                    dismiss()
                }
                is Result.Error -> {
                    binding.progressBarApproval.visibility = View.GONE
                    binding.buttonConfirmApproval.isEnabled = true
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                    pendingViewModel.clearOperationResult()
                }
            }
        }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select start date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            selectedStartDate = LocalDate.ofEpochDay(millis / 86_400_000)
            binding.editTextStartDate.setText(displayFormat.format(Date(millis)))
            updateCoveragePreview()
        }
        picker.show(parentFragmentManager, "date_picker")
    }

    private fun updateCoveragePreview() {
        val pkg = selectedPackage ?: return
        val endDate = selectedStartDate.plusDays(pkg.durationDays.toLong())
        binding.textViewCoveragePreview.text =
            "Coverage: $selectedStartDate → $endDate (${pkg.durationDays} days)"
        binding.textViewCoveragePreview.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}