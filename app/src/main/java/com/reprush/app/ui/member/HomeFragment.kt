package com.reprush.app.ui.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.R
import com.reprush.app.data.local.dao.PlanDayDao
import com.reprush.app.data.local.dao.WorkoutPlanDao
import com.reprush.app.data.local.datastore.AppPreferences
import com.reprush.app.data.repository.SessionRepository
import com.reprush.app.databinding.FragmentHomeBinding
import com.reprush.app.ui.member.session.SessionState
import com.reprush.app.ui.member.session.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sessionViewModel: SessionViewModel by activityViewModels()

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
