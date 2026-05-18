package com.reprush.app.ui.member

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import androidx.core.os.bundleOf
import com.reprush.app.R
import com.reprush.app.databinding.DialogAchievementBinding
import com.reprush.app.databinding.FragmentProfileBinding
import com.reprush.app.ui.member.profile.AchievementAdapter
import com.reprush.app.ui.member.profile.AchievementDisplayItem
import com.reprush.app.ui.member.profile.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var achievementAdapter: AchievementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply { duration = 300L }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply { duration = 300L }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        achievementAdapter = AchievementAdapter { item -> showAchievementDialog(item) }
        binding.recyclerViewAchievements.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.recyclerViewAchievements.adapter = achievementAdapter

        observeViewModel()
        setupNavigation()
        viewModel.loadData()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressProfile.visibility = if (loading) View.VISIBLE else View.GONE
            binding.layoutProfileContent.visibility = if (loading) View.GONE else View.VISIBLE
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.textViewDisplayName.text = user.displayName
                binding.textViewFitnessLevel.text = buildString {
                    append(user.fitnessLevel?.replaceFirstChar { it.uppercase() } ?: "")
                    if (!user.primaryGoal.isNullOrBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(user.primaryGoal)
                    }
                }
                binding.imageViewAvatar.contentDescription =
                    "${user.displayName} profile photo"
                if (!user.photoUrl.isNullOrBlank()) {
                    Glide.with(this)
                        .load(user.photoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(binding.imageViewAvatar)
                }
            }
        }

        viewModel.profileStats.observe(viewLifecycleOwner) { stats ->
            binding.textViewStatWorkouts.text = stats.totalWorkouts.toString()
            binding.textViewStatPRs.text = stats.totalPRs.toString()
            binding.textViewStatStreak.text = stats.currentStreak.toString()
            animatePoints(stats.totalPoints)
        }

        viewModel.achievements.observe(viewLifecycleOwner) { items ->
            achievementAdapter.submitList(items)
        }

        viewModel.activePlan.observe(viewLifecycleOwner) { plan ->
            if (plan != null) {
                binding.cardActivePlan.visibility = View.VISIBLE
                binding.textViewNoPlan.visibility = View.GONE
                binding.textViewActivePlanName.text = plan.planName
                binding.textViewActivePlanMeta.text =
                    "${plan.goal} · ${plan.daysPerWeek} days/week · ${plan.totalWeeks} weeks"
                binding.cardActivePlan.setOnClickListener {
                    findNavController().navigate(
                        R.id.action_profileFragment_to_planDetailFragment,
                        bundleOf("planId" to plan.id)
                    )
                }
            } else {
                binding.cardActivePlan.visibility = View.GONE
                binding.textViewNoPlan.visibility = View.VISIBLE
                binding.cardActivePlan.setOnClickListener(null)
            }
        }

        viewModel.membershipDisplay.observe(viewLifecycleOwner) { display ->
            if (display != null) {
                binding.textViewMembershipPackage.text = display.packageName
                binding.textViewMembershipExpiry.text = "Expires: ${display.endDateStr}"
                val daysLeft = display.daysLeft
                val daysText = when {
                    daysLeft < 0 -> "Expired"
                    daysLeft == 0L -> "Expires today"
                    else -> "$daysLeft days remaining"
                }
                binding.textViewMembershipDaysLeft.text = daysText
                val color = when {
                    daysLeft < 0 -> ContextCompat.getColor(requireContext(), R.color.miss_red)
                    daysLeft <= 3 -> ContextCompat.getColor(requireContext(), R.color.miss_red)
                    daysLeft <= 10 -> ContextCompat.getColor(requireContext(), R.color.amber)
                    else -> ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
                }
                binding.textViewMembershipDaysLeft.setTextColor(color)
            }
        }
    }

    private fun animatePoints(target: Int) {
        ValueAnimator.ofInt(0, target).apply {
            duration = 600
            addUpdateListener { anim ->
                binding.textViewStatPoints.text = (anim.animatedValue as Int).toString()
            }
            start()
        }
    }

    private fun setupNavigation() {
        binding.buttonEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_profileEditFragment)
        }
        binding.buttonSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
        binding.buttonNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_notificationsFragment)
        }
    }

    private fun showAchievementDialog(item: AchievementDisplayItem) {
        val dialogView = DialogAchievementBinding.inflate(layoutInflater)
        dialogView.textViewDialogBadgeIcon.text = item.badge.icon
        dialogView.textViewDialogBadgeName.text = item.badge.name
        dialogView.textViewDialogDescription.text = item.badge.description
        dialogView.textViewDialogCondition.text = "Condition: ${item.badge.unlockCondition}"

        if (item.isUnlocked) {
            dialogView.textViewDialogLockOverlay.visibility = View.GONE
            dialogView.textViewDialogBadgeIcon.alpha = 1.0f
            if (item.unlockedAt != null) {
                val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format(Date(item.unlockedAt))
                dialogView.textViewDialogUnlockDate.text = "Unlocked on $dateStr"
                dialogView.textViewDialogUnlockDate.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.primary)
                )
            }
        } else {
            dialogView.textViewDialogLockOverlay.visibility = View.VISIBLE
            dialogView.textViewDialogBadgeIcon.alpha = 0.3f
            dialogView.textViewDialogUnlockDate.text = "Not yet unlocked"
            dialogView.textViewDialogUnlockDate.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
            )
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView.root)
            .create()

        dialogView.buttonDialogDismiss.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
