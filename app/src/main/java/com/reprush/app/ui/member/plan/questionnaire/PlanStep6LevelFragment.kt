package com.reprush.app.ui.member.plan.questionnaire

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.reprush.app.databinding.FragmentPlanStepLevelBinding
import com.reprush.app.ui.member.plan.PlanGenerationViewModel

class PlanStep6LevelFragment : Fragment(), QuestionnaireStep {
    private var _binding: FragmentPlanStepLevelBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanStepLevelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun isSelectionMade(): Boolean = binding.chipGroupLevel.checkedChipId != View.NO_ID

    override fun saveSelection(viewModel: PlanGenerationViewModel) {
        val chip = binding.chipGroupLevel.findViewById<Chip>(binding.chipGroupLevel.checkedChipId)
        viewModel.fitnessLevel = chip?.text?.toString() ?: ""
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
