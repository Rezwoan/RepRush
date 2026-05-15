package com.reprush.app.ui.member.plan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.R
import com.reprush.app.databinding.FragmentPlanSummaryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlanSummaryFragment : Fragment() {

    private var _binding: FragmentPlanSummaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlanGenerationViewModel by activityViewModels()
    private lateinit var dayAdapter: PlanDayAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dayAdapter = PlanDayAdapter()
        binding.recyclerViewPlanDays.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPlanDays.adapter = dayAdapter

        val plan = viewModel.importedPlan.value
        if (plan != null) {
            binding.textViewPlanName.text = plan.planName
            binding.textViewPlanGoal.text = "${plan.goal} · ${plan.weeks} weeks · ${plan.daysPerWeek} days/week"
            dayAdapter.submitList(plan.days)

            if (plan.unverifiedExercises.isNotEmpty()) {
                binding.textViewUnverifiedWarning.visibility = View.VISIBLE
                binding.textViewUnverifiedWarning.text =
                    "This plan uses ${plan.unverifiedExercises.size} unverified exercise(s). Review them in your library."
            } else {
                binding.textViewUnverifiedWarning.visibility = View.GONE
            }
        }

        binding.buttonSetActive.setOnClickListener {
            Snackbar.make(binding.root, "Plan saved and set as active!", Snackbar.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_planSummaryFragment_to_workoutFragment)
        }

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
