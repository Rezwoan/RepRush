package com.reprush.app.ui.member.exercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.reprush.app.databinding.FragmentExerciseDetailBinding
import com.reprush.app.utils.AssetImageLoader
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExerciseDetailFragment : Fragment() {

    private var _binding: FragmentExerciseDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExerciseDetailViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exerciseId = arguments?.getString("exerciseId") ?: return

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        viewModel.exercise.observe(viewLifecycleOwner) { exercise ->
            if (exercise == null) return@observe

            binding.textViewExerciseDetailName.text = exercise.name
            binding.textViewPrimaryMuscle.text = "Primary: ${exercise.primaryMuscle}"

            if (!exercise.secondaryMuscles.isNullOrEmpty()) {
                binding.textViewSecondaryMuscles.text = "Secondary: ${exercise.secondaryMuscles}"
                binding.textViewSecondaryMuscles.visibility = View.VISIBLE
            } else {
                binding.textViewSecondaryMuscles.visibility = View.GONE
            }

            binding.textViewEquipment.text = exercise.equipment
            binding.textViewExerciseCategory.text = exercise.category

            binding.chipCustomBadge.visibility = if (exercise.isCustom == 1) View.VISIBLE else View.GONE
            binding.chipUnverifiedBadge.visibility = if (exercise.isVerified == 0) View.VISIBLE else View.GONE

            binding.imageViewExerciseFull.contentDescription = "${exercise.name} exercise demonstration"
            AssetImageLoader.load(requireContext(), exercise.imageUrl, binding.imageViewExerciseFull)

            binding.textViewNoPrYet.visibility = View.VISIBLE
            binding.textViewInsufficientData.visibility = View.VISIBLE
        }

        viewModel.loadExercise(exerciseId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
