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
    private var isEditing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePackageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarCreatePackage.setNavigationOnClickListener {
            viewModel.setEditTarget(null)
            findNavController().navigateUp()
        }

        val editPkg = viewModel.editTarget.value
        if (editPkg != null) {
            isEditing = true
            binding.toolbarCreatePackage.title = "Edit Package"
            binding.buttonSavePackage.text = "Save Changes"
            binding.editTextPackageName.setText(editPkg.name)
            binding.editTextPackagePrice.setText("%.2f".format(editPkg.price))
            binding.editTextPackageDuration.setText(editPkg.durationDays.toString())
            binding.editTextPackageDescription.setText(editPkg.description ?: "")
        }

        binding.buttonSavePackage.setOnClickListener { attemptSave() }
        observeViewModel()
    }

    private fun attemptSave() {
        val name = binding.editTextPackageName.text?.toString().orEmpty().trim()
        val priceStr = binding.editTextPackagePrice.text?.toString().orEmpty().trim()
        val durationStr = binding.editTextPackageDuration.text?.toString().orEmpty().trim()
        val description = binding.editTextPackageDescription.text?.toString().orEmpty().trim().ifBlank { null }

        var valid = true
        binding.textInputLayoutPackageName.error = null
        binding.textInputLayoutPackagePrice.error = null
        binding.textInputLayoutPackageDuration.error = null

        if (name.isEmpty()) {
            binding.textInputLayoutPackageName.error = "Package name is required"
            valid = false
        }
        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0) {
            binding.textInputLayoutPackagePrice.error = "Enter a valid price greater than 0"
            valid = false
        }
        val duration = durationStr.toIntOrNull()
        if (duration == null || duration <= 0) {
            binding.textInputLayoutPackageDuration.error = "Enter a valid duration greater than 0"
            valid = false
        }
        if (!valid) return

        binding.progressBarSavePackage.visibility = View.VISIBLE
        binding.buttonSavePackage.isEnabled = false
        isSubmitting = true

        if (isEditing) {
            val updated = viewModel.editTarget.value!!.copy(
                name = name, price = price!!, durationDays = duration!!, description = description
            )
            viewModel.updatePackage(updated)
        } else {
            viewModel.createPackage(name, price!!, duration!!, description)
        }
    }

    private fun observeViewModel() {
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (!isSubmitting) return@observe
            isSubmitting = false
            when (result) {
                is Result.Success<*> -> {
                    viewModel.setEditTarget(null)
                    viewModel.clearOperationResult()
                    val msg = if (isEditing) "Package updated" else "Package created"
                    Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        msg, Snackbar.LENGTH_LONG
                    ).show()
                    findNavController().navigateUp()
                }
                is Result.Error -> {
                    binding.progressBarSavePackage.visibility = View.GONE
                    binding.buttonSavePackage.isEnabled = true
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
