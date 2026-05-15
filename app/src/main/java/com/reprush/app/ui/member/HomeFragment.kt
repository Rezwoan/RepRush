package com.reprush.app.ui.member

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.R
import com.reprush.app.data.local.dao.PlanDayDao
import com.reprush.app.data.local.dao.WorkoutPlanDao
import com.reprush.app.data.local.datastore.AppPreferences
import com.reprush.app.data.local.entity.PrRecordEntity
import com.reprush.app.data.local.entity.UserEntity
import com.reprush.app.data.repository.SessionRepository
import com.reprush.app.databinding.FragmentHomeBinding
import com.reprush.app.ui.member.home.HomeViewModel
import com.reprush.app.ui.member.session.SessionState
import com.reprush.app.ui.member.session.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sessionViewModel: SessionViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var workoutPlanDao: WorkoutPlanDao
    @Inject lateinit var planDayDao: PlanDayDao

    private var todayPlanDayId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            val synced = appPreferences.isLibrarySynced.first()
            if (!synced && isAdded) {
                findNavController().navigate(R.id.action_homeFragment_to_librarySyncFragment)
                return@launch
            }
            checkForIncompleteSession()
            loadTodayPlanDay()
        }

        binding.buttonStartWorkout.setOnClickListener {
            val bundle = Bundle().apply { putString("planDayId", todayPlanDayId ?: "") }
            findNavController().navigate(R.id.action_homeFragment_to_activeSessionFragment, bundle)
        }

        binding.buttonViewLeaderboard.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_leaderboardFragment)
        }

        observeGamification()
        homeViewModel.loadData()
    }

    private fun observeGamification() {
        homeViewModel.streak.observe(viewLifecycleOwner) { streak ->
            val count = streak?.currentStreak ?: 0
            if (count > 0) {
                binding.rowHomeStreak.visibility = View.VISIBLE
                binding.textViewHomeStreakCount.text = count.toString()
            } else {
                binding.rowHomeStreak.visibility = View.GONE
            }
        }

        homeViewModel.recentPRs.observe(viewLifecycleOwner) { prs ->
            if (prs.isEmpty()) {
                binding.sectionRecentPRs.visibility = View.GONE
            } else {
                binding.sectionRecentPRs.visibility = View.VISIBLE
                bindRecentPRs(prs)
            }
        }

        homeViewModel.membershipInfo.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            bindMembershipCard(user)
        }
    }

    private fun bindRecentPRs(prs: List<PrRecordEntity>) {
        binding.containerRecentPRs.removeAllViews()
        for (pr in prs) {
            val row = TextView(requireContext()).apply {
                text = "${pr.exerciseId}  ${pr.repCount}×${pr.weight}kg"
                textSize = 13f
                setTextColor(requireContext().getColor(R.color.on_surface))
                setPadding(0, 4, 0, 4)
            }
            binding.containerRecentPRs.addView(row)
        }
    }

    private fun bindMembershipCard(user: UserEntity) {
        val endDateStr = user.membershipEndDate ?: return
        binding.cardMembership.visibility = View.VISIBLE

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val endDate = try { sdf.parse(endDateStr) } catch (_: Exception) { null } ?: return
        val now = Date()
        val daysLeft = TimeUnit.MILLISECONDS.toDays(endDate.time - now.time)

        binding.textViewMembershipExpiry.text = "Expires: $endDateStr"
        binding.textViewMembershipDaysLeft.text = when {
            daysLeft < 0 -> "Expired"
            daysLeft == 0L -> "Expires today"
            else -> "$daysLeft days left"
        }

        val cardColor = when {
            daysLeft < 0 -> requireContext().getColor(R.color.miss_red)
            daysLeft <= 3 -> requireContext().getColor(R.color.miss_red)
            daysLeft <= 10 -> requireContext().getColor(R.color.amber)
            else -> Color.TRANSPARENT
        }
        binding.cardMembership.setCardBackgroundColor(
            if (cardColor == Color.TRANSPARENT)
                requireContext().getColor(R.color.surface)
            else cardColor
        )

        val daysTextColor = when {
            daysLeft < 0 -> requireContext().getColor(R.color.error)
            daysLeft <= 3 -> requireContext().getColor(R.color.error)
            daysLeft <= 10 -> requireContext().getColor(R.color.amber)
            else -> requireContext().getColor(R.color.pr_green)
        }
        binding.textViewMembershipDaysLeft.setTextColor(daysTextColor)
    }

    private fun checkForIncompleteSession() {
        val userId = auth.currentUser?.uid ?: return
        val alreadyActive = sessionViewModel.sessionState.value is SessionState.Active
        if (alreadyActive) return

        viewLifecycleOwner.lifecycleScope.launch {
            val incompleteSession = sessionRepository.getIncompleteSession(userId)
            if (incompleteSession != null && isAdded) {
                val startDateStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                    .format(Date(incompleteSession.startTime))
                AlertDialog.Builder(requireContext())
                    .setTitle("Unfinished Session")
                    .setMessage(
                        "You have an unfinished session from $startDateStr. " +
                        "Would you like to resume or discard it?"
                    )
                    .setPositiveButton("Resume") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val recoveredState = sessionRepository.recoverSession(incompleteSession)
                            sessionViewModel.resumeSession(recoveredState)
                            if (isAdded) {
                                findNavController().navigate(R.id.action_homeFragment_to_activeSessionFragment)
                            }
                        }
                    }
                    .setNegativeButton("Discard") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            sessionRepository.discardIncompleteSession(incompleteSession.id)
                        }
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private suspend fun loadTodayPlanDay() {
        val userId = auth.currentUser?.uid ?: return
        val plan = workoutPlanDao.getActivePlan(userId) ?: return
        val planDays = planDayDao.getDaysForPlan(plan.id)
        if (planDays.isEmpty()) return
        val daysSinceStart = ((System.currentTimeMillis() - plan.createdAt) / 86400000L).toInt()
        val todayIndex = daysSinceStart % plan.daysPerWeek
        val todayDay = planDays.getOrNull(todayIndex) ?: planDays.first()
        todayPlanDayId = todayDay.id
        if (isAdded) binding.textViewTodayDayLabel.text = todayDay.dayLabel
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
