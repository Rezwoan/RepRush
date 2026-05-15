package com.reprush.app.ui.member.plan.questionnaire

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.reprush.app.databinding.FragmentPlanStepEquipmentBinding
import com.reprush.app.ui.member.plan.PlanGenerationViewModel

class PlanStep5EquipmentFragment : Fragment(), QuestionnaireStep {
    private var _binding: FragmentPlanStepEquipmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanStepEquipmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun isSelectionMade(): Boolean = binding.chipGroupEquipment.checkedChipId != View.NO_ID

    override fun saveSelection(viewModel: PlanGenerationViewModel) {
        val chip = binding.chipGroupEquipment.findViewById<Chip>(binding.chipGroupEquipment.checkedChipId)
        viewModel.equipment = chip?.text?.toString() ?: "Full Gym"
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
