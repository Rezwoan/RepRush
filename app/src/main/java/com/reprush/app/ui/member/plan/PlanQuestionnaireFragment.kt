package com.reprush.app.ui.member.plan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.reprush.app.R
import com.reprush.app.databinding.FragmentPlanQuestionnaireBinding
import com.reprush.app.ui.member.plan.questionnaire.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlanQuestionnaireFragment : Fragment() {

    private var _binding: FragmentPlanQuestionnaireBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlanGenerationViewModel by activityViewModels()

    private val steps: List<QuestionnaireStep> by lazy {
        listOf(
            PlanStep1GoalFragment(),
            PlanStep2DaysFragment(),
            PlanStep3SplitFragment(),
            PlanStep4DurationFragment(),
            PlanStep5EquipmentFragment(),
            PlanStep6LevelFragment(),
            PlanStep7WeeksFragment(),
            PlanStep8InjuriesFragment()
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanQuestionnaireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = steps.size
            override fun createFragment(position: Int) = steps[position] as Fragment
        }
        binding.viewPagerQuestionnaire.adapter = adapter
        binding.viewPagerQuestionnaire.isUserInputEnabled = false

        // Register selection-changed callbacks on every step so the host knows when to enable Next.
        steps.forEachIndexed { index, step ->
            step.onSelectionChanged = {
                if (binding.viewPagerQuestionnaire.currentItem == index) {
                    refreshNextButton(index)
                }
            }
        }

        updateStepIndicator(0)
        refreshNextButton(0)

        binding.viewPagerQuestionnaire.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateStepIndicator(position)
                updateButtons(position)
                refreshNextButton(position)
            }
        })

        binding.buttonPrevious.setOnClickListener {
            val current = binding.viewPagerQuestionnaire.currentItem
            if (current > 0) {
                binding.viewPagerQuestionnaire.currentItem = current - 1
            } else {
                findNavController().popBackStack()
            }
        }

        binding.buttonNext.setOnClickListener {
            val current = binding.viewPagerQuestionnaire.currentItem
            val step = steps[current]

            if (!step.isSelectionMade()) {
                Toast.makeText(requireContext(), "Please make a selection to continue", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            step.saveSelection(viewModel)

            if (current < steps.size - 1) {
                binding.viewPagerQuestionnaire.currentItem = current + 1
            } else {
                findNavController().navigate(R.id.action_planQuestionnaireFragment_to_geminiLoadingFragment)
            }
        }
    }

    private fun refreshNextButton(position: Int) {
        val isMade = steps[position].isSelectionMade()
        binding.buttonNext.isEnabled = isMade
        binding.buttonNext.alpha = if (isMade) 1.0f else 0.5f
    }

    private fun updateStepIndicator(position: Int) {
        binding.textViewStepIndicator.text = "Step ${position + 1} of 8"
        binding.progressBarSteps.progress = position + 1
    }

    private fun updateButtons(position: Int) {
        binding.buttonPrevious.text = if (position == 0) "Cancel" else "Back"
        binding.buttonNext.text = if (position == steps.size - 1) "Generate Plan" else "Next"
    }

    override fun onDestroyView() {
        // Clear callbacks to avoid leaking references to this Fragment's binding.
        steps.forEach { it.onSelectionChanged = null }
        super.onDestroyView()
        _binding = null
    }
}
