package com.reprush.app.ui.member.plan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.reprush.app.R
import com.reprush.app.databinding.FragmentPlanListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlanListFragment : Fragment() {

    private var _binding: FragmentPlanListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlanListViewModel by viewModels()
    private lateinit var adapter: PlanListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlanListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PlanListAdapter(
            onSetActive = { plan -> viewModel.setActive(plan) },
            onDelete = { plan ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Plan")
                    .setMessage("Are you sure you want to delete \"${plan.planName}\"?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deletePlan(plan) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.recyclerViewPlans.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPlans.adapter = adapter

        viewModel.plans.observe(viewLifecycleOwner) { plans ->
            adapter.submitList(plans)
            binding.layoutEmptyPlans.visibility = if (plans.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewPlans.visibility = if (plans.isEmpty()) View.GONE else View.VISIBLE
        }

        binding.buttonGeneratePlan.setOnClickListener {
            findNavController().navigate(R.id.action_planListFragment_to_planQuestionnaireFragment)
        }

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        viewModel.loadPlans()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
