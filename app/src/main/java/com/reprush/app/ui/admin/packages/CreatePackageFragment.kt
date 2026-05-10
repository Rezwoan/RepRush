package com.reprush.app.ui.admin.packages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentCreatePackageBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreatePackageFragment : Fragment() {

    private var _binding: FragmentCreatePackageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PackageViewModel by activityViewModels()
    private var isSubmitting = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePackageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbarCreatePackage.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.buttonSavePackage.setOnClickListener { attemptSave() }
        observeViewModel()
    }

    private fun attemptSave() {
        val name = binding.editTextPackageName.text?.toString().orEmpty().trim()
        val priceStr = binding.editTextPackagePrice.text?.toString().orEmpty().trim()
        val durationStr = binding.editTextPackageDuration.text?.toString().orEmpty().trim()
        val description = binding.editTextPackageDescription.text?.toString().orEmpty().trim()
            .ifBlank { null }

        var valid = true

        binding.textInputLayoutPackageName.error = null
        binding.textInputLayoutPackagePrice.error = null
        binding.textInputLayoutPackageDuration.error = null

        if (name.isEmpty()) {
            binding.textInputLayoutPackageName.error = "Package name is required"
            valid = false
        }

        val price = priceStr.toDoubleOrNull()
        if (priceStr.isEmpty() || price == null || price <= 0) {
            binding.textInputLayoutPackagePrice.error = "Enter a valid price greater than 0"
            valid = false
        }

        val duration = durationStr.toIntOrNull()
        if (durationStr.isEmpty() || duration == null || duration <= 0) {
            binding.textInputLayoutPackageDuration.error = "Enter a valid duration greater than 0"
            valid = false
        }

        if (!valid) return

        binding.progressBarSavePackage.visibility = View.VISIBLE
        binding.buttonSavePackage.isEnabled = false
        isSubmitting = true
        viewModel.createPackage(name, price!!, duration!!, description)
    }

    private fun observeViewModel() {
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (!isSubmitting) return@observe
            when (result) {
                is Result.Success<*> -> {
                    viewModel.clearOperationResult()
                    Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        "Package created successfully",
                        Snackbar.LENGTH_LONG
                    ).show()
                    findNavController().navigateUp()
                }
                is Result.Error -> {
                    binding.progressBarSavePackage.visibility = View.GONE
                    binding.buttonSavePackage.isEnabled = true
                    isSubmitting = false
                    viewModel.clearOperationResult()
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}