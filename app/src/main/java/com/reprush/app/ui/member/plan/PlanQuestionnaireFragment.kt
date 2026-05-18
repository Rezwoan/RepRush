package com.reprush.app.ui.member.plan

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.reprush.app.R
import com.reprush.app.databinding.FragmentPlanQuestionnaireBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlanQuestionnaireFragment : Fragment() {

    private var _binding: FragmentPlanQuestionnaireBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlanGenerationViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanQuestionnaireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        val changeListener = ChipGroup.OnCheckedStateChangeListener { _, _ -> updateUI() }
        binding.chipGroupGoal.setOnCheckedStateChangeListener(changeListener)
        binding.chipGroupDays.setOnCheckedStateChangeListener(changeListener)
        binding.chipGroupSplit.setOnCheckedStateChangeListener(changeListener)
        binding.chipGroupDuration.setOnCheckedStateChangeListener(changeListener)
        binding.chipGroupEquipment.setOnCheckedStateChangeListener(changeListener)
        binding.chipGroupLevel.setOnCheckedStateChangeListener(changeListener)
        binding.chipGroupWeeks.setOnCheckedStateChangeListener(changeListener)

        binding.editTextInjuries.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateUI() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.buttonGenerate.setOnClickListener { submitToAi() }

        updateUI()
    }

    private fun chipText(group: ChipGroup): String? {
        val id = group.checkedChipId
        if (id == View.NO_ID) return null
        return group.findViewById<Chip>(id)?.text?.toString()
    }

    private fun isComplete(): Boolean =
        chipText(binding.chipGroupGoal) != null &&
        chipText(binding.chipGroupDays) != null &&
        chipText(binding.chipGroupSplit) != null &&
        chipText(binding.chipGroupDuration) != null &&
        chipText(binding.chipGroupEquipment) != null &&
        chipText(binding.chipGroupLevel) != null &&
        chipText(binding.chipGroupWeeks) != null

    private fun updateUI() {
        val complete = isComplete()

        binding.textSummaryGoal.text = chipText(binding.chipGroupGoal) ?: "—"
        binding.textSummaryDays.text = chipText(binding.chipGroupDays) ?: "—"
        binding.textSummarySplit.text = chipText(binding.chipGroupSplit) ?: "—"
        binding.textSummaryDuration.text = chipText(binding.chipGroupDuration) ?: "—"
        binding.textSummaryEquipment.text = chipText(binding.chipGroupEquipment) ?: "—"
        binding.textSummaryLevel.text = chipText(binding.chipGroupLevel) ?: "—"
        binding.textSummaryWeeks.text = chipText(binding.chipGroupWeeks) ?: "—"
        val injuries = binding.editTextInjuries.text?.toString()?.trim() ?: ""
        binding.textSummaryInjuries.text = injuries.ifEmpty { "None" }

        binding.cardSummary.visibility = if (complete) View.VISIBLE else View.GONE
        binding.buttonGenerate.isEnabled = complete
        binding.buttonGenerate.alpha = if (complete) 1f else 0.5f
    }

    private fun submitToAi() {
        viewModel.goal = chipText(binding.chipGroupGoal) ?: return
        viewModel.daysPerWeek = chipText(binding.chipGroupDays)?.filter { it.isDigit() }?.toIntOrNull() ?: 3
        viewModel.splitType = chipText(binding.chipGroupSplit) ?: return
        viewModel.sessionDuration = chipText(binding.chipGroupDuration)?.filter { it.isDigit() }?.toIntOrNull() ?: 60
        viewModel.equipment = chipText(binding.chipGroupEquipment) ?: "Full Gym"
        viewModel.fitnessLevel = chipText(binding.chipGroupLevel) ?: return
        viewModel.weeks = chipText(binding.chipGroupWeeks)?.filter { it.isDigit() }?.toIntOrNull() ?: 8
        viewModel.injuries = binding.editTextInjuries.text?.toString()?.trim() ?: ""
        viewModel.resetState()
        findNavController().navigate(R.id.action_planQuestionnaireFragment_to_geminiLoadingFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
