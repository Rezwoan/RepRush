package com.reprush.app.ui.member.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.databinding.FragmentLeaderboardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeaderboardViewModel by viewModels()
    private val adapter = LeaderboardAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewLeaderboard.adapter = adapter

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarLeaderboard.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.entries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
            if (entries.isEmpty()) {
                binding.textViewEmptyState.visibility = View.VISIBLE
                binding.recyclerViewLeaderboard.visibility = View.GONE
            } else {
                binding.textViewEmptyState.visibility = View.GONE
                binding.recyclerViewLeaderboard.visibility = View.VISIBLE
            }

            // Pin current user row at bottom if outside top 10
            val currentUserRank = viewModel.currentUserRank.value ?: -1
            val currentUser = entries.firstOrNull { it.isCurrentUser }
            if (currentUser != null && currentUserRank > 10) {
                binding.dividerPinnedUser.visibility = View.VISIBLE
                binding.layoutPinnedUser.visibility = View.VISIBLE
                binding.textViewPinnedRank.text = "#$currentUserRank"
                binding.textViewPinnedName.text = currentUser.entry.displayName.ifBlank { "You" }
                binding.textViewPinnedPoints.text = "${currentUser.entry.points} pts"
            } else {
                binding.dividerPinnedUser.visibility = View.GONE
                binding.layoutPinnedUser.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error ?: return@observe
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            viewModel.clearError()
        }

        viewModel.loadLeaderboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
