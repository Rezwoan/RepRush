package com.reprush.app.ui.member.exercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentAddExerciseBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddCustomExerciseFragment : Fragment() {

    private var _binding: FragmentAddExerciseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExerciseDetailViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val muscles = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Abdominals", "Quadriceps", "Hamstrings", "Glutes", "Calves", "Forearms", "Other")
        val equipments = listOf("Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight", "Kettlebell", "Band", "Other")
        val categories = listOf("Strength", "Cardio", "Stretching", "Plyometrics", "Olympic Weightlifting", "Powerlifting", "Other")

        binding.editTextMuscleGroup.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, muscles))
        binding.editTextEquipment.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, equipments))
        binding.editTextCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        binding.buttonSaveExercise.setOnClickListener {
            val name = binding.editTextExerciseName.text?.toString()?.trim() ?: ""
            val muscle = binding.editTextMuscleGroup.text?.toString()?.trim() ?: ""
            val equipment = binding.editTextEquipment.text?.toString()?.trim() ?: ""
            val category = binding.editTextCategory.text?.toString()?.trim() ?: ""

            var valid = true
            if (name.isEmpty()) {
                binding.inputLayoutName.error = "Name is required"
                valid = false
            } else {
                binding.inputLayoutName.error = null
            }
            if (muscle.isEmpty()) {
                binding.inputLayoutMuscle.error = "Muscle group is required"
                valid = false
            } else {
                binding.inputLayoutMuscle.error = null
            }
            if (equipment.isEmpty()) {
                binding.inputLayoutEquipment.error = "Equipment is required"
                valid = false
            } else {
                binding.inputLayoutEquipment.error = null
            }
            if (category.isEmpty()) {
                binding.inputLayoutCategory.error = "Category is required"
                valid = false
            } else {
                binding.inputLayoutCategory.error = null
            }

            if (valid) {
                viewModel.saveCustomExercise(name, muscle, equipment, category)
            }
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            when (result) {
                is Result.Success -> findNavController().popBackStack()
                is Result.Error -> {
                    if (result.message.contains("already exists", ignoreCase = true)) {
                        binding.inputLayoutName.error = "An exercise with this name already exists."
                    } else {
                        Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
