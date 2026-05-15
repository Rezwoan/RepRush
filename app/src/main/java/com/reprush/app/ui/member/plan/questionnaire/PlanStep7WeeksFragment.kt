package com.reprush.app.ui.member.plan.questionnaire

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.reprush.app.databinding.FragmentPlanStepWeeksBinding
import com.reprush.app.ui.member.plan.PlanGenerationViewModel

class PlanStep7WeeksFragment : Fragment(), QuestionnaireStep {
    private var _binding: FragmentPlanStepWeeksBinding? = null
    private val binding get() = _binding!!

    override var onSelectionChanged: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanStepWeeksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chipGroupWeeks.setOnCheckedStateChangeListener { _, _ ->
            onSelectionChanged?.invoke()
        }
    }

    override fun isSelectionMade(): Boolean =
        _binding != null && binding.chipGroupWeeks.checkedChipId != View.NO_ID

    override fun saveSelection(viewModel: PlanGenerationViewModel) {
        val chip = binding.chipGroupWeeks.findViewById<Chip>(binding.chipGroupWeeks.checkedChipId)
        viewModel.weeks = chip?.text?.toString()?.filter { it.isDigit() }?.toIntOrNull() ?: 8
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
