package com.reprush.app.ui.member.plan

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.reprush.app.databinding.FragmentManualPlanBuilderBinding
import com.reprush.app.databinding.ItemBuilderExerciseBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManualPlanBuilderFragment : Fragment() {

    private var _binding: FragmentManualPlanBuilderBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ManualPlanBuilderViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManualPlanBuilderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = refreshSaveButton()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        }
        binding.editTextPlanName.addTextChangedListener(textWatcher)

        binding.chipGroupGoal.setOnCheckedStateChangeListener { _, _ -> refreshSaveButton() }
        binding.chipGroupWeeks.setOnCheckedStateChangeListener { _, _ -> refreshSaveButton() }

        binding.chipGroupDays.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = checkedIds.firstOrNull()?.let { group.findViewById<Chip>(it) }
            val count = chip?.text?.toString()?.toIntOrNull() ?: return@setOnCheckedStateChangeListener
            viewModel.initDays(count)
        }

        viewModel.days.observe(viewLifecycleOwner) { days ->
            rebuildDayContainers(days)
            refreshSaveButton()
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                viewModel.clearSaveResult()
                findNavController().popBackStack()
            }
        }

        binding.buttonSavePlan.setOnClickListener {
            val name = binding.editTextPlanName.text?.toString() ?: return@setOnClickListener
            val goal = selectedChipText(binding.chipGroupGoal) ?: return@setOnClickListener
            val weeksText = selectedChipText(binding.chipGroupWeeks) ?: return@setOnClickListener
            val weeks = weeksText.filter { it.isDigit() }.toIntOrNull() ?: 8
            viewModel.savePlan(name, goal, weeks)
        }
    }

    private fun selectedChipText(group: ChipGroup): String? {
        val id = group.checkedChipId
        if (id == View.NO_ID) return null
        return group.findViewById<Chip>(id)?.text?.toString()
    }

    private fun refreshSaveButton() {
        val planName = binding.editTextPlanName.text?.toString() ?: ""
        val goal = selectedChipText(binding.chipGroupGoal) ?: ""
        val ready = viewModel.isReadyToSave(planName, goal) && selectedChipText(binding.chipGroupWeeks) != null
        binding.buttonSavePlan.isEnabled = ready
        binding.buttonSavePlan.alpha = if (ready) 1f else 0.5f
    }

    private fun rebuildDayContainers(days: List<DraftDay>) {
        val container = binding.containerDays
        container.removeAllViews()
        days.forEachIndexed { dayIndex, day ->
            val dayCard = buildDayCard(dayIndex, day)
            container.addView(dayCard)
        }
    }

    private fun buildDayCard(dayIndex: Int, day: DraftDay): View {
        val inflater = LayoutInflater.from(requireContext())
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = resources.getDimensionPixelSize(com.reprush.app.R.dimen.spacing_md) }
            cardElevation = 2f
            radius = resources.getDimension(com.reprush.app.R.dimen.shape_md)
            setCardBackgroundColor(resources.getColor(com.reprush.app.R.color.surface, null))
        }

        val inner = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = resources.getDimensionPixelSize(com.reprush.app.R.dimen.spacing_md)
            setPadding(pad, pad, pad, pad)
        }

        // Day label row
        val labelLayout = TextInputLayout(
            requireContext(),
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = resources.getDimensionPixelSize(com.reprush.app.R.dimen.spacing_sm) }
            hint = "Day ${day.dayNumber} label"
        }
        val labelEdit = TextInputEditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setText(day.dayLabel)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.updateDayLabel(dayIndex, s?.toString() ?: "")
                }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        labelLayout.addView(labelEdit)
        inner.addView(labelLayout)

        // Exercises container
        val exercisesContainer = LinearLayout(requireContext()).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
        }
        inner.addView(exercisesContainer)

        // Populate exercises
        day.exercises.forEachIndexed { exIndex, ex ->
            val exBinding = ItemBuilderExerciseBinding.inflate(inflater, exercisesContainer, false)
            exBinding.textExerciseName.text = ex.exerciseName
            exBinding.textExerciseConfig.text = "${ex.sets} sets · ${ex.repsRange} reps · ${ex.restSeconds}s rest"
            exBinding.buttonEditExercise.setOnClickListener {
                showExerciseConfigDialog(dayIndex, exIndex, ex)
            }
            exBinding.buttonRemoveExercise.setOnClickListener {
                viewModel.removeExercise(dayIndex, exIndex)
            }
            exercisesContainer.addView(exBinding.root)
        }

        // Add Exercise button
        val addBtn = com.google.android.material.button.MaterialButton(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = resources.getDimensionPixelSize(com.reprush.app.R.dimen.spacing_xs) }
            text = "+ Add Exercise"
            setOnClickListener { showExercisePicker(dayIndex) }
        }
        inner.addView(addBtn)

        card.addView(inner)
        return card
    }

    private fun showExercisePicker(dayIndex: Int) {
        val sheet = PlanExercisePickerBottomSheet.newInstance()
        sheet.onExerciseSelected = { exerciseId, exerciseName ->
            showExerciseConfigDialog(dayIndex, -1, DraftExercise(exerciseId, exerciseName))
        }
        sheet.show(childFragmentManager, "plan_exercise_picker")
    }

    private fun showExerciseConfigDialog(dayIndex: Int, exerciseIndex: Int, current: DraftExercise) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(com.reprush.app.R.layout.dialog_exercise_config, null)
        val setsEdit = dialogView.findViewById<TextInputEditText>(com.reprush.app.R.id.editText_sets)
        val repsEdit = dialogView.findViewById<TextInputEditText>(com.reprush.app.R.id.editText_reps)
        val restEdit = dialogView.findViewById<TextInputEditText>(com.reprush.app.R.id.editText_rest)

        setsEdit.setText(current.sets.toString())
        repsEdit.setText(current.repsRange)
        restEdit.setText(current.restSeconds.toString())

        AlertDialog.Builder(requireContext())
            .setTitle(current.exerciseName)
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val sets = setsEdit.text?.toString()?.trim()?.toIntOrNull() ?: current.sets
                val reps = repsEdit.text?.toString()?.trim()?.ifEmpty { current.repsRange } ?: current.repsRange
                val rest = restEdit.text?.toString()?.trim()?.toIntOrNull() ?: current.restSeconds
                if (exerciseIndex == -1) {
                    viewModel.addExercise(dayIndex, current.copy(sets = sets, repsRange = reps, restSeconds = rest))
                } else {
                    viewModel.updateExercise(dayIndex, exerciseIndex, sets, reps, rest)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
