package com.reprush.app.ui.member.plan.questionnaire

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.reprush.app.databinding.FragmentPlanStepGoalBinding
import com.reprush.app.ui.member.plan.PlanGenerationViewModel

class PlanStep1GoalFragment : Fragment(), QuestionnaireStep {
    private var _binding: FragmentPlanStepGoalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanStepGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun isSelectionMade(): Boolean = binding.chipGroupGoal.checkedChipId != View.NO_ID

    override fun saveSelection(viewModel: PlanGenerationViewModel) {
        val chip = binding.chipGroupGoal.findViewById<Chip>(binding.chipGroupGoal.checkedChipId)
        viewModel.goal = chip?.text?.toString() ?: ""
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
