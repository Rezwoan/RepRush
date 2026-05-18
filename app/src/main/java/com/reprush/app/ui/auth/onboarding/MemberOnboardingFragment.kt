package com.reprush.app.ui.auth.onboarding

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.reprush.app.R
import com.reprush.app.databinding.FragmentMemberOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MemberOnboardingFragment : Fragment() {

    private var _binding: FragmentMemberOnboardingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MemberOnboardingViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMemberOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updateButton()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.editTextHeight.addTextChangedListener(watcher)
        binding.editTextWeight.addTextChangedListener(watcher)

        binding.buttonSaveOnboarding.setOnClickListener { saveAndContinue() }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.buttonSaveOnboarding.isEnabled = !loading
            binding.buttonSaveOnboarding.alpha = if (!loading) 1f else 0.5f
        }

        viewModel.saved.observe(viewLifecycleOwner) { saved ->
            if (saved && isAdded) {
                findNavController().navigate(R.id.action_memberOnboardingFragment_to_homeFragment)
            }
        }
    }

    private fun updateButton() {
        val heightOk = binding.editTextHeight.text?.toString()?.trim()?.toFloatOrNull() != null
        val weightOk = binding.editTextWeight.text?.toString()?.trim()?.toFloatOrNull() != null
        val loading = viewModel.isLoading.value ?: false
        val enabled = heightOk && weightOk && !loading
        binding.buttonSaveOnboarding.isEnabled = enabled
        binding.buttonSaveOnboarding.alpha = if (enabled) 1f else 0.5f
    }

    private fun chipText(group: com.google.android.material.chip.ChipGroup): String {
        val id = group.checkedChipId
        if (id == View.NO_ID) return ""
        return group.findViewById<Chip>(id)?.text?.toString() ?: ""
    }

    private fun saveAndContinue() {
        val heightCm = binding.editTextHeight.text?.toString()?.trim()?.toFloatOrNull() ?: return
        val weightKg = binding.editTextWeight.text?.toString()?.trim()?.toFloatOrNull() ?: return
        val experience = chipText(binding.chipGroupExperience)
        val lastExercised = chipText(binding.chipGroupLastExercised)
        val squatKg = binding.editTextSquat.text?.toString()?.trim()?.toFloatOrNull()
        val benchKg = binding.editTextBench.text?.toString()?.trim()?.toFloatOrNull()
        val deadliftKg = binding.editTextDeadlift.text?.toString()?.trim()?.toFloatOrNull()

        viewModel.saveOnboarding(heightCm, weightKg, experience, lastExercised, squatKg, benchKg, deadliftKg)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
