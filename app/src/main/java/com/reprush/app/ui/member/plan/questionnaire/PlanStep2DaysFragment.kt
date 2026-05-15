package com.reprush.app.ui.member.plan.questionnaire

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.reprush.app.databinding.FragmentPlanStepDaysBinding
import com.reprush.app.ui.member.plan.PlanGenerationViewModel

class PlanStep2DaysFragment : Fragment(), QuestionnaireStep {
    private var _binding: FragmentPlanStepDaysBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanStepDaysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun isSelectionMade(): Boolean = binding.chipGroupDays.checkedChipId != View.NO_ID

    override fun saveSelection(viewModel: PlanGenerationViewModel) {
        val chip = binding.chipGroupDays.findViewById<Chip>(binding.chipGroupDays.checkedChipId)
        viewModel.daysPerWeek = chip?.text?.toString()?.filter { it.isDigit() }?.toIntOrNull() ?: 3
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
