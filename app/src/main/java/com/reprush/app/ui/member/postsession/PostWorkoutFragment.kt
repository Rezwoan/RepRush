package com.reprush.app.ui.member.postsession

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.reprush.app.R
import com.reprush.app.databinding.FragmentPostWorkoutBinding
import com.reprush.app.ui.member.session.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PostWorkoutFragment : Fragment() {

    private var _binding: FragmentPostWorkoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostWorkoutViewModel by viewModels()
    private val sessionViewModel: SessionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionId = arguments?.getString("sessionId") ?: run {
            navigateHome()
            return
        }

        viewModel.runPipeline(sessionId)

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarPostWorkout.visibility = if (loading) View.VISIBLE else View.GONE
            binding.scrollViewPostWorkout.visibility = if (loading) View.GONE else View.VISIBLE
        }

        viewModel.result.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            when (result) {
                is com.reprush.app.data.repository.Result.Success -> bindResult(result.data)
                is com.reprush.app.data.repository.Result.Error -> navigateHome()
            }
            viewModel.clearResult()
        }

        binding.buttonBackToHome.setOnClickListener { navigateHome() }
    }

    private fun bindResult(data: com.reprush.app.data.gamification.PostWorkoutResult) {
        binding.textViewTotalPoints.text = data.pointsBreakdown.totalPoints.toString()

        // Points breakdown
        binding.textViewAttendancePoints.text = "+${data.pointsBreakdown.attendancePoints}"
        binding.textViewExercisePoints.text = "+${data.pointsBreakdown.exercisePoints}"
        binding.textViewSetPoints.text = "+${data.pointsBreakdown.setPoints}"
        binding.textViewPrPoints.text = "+${data.pointsBreakdown.prPoints}"

        if (data.pointsBreakdown.streakBonus > 0) {
            binding.rowStreakBonus.visibility = View.VISIBLE
            binding.textViewStreakBonus.text = "+${data.pointsBreakdown.streakBonus}"
        } else {
            binding.rowStreakBonus.visibility = View.GONE
        }

        if (data.pointsBreakdown.capApplied) {
            binding.textViewCapNotice.visibility = View.VISIBLE
        } else {
            binding.textViewCapNotice.visibility = View.GONE
        }

        // Streak
        if (data.streakUpdate.currentStreak > 0) {
            binding.rowStreak.visibility = View.VISIBLE
            binding.textViewStreakCount.text = "${data.streakUpdate.currentStreak} day streak"
        } else {
            binding.rowStreak.visibility = View.GONE
        }

        // PR section
        if (data.newPRs.isEmpty()) {
            binding.sectionPRs.visibility = View.GONE
        } else {
            binding.sectionPRs.visibility = View.VISIBLE
            addPRCards(data.newPRs)
        }

        // Achievement section
        if (data.newAchievements.isEmpty()) {
            binding.sectionAchievements.visibility = View.GONE
        } else {
            binding.sectionAchievements.visibility = View.VISIBLE
            addAchievementCards(data.newAchievements)
        }

        animateTotalPoints(data.pointsBreakdown.totalPoints)
    }

    private fun addPRCards(prs: List<com.reprush.app.data.gamification.NewPR>) {
        binding.containerPRCards.removeAllViews()
        for (pr in prs) {
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = "${pr.exerciseName}  ${pr.reps}×${pr.weight}kg  🏆"
                isClickable = false
            }
            binding.containerPRCards.addView(chip)
        }
    }

    private fun addAchievementCards(badges: List<String>) {
        binding.containerAchievements.removeAllViews()
        for (badgeId in badges) {
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = badgeDisplayName(badgeId)
                isClickable = false
            }
            binding.containerAchievements.addView(chip)
        }
    }

    private fun animateTotalPoints(total: Int) {
        val animator = android.animation.ValueAnimator.ofInt(0, total).apply {
            duration = 600
            addUpdateListener { anim ->
                binding.textViewTotalPoints.text = anim.animatedValue.toString()
            }
        }
        animator.start()
    }

    private fun badgeDisplayName(badgeId: String) = when (badgeId) {
        "first_rep" -> "First Rep"
        "on_a_roll" -> "On A Roll"
        "unstoppable" -> "Unstoppable"
        "century" -> "Century"
        "pr_machine" -> "PR Machine"
        "plan_master" -> "Plan Master"
        "heavy_bench" -> "Heavy Bench"
        "heavy_squat" -> "Heavy Squat"
        "heavy_deadlift" -> "Heavy Deadlift"
        "comeback" -> "Comeback"
        "full_house" -> "Full House"
        else -> badgeId
    }

    private fun navigateHome() {
        sessionViewModel.clearSession()
        findNavController().navigate(
            R.id.action_postWorkoutFragment_to_homeFragment,
            null,
            androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, false)
                .build()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
