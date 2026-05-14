package com.reprush.app.ui.admin.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.data.model.MembershipPackage
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentManualRegisterBinding
import com.reprush.app.ui.admin.packages.PackageViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

@AndroidEntryPoint
class ManualRegisterFragment : Fragment() {

    private var _binding: FragmentManualRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PendingMembersViewModel by activityViewModels()
    private val packageViewModel: PackageViewModel by activityViewModels()
    private var isSubmitting = false
    private var selectedPackage: MembershipPackage? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarManualRegister.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.switchRegisterActive.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutRegisterPackage.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                packageViewModel.loadActivePackages()
            } else {
                selectedPackage = null
            }
        }

        packageViewModel.activePackages.observe(viewLifecycleOwner) { packages ->
            if (packages.isNullOrEmpty()) {
                binding.layoutRegisterPackage.error = "No packages available. Create a package first."
                return@observe
            }
            binding.layoutRegisterPackage.error = null
            val packageNames = packages.map { "${it.name} — ${it.durationDays} days" }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                packageNames
            )
            binding.editTextSelectPackage.setAdapter(adapter)
            binding.editTextSelectPackage.setOnItemClickListener { _, _, position, _ ->
                selectedPackage = packages[position]
                binding.layoutRegisterPackage.error = null
            }
        }

        binding.buttonRegisterMember.setOnClickListener {
            val name = binding.editTextRegisterName.text?.toString()?.trim() ?: ""
            val email = binding.editTextRegisterEmail.text?.toString()?.trim() ?: ""
            val phone = binding.editTextRegisterPhone.text?.toString()?.trim() ?: ""
            val registerAsActive = binding.switchRegisterActive.isChecked

            var isValid = true

            if (name.isEmpty()) {
                binding.layoutRegisterName.error = "Name is required"
                isValid = false
            } else {
                binding.layoutRegisterName.error = null
            }

            if (email.isEmpty()) {
                binding.layoutRegisterEmail.error = "Email is required"
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.layoutRegisterEmail.error = "Enter a valid email address"
                isValid = false
            } else {
                binding.layoutRegisterEmail.error = null
            }

            if (registerAsActive && selectedPackage == null) {
                binding.layoutRegisterPackage.error = "Please select a package"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            binding.progressBarRegister.visibility = View.VISIBLE
            binding.buttonRegisterMember.isEnabled = false
            isSubmitting = true

            if (registerAsActive) {
                val pkg = selectedPackage!!
                val startDate = LocalDate.now().toString()
                val endDate = LocalDate.now().plusDays(pkg.durationDays.toLong()).toString()
                viewModel.registerMemberAsActive(name, email, phone, pkg.id, startDate, endDate)
            } else {
                viewModel.registerMemberManually(name, email, phone)
            }
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (!isSubmitting) return@observe
            when (result) {
                is Result.Success -> {
                    viewModel.clearOperationResult()
                    Snackbar.make(
                        binding.root,
                        "Member registered successfully",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    findNavController().navigateUp()
                }
                is Result.Error -> {
                    binding.progressBarRegister.visibility = View.GONE
                    binding.buttonRegisterMember.isEnabled = true
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (!isSubmitting) return@observe
            binding.progressBarRegister.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        viewModel.clearOperationResult()
        super.onDestroyView()
        _binding = null
    }
}
