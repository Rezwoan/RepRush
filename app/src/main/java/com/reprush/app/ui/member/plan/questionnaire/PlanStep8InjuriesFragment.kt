package com.reprush.app.ui.member.plan.questionnaire

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.reprush.app.databinding.FragmentPlanStepInjuriesBinding
import com.reprush.app.ui.member.plan.PlanGenerationViewModel

class PlanStep8InjuriesFragment : Fragment(), QuestionnaireStep {
    private var _binding: FragmentPlanStepInjuriesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanStepInjuriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun isSelectionMade(): Boolean = true // injuries is optional free text

    override fun saveSelection(viewModel: PlanGenerationViewModel) {
        viewModel.injuries = binding.editTextInjuries.text?.toString()?.trim() ?: ""
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
