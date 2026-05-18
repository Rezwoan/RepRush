package com.reprush.app.ui.member.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.reprush.app.databinding.FragmentProfileEditBinding
import com.reprush.app.data.repository.Result
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileEditFragment : Fragment() {

    private var _binding: FragmentProfileEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileEditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply { duration = 300L }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply { duration = 300L }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                prefillFitnessLevel(user.fitnessLevel)
                prefillGoal(user.primaryGoal)
                prefillEquipment(user.availableEquipment)
                binding.editTextInjuries.setText(user.injuries ?: "")
            }
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    viewModel.clearSaveResult()
                    findNavController().popBackStack()
                }
                is Result.Error -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                    viewModel.clearSaveResult()
                }
                null -> {}
            }
        }

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }
        binding.buttonSaveProfile.setOnClickListener { saveProfile() }

        viewModel.loadProfile()
    }

    private fun prefillFitnessLevel(level: String?) {
        when (level?.lowercase()) {
            "beginner" -> binding.chipBeginner.isChecked = true
            "intermediate" -> binding.chipIntermediate.isChecked = true
            "advanced" -> binding.chipAdvanced.isChecked = true
        }
    }

    private fun prefillGoal(goal: String?) {
        val normalized = goal?.lowercase() ?: return
        when {
            "muscle" in normalized -> binding.chipMuscleGain.isChecked = true
            "fat" in normalized || "loss" in normalized -> binding.chipFatLoss.isChecked = true
            "strength" in normalized -> binding.chipStrength.isChecked = true
            "endurance" in normalized -> binding.chipEndurance.isChecked = true
        }
    }

    private fun prefillEquipment(equipment: String?) {
        val lower = equipment?.lowercase() ?: return
        if ("full" in lower || "gym" in lower) binding.chipFullGym.isChecked = true
        if ("dumbbell" in lower) binding.chipDumbbells.isChecked = true
        if ("barbell" in lower) binding.chipBarbell.isChecked = true
        if ("bodyweight" in lower || "body weight" in lower) binding.chipBodyweightOnly.isChecked = true
        if ("band" in lower) binding.chipResistanceBands.isChecked = true
    }

    private fun saveProfile() {
        val fitnessLevel = when (binding.chipGroupFitnessLevel.checkedChipId) {
            binding.chipBeginner.id -> "Beginner"
            binding.chipIntermediate.id -> "Intermediate"
            binding.chipAdvanced.id -> "Advanced"
            else -> ""
        }
        val goal = when (binding.chipGroupPrimaryGoal.checkedChipId) {
            binding.chipMuscleGain.id -> "Muscle Gain"
            binding.chipFatLoss.id -> "Fat Loss"
            binding.chipStrength.id -> "Strength"
            binding.chipEndurance.id -> "Endurance"
            else -> ""
        }

        val selectedEquipment = mutableListOf<String>()
        if (binding.chipFullGym.isChecked) selectedEquipment.add("Full Gym")
        if (binding.chipDumbbells.isChecked) selectedEquipment.add("Dumbbells")
        if (binding.chipBarbell.isChecked) selectedEquipment.add("Barbell")
        if (binding.chipBodyweightOnly.isChecked) selectedEquipment.add("Bodyweight")
        if (binding.chipResistanceBands.isChecked) selectedEquipment.add("Resistance Bands")
        val equipment = selectedEquipment.joinToString(", ")

        val injuries = binding.editTextInjuries.text?.toString() ?: ""

        viewModel.saveProfile(fitnessLevel, goal, equipment, injuries)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
