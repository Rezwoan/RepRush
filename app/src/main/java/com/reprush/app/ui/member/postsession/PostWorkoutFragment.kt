package com.reprush.app.ui.member.postsession

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.reprush.app.R
import com.reprush.app.data.gamification.NewPR
import com.reprush.app.data.gamification.PostWorkoutResult
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentPostWorkoutBinding
import com.reprush.app.databinding.ItemAchievementCardBinding
import com.reprush.app.databinding.ItemPrCardBinding
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

        val sessionId = arguments?.getString("sessionId").orEmpty()
        if (sessionId.isEmpty()) { navigateHome(); return }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarPostWorkout.visibility = if (loading) View.VISIBLE else View.GONE
            binding.scrollViewPostWorkout.visibility = if (loading) View.GONE else View.VISIBLE
        }

        viewModel.result.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            when (result) {
                is Result.Success -> bindResult(result.data)
                is Result.Error -> navigateHome()
            }
            viewModel.clearResult()
        }

        binding.buttonBackToHome.setOnClickListener { navigateHome() }

        viewModel.runPipeline(sessionId)
    }

    private fun bindResult(data: PostWorkoutResult) {
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

        binding.textViewCapNotice.visibility =
            if (data.pointsBreakdown.capApplied) View.VISIBLE else View.GONE

        if (data.streakUpdate.currentStreak > 0) {
            binding.rowStreak.visibility = View.VISIBLE
            binding.textViewStreakCount.text = "${data.streakUpdate.currentStreak} day streak"
        } else {
            binding.rowStreak.visibility = View.GONE
        }

        if (data.newPRs.isEmpty()) {
            binding.sectionPRs.visibility = View.GONE
        } else {
            binding.sectionPRs.visibility = View.VISIBLE
            addPRCards(data.newPRs)
        }

        if (data.newAchievements.isEmpty()) {
            binding.sectionAchievements.visibility = View.GONE
        } else {
            binding.sectionAchievements.visibility = View.VISIBLE
            addAchievementCards(data.newAchievements)
        }

        animateTotalPoints(data.pointsBreakdown.totalPoints)
    }

    private fun addPRCards(prs: List<NewPR>) {
        binding.containerPRCards.removeAllViews()
        prs.forEachIndexed { index, pr ->
            val cardBinding = ItemPrCardBinding.inflate(
                LayoutInflater.from(requireContext()), binding.containerPRCards, false
            )
            cardBinding.textViewPrExerciseName.text = pr.exerciseName
            cardBinding.textViewPrValue.text = "${pr.reps}×${pr.weight}kg"

            cardBinding.root.alpha = 0f
            cardBinding.root.scaleX = 0.8f
            cardBinding.root.scaleY = 0.8f
            binding.containerPRCards.addView(cardBinding.root)

            cardBinding.root.postDelayed({
                ObjectAnimator.ofFloat(cardBinding.root, "alpha", 0f, 1f).apply {
                    duration = 400
                    start()
                }
                ObjectAnimator.ofFloat(cardBinding.root, "scaleX", 0.8f, 1f).apply {
                    duration = 400
                    start()
                }
                ObjectAnimator.ofFloat(cardBinding.root, "scaleY", 0.8f, 1f).apply {
                    duration = 400
                    start()
                }
            }, index * 100L)
        }
    }

    private fun addAchievementCards(badges: List<String>) {
        binding.containerAchievements.removeAllViews()
        badges.forEachIndexed { index, badgeId ->
            val cardBinding = ItemAchievementCardBinding.inflate(
                LayoutInflater.from(requireContext()), binding.containerAchievements, false
            )
            cardBinding.textViewAchievementName.text = badgeDisplayName(badgeId)
            cardBinding.textViewAchievementIcon.text = badgeIcon(badgeId)

            cardBinding.root.scaleX = 0f
            cardBinding.root.scaleY = 0f
            binding.containerAchievements.addView(cardBinding.root)

            cardBinding.root.postDelayed({
                SpringAnimation(cardBinding.root, DynamicAnimation.SCALE_X, 1f).apply {
                    spring.stiffness = SpringForce.STIFFNESS_MEDIUM
                    spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                    start()
                }
                SpringAnimation(cardBinding.root, DynamicAnimation.SCALE_Y, 1f).apply {
                    spring.stiffness = SpringForce.STIFFNESS_MEDIUM
                    spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                    start()
                }
            }, index * 150L)
        }
    }

    private fun animateTotalPoints(total: Int) {
        ValueAnimator.ofInt(0, total).apply {
            duration = 600
            addUpdateListener { anim ->
                binding.textViewTotalPoints.text = anim.animatedValue.toString()
            }
            start()
        }
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

    private fun badgeIcon(badgeId: String) = when (badgeId) {
        "first_rep" -> "💪"
        "on_a_roll" -> "🔥"
        "unstoppable" -> "⚡"
        "century" -> "💯"
        "pr_machine" -> "🏆"
        "plan_master" -> "📋"
        "heavy_bench" -> "🏋️"
        "heavy_squat" -> "🦵"
        "heavy_deadlift" -> "⬆️"
        "comeback" -> "🔄"
        "full_house" -> "✅"
        else -> "🏅"
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
