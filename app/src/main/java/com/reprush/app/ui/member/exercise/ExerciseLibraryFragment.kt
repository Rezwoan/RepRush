package com.reprush.app.ui.member.exercise

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.reprush.app.R
import com.reprush.app.databinding.FragmentExerciseLibraryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExerciseLibraryFragment : Fragment() {

    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExerciseLibraryViewModel by viewModels()
    private lateinit var adapter: ExerciseAdapter

    private var selectedMuscle = ""
    private var selectedEquipment = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ExerciseAdapter { exercise ->
            findNavController().navigate(
                R.id.action_exerciseLibraryFragment_to_exerciseDetailFragment,
                bundleOf("exerciseId" to exercise.id)
            )
        }

        binding.recyclerViewExercises.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewExercises.adapter = adapter

        viewModel.exercises.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.layoutEmptyExercises.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewExercises.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarExerciseLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }

        binding.editTextSearchExercises.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.loadExercises(query = s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        setupMuscleChips()
        setupEquipmentChips()

        binding.buttonClearFilters.setOnClickListener {
            binding.editTextSearchExercises.setText("")
            binding.chipGroupMuscle.clearCheck()
            binding.chipGroupEquipment.clearCheck()
            selectedMuscle = ""
            selectedEquipment = ""
            viewModel.loadExercises("", "", "")
        }

        binding.fabAddCustomExercise.setOnClickListener {
            findNavController().navigate(R.id.action_exerciseLibraryFragment_to_addCustomExerciseFragment)
        }

        viewModel.loadExercises()
    }

    private fun setupMuscleChips() {
        val muscles = listOf("All", "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Abdominals", "Quadriceps", "Hamstrings", "Glutes", "Calves", "Forearms")
        muscles.forEach { muscle ->
            val chip = Chip(requireContext()).apply {
                text = muscle
                isCheckable = true
                id = View.generateViewId()
            }
            binding.chipGroupMuscle.addView(chip)
        }
        binding.chipGroupMuscle.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = if (checkedIds.isEmpty()) null else group.findViewById<Chip>(checkedIds[0])
            selectedMuscle = if (chip == null || chip.text == "All") "" else chip.text.toString()
            viewModel.loadExercises(muscle = selectedMuscle)
        }
    }

    private fun setupEquipmentChips() {
        val equipments = listOf("All", "Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight")
        equipments.forEach { eq ->
            val chip = Chip(requireContext()).apply {
                text = eq
                isCheckable = true
                id = View.generateViewId()
            }
            binding.chipGroupEquipment.addView(chip)
        }
        binding.chipGroupEquipment.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = if (checkedIds.isEmpty()) null else group.findViewById<Chip>(checkedIds[0])
            selectedEquipment = if (chip == null || chip.text == "All") "" else chip.text.toString()
            viewModel.loadExercises(equipment = selectedEquipment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
