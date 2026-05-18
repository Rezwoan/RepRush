package com.reprush.app.ui.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.reprush.app.R
import com.reprush.app.databinding.FragmentProgressBinding
import com.reprush.app.ui.member.progress.ProgressPagerAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ProgressPagerAdapter(this)
        binding.viewPagerProgress.adapter = adapter

        TabLayoutMediator(binding.tabLayoutProgress, binding.viewPagerProgress) { tab, position ->
            tab.text = when (position) {
                0 -> "Overview"
                1 -> "Weight"
                2 -> "Strength"
                else -> ""
            }
            tab.icon = ContextCompat.getDrawable(requireContext(), when (position) {
                0 -> R.drawable.ic_dashboard
                1 -> R.drawable.ic_fitness_center
                2 -> R.drawable.ic_trending_up
                else -> R.drawable.ic_dashboard
            })
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
