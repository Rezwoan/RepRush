package com.reprush.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.reprush.app.R
import com.reprush.app.databinding.FragmentAdminDashboardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminDashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.totalMembers.observe(viewLifecycleOwner) {
            binding.textViewStatTotalMembers.text = it.toString()
        }
        viewModel.activeMembers.observe(viewLifecycleOwner) {
            binding.textViewStatActiveMembers.text = it.toString()
        }
        viewModel.pendingMembers.observe(viewLifecycleOwner) {
            binding.textViewStatPendingMembers.text = it.toString()
        }
        viewModel.todayCheckIns.observe(viewLifecycleOwner) {
            binding.textViewStatTodayCheckIns.text = it.toString()
        }
        viewModel.monthlyRevenue.observe(viewLifecycleOwner) {
            binding.textViewStatMonthlyRevenue.text = "৳${"%.0f".format(it)}"
        }
        viewModel.yearlyRevenue.observe(viewLifecycleOwner) {
            binding.textViewStatYearlyRevenue.text = "৳${"%.0f".format(it)}"
        }

        binding.cardPendingMembers.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_pendingMembersFragment)
        }

        binding.cardMemberDirectory.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_memberDirectoryFragment)
        }

        binding.cardPackages.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_packageListFragment)
        }

        binding.cardAnnouncements.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_announcementListFragment)
        }

        binding.cardRegisterMember.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_manualRegisterFragment)
        }

        viewModel.loadStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
