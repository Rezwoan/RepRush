package com.reprush.app.ui.member.plan.questionnaire

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.reprush.app.databinding.FragmentPlanStepSplitBinding
import com.reprush.app.ui.member.plan.PlanGenerationViewModel

class PlanStep3SplitFragment : Fragment(), QuestionnaireStep {
    private var _binding: FragmentPlanStepSplitBinding? = null
    private val binding get() = _binding!!

    override var onSelectionChanged: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanStepSplitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chipGroupSplit.setOnCheckedStateChangeListener { _, _ ->
            onSelectionChanged?.invoke()
        }
    }

    override fun isSelectionMade(): Boolean =
        _binding != null && binding.chipGroupSplit.checkedChipId != View.NO_ID

    override fun saveSelection(viewModel: PlanGenerationViewModel) {
        val chip = binding.chipGroupSplit.findViewById<Chip>(binding.chipGroupSplit.checkedChipId)
        viewModel.splitType = chip?.text?.toString() ?: ""
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
