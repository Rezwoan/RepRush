package com.reprush.app.ui.member.plan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.reprush.app.R
import com.reprush.app.data.local.entity.PlanDayEntity
import com.reprush.app.data.local.entity.PlanExerciseEntity
import com.reprush.app.databinding.FragmentPlanDetailBinding
import com.reprush.app.databinding.ItemPlanDetailExerciseBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlanDetailFragment : Fragment() {

    private var _binding: FragmentPlanDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlanDetailViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val planId = arguments?.getString("planId") ?: run {
            findNavController().popBackStack(); return
        }

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        binding.buttonToggleEdit.setOnClickListener { viewModel.toggleEditMode() }

        viewModel.isEditMode.observe(viewLifecycleOwner) { editMode ->
            binding.buttonToggleEdit.text = if (editMode) "Done" else "Edit"
            rebuildDetail()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe
            binding.textPlanTitle.text = state.plan.planName
            binding.textPlanTitle.setOnClickListener {
                if (viewModel.isEditMode.value == true) showRenamePlanDialog(state.plan.planName)
            }
            rebuildDetail()
        }

        viewModel.load(planId)
    }

    private fun rebuildDetail() {
        val state = viewModel.state.value ?: return
        val editMode = viewModel.isEditMode.value ?: false
        val container = binding.containerPlanDetail
        container.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())
        val spacingMd = resources.getDimensionPixelSize(R.dimen.spacing_md)
        val spacingSm = resources.getDimensionPixelSize(R.dimen.spacing_sm)

        // Plan meta row
        val metaText = TextView(requireContext()).apply {
            text = "${state.plan.goal} · ${state.plan.totalWeeks} weeks · ${state.plan.daysPerWeek} days/week"
            textSize = 13f
            setTextColor(resources.getColor(R.color.on_surface_variant, null))
            setPadding(0, 0, 0, spacingMd)
        }
        container.addView(metaText)

        state.days.forEachIndexed { dayIdx, dayDetail ->
            val card = MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = spacingMd }
                cardElevation = 2f
                radius = resources.getDimension(R.dimen.shape_md)
                setCardBackgroundColor(resources.getColor(R.color.surface, null))
            }

            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(spacingMd, spacingMd, spacingMd, spacingMd)
            }

            // Day label row
            val labelRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = spacingSm }
            }
            val dayLabel = TextView(requireContext()).apply {
                text = dayDetail.day.dayLabel
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(resources.getColor(R.color.on_surface, null))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                if (editMode) {
                    setOnClickListener { showRenameDayDialog(dayDetail.day) }
                    paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                }
            }
            labelRow.addView(dayLabel)
            inner.addView(labelRow)

            // Exercises
            dayDetail.exercises.forEachIndexed { exIdx, exDetail ->
                val exBinding = ItemPlanDetailExerciseBinding.inflate(inflater, inner, false)
                exBinding.textDetailExerciseName.text = exDetail.exerciseName
                exBinding.textDetailExerciseConfig.text =
                    "${exDetail.planExercise.sets} sets · ${exDetail.planExercise.repsRange} reps · ${exDetail.planExercise.restSeconds}s rest"

                if (editMode) {
                    exBinding.buttonMoveUp.visibility = if (exIdx > 0) View.VISIBLE else View.GONE
                    exBinding.buttonEditEx.visibility = View.VISIBLE
                    exBinding.buttonDeleteEx.visibility = View.VISIBLE

                    exBinding.buttonMoveUp.setOnClickListener {
                        viewModel.moveExerciseUp(dayDetail, exIdx)
                    }
                    exBinding.buttonEditEx.setOnClickListener {
                        showEditExerciseDialog(exDetail.planExercise, exDetail.exerciseName)
                    }
                    exBinding.buttonDeleteEx.setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Remove Exercise")
                            .setMessage("Remove ${exDetail.exerciseName} from this day?")
                            .setPositiveButton("Remove") { _, _ -> viewModel.deleteExercise(exDetail.planExercise) }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
                inner.addView(exBinding.root)
            }

            // Add Exercise button (edit mode only)
            if (editMode) {
                val addBtn = MaterialButton(requireContext()).apply {
                    text = "+ Add Exercise"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.topMargin = resources.getDimensionPixelSize(R.dimen.spacing_xs) }
                    setOnClickListener { showAddExercisePicker(dayDetail.day) }
                }
                inner.addView(addBtn)
            }

            card.addView(inner)
            container.addView(card)
        }
    }

    private fun showRenamePlanDialog(currentName: String) {
        val input = TextInputEditText(requireContext()).apply { setText(currentName) }
        val layout = TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = "Plan name"
            addView(input)
            val pad = resources.getDimensionPixelSize(R.dimen.spacing_md)
            setPadding(pad, pad, pad, 0)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Rename Plan")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text?.toString()?.trim()
                if (!name.isNullOrBlank()) viewModel.updatePlanName(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDayDialog(day: PlanDayEntity) {
        val input = TextInputEditText(requireContext()).apply { setText(day.dayLabel) }
        val layout = TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = "Day label"
            addView(input)
            val pad = resources.getDimensionPixelSize(R.dimen.spacing_md)
            setPadding(pad, pad, pad, 0)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Rename Day")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val label = input.text?.toString()?.trim()
                if (!label.isNullOrBlank()) viewModel.updateDayLabel(day, label)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditExerciseDialog(planExercise: PlanExerciseEntity, exerciseName: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_exercise_config, null)
        val setsEdit = dialogView.findViewById<TextInputEditText>(R.id.editText_sets)
        val repsEdit = dialogView.findViewById<TextInputEditText>(R.id.editText_reps)
        val restEdit = dialogView.findViewById<TextInputEditText>(R.id.editText_rest)
        setsEdit.setText(planExercise.sets.toString())
        repsEdit.setText(planExercise.repsRange)
        restEdit.setText(planExercise.restSeconds.toString())

        AlertDialog.Builder(requireContext())
            .setTitle(exerciseName)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val sets = setsEdit.text?.toString()?.toIntOrNull() ?: planExercise.sets
                val reps = repsEdit.text?.toString()?.trim()?.ifEmpty { planExercise.repsRange } ?: planExercise.repsRange
                val rest = restEdit.text?.toString()?.toIntOrNull() ?: planExercise.restSeconds
                viewModel.updateExerciseConfig(planExercise, sets, reps, rest)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddExercisePicker(day: PlanDayEntity) {
        val sheet = PlanExercisePickerBottomSheet.newInstance()
        sheet.onExerciseSelected = { exerciseId, exerciseName ->
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_exercise_config, null)
            val setsEdit = dialogView.findViewById<TextInputEditText>(R.id.editText_sets)
            val repsEdit = dialogView.findViewById<TextInputEditText>(R.id.editText_reps)
            val restEdit = dialogView.findViewById<TextInputEditText>(R.id.editText_rest)
            setsEdit.setText("3")
            repsEdit.setText("8-12")
            restEdit.setText("90")

            AlertDialog.Builder(requireContext())
                .setTitle(exerciseName)
                .setView(dialogView)
                .setPositiveButton("Add") { _, _ ->
                    val sets = setsEdit.text?.toString()?.toIntOrNull() ?: 3
                    val reps = repsEdit.text?.toString()?.trim()?.ifEmpty { "8-12" } ?: "8-12"
                    val rest = restEdit.text?.toString()?.toIntOrNull() ?: 90
                    viewModel.addExerciseToDay(day, exerciseId, exerciseName, sets, reps, rest)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        sheet.show(childFragmentManager, "detail_exercise_picker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
