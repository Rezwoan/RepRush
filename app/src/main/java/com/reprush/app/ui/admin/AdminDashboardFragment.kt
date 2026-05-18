package com.reprush.app.ui.admin

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressDashboard.visibility = if (loading) View.VISIBLE else View.GONE
            binding.layoutDashboardContent.visibility = if (loading) View.GONE else View.VISIBLE
        }

        viewModel.totalMembers.observe(viewLifecycleOwner) {
            binding.textViewStatTotalMembers.text = it.toString()
        }
        viewModel.activeMembers.observe(viewLifecycleOwner) { count ->
            animateIntValue(binding.textViewStatActiveMembers, count, 800)
        }
        viewModel.pendingMembers.observe(viewLifecycleOwner) {
            binding.textViewStatPendingMembers.text = it.toString()
        }
        viewModel.todayCheckIns.observe(viewLifecycleOwner) {
            binding.textViewStatTodayCheckIns.text = it.toString()
        }
        viewModel.monthlyRevenue.observe(viewLifecycleOwner) { value ->
            animateMoneyValue(binding.textViewStatMonthlyRevenue, value, 600)
        }
        viewModel.yearlyRevenue.observe(viewLifecycleOwner) { value ->
            animateMoneyValue(binding.textViewStatYearlyRevenue, value, 600)
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

    private fun animateMoneyValue(textView: TextView, target: Double, durationMs: Long) {
        ValueAnimator.ofFloat(0f, target.toFloat()).apply {
            duration = durationMs
            addUpdateListener { anim ->
                textView.text = "৳${"%.0f".format(anim.animatedValue as Float)}"
            }
            start()
        }
    }

    private fun animateIntValue(textView: TextView, target: Int, durationMs: Long) {
        ValueAnimator.ofInt(0, target).apply {
            duration = durationMs
            addUpdateListener { anim ->
                textView.text = (anim.animatedValue as Int).toString()
            }
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
