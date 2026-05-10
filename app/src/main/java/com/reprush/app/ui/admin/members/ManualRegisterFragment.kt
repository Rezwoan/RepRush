package com.reprush.app.ui.admin.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentManualRegisterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManualRegisterFragment : Fragment() {

    private var _binding: FragmentManualRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PendingMembersViewModel by activityViewModels()

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

        binding.buttonRegisterMember.setOnClickListener {
            val name = binding.editTextRegisterName.text?.toString()?.trim() ?: ""
            val email = binding.editTextRegisterEmail.text?.toString()?.trim() ?: ""
            val phone = binding.editTextRegisterPhone.text?.toString()?.trim() ?: ""

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

            if (!isValid) return@setOnClickListener

            binding.progressBarRegister.visibility = View.VISIBLE
            binding.buttonRegisterMember.isEnabled = false

            viewModel.registerMemberManually(name, email, phone)
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Member registered successfully", Snackbar.LENGTH_SHORT).show()
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
            binding.progressBarRegister.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}