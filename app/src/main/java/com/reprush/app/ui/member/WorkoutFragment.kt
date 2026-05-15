package com.reprush.app.ui.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.reprush.app.R
import com.reprush.app.databinding.FragmentWorkoutBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardExerciseLibrary.setOnClickListener {
            findNavController().navigate(R.id.action_workoutFragment_to_exerciseLibraryFragment)
        }

        binding.cardMyPlans.setOnClickListener {
            findNavController().navigate(R.id.action_workoutFragment_to_planListFragment)
        }

        binding.cardGeneratePlan.setOnClickListener {
            findNavController().navigate(R.id.action_workoutFragment_to_planQuestionnaireFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
