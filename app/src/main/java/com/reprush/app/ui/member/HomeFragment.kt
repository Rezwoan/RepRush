package com.reprush.app.ui.member

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.reprush.app.R
import com.reprush.app.data.local.dao.PlanDayDao
import com.reprush.app.data.local.dao.WorkoutPlanDao
import com.reprush.app.data.local.datastore.AppPreferences
import com.reprush.app.data.repository.SessionRepository
import com.reprush.app.databinding.FragmentHomeBinding
import com.reprush.app.ui.member.home.HomeViewModel
import com.reprush.app.ui.member.home.MembershipDisplay
import com.reprush.app.ui.member.home.PrDisplay
import com.reprush.app.ui.member.progress.HeatmapDayData
import com.reprush.app.ui.member.progress.HeatmapViewModel
import com.reprush.app.ui.member.session.SessionState
import com.reprush.app.ui.member.session.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sessionViewModel: SessionViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val heatmapViewModel: HeatmapViewModel by activityViewModels()

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var workoutPlanDao: WorkoutPlanDao
    @Inject lateinit var planDayDao: PlanDayDao

    private var todayPlanDayId: String? = null
    private var todayDayLabel: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            val synced = appPreferences.isLibrarySynced.first()
            if (!isAdded || findNavController().currentDestination?.id != R.id.homeFragment) return@launch
            if (!synced) {
                findNavController().navigate(R.id.action_homeFragment_to_librarySyncFragment)
                return@launch
            }
            val onboarded = appPreferences.isMemberOnboardingComplete.first()
            if (!isAdded || findNavController().currentDestination?.id != R.id.homeFragment) return@launch
            if (!onboarded) {
                findNavController().navigate(R.id.action_homeFragment_to_memberOnboardingFragment)
                return@launch
            }
            checkForIncompleteSession()
            loadTodayPlanDay()
        }

        binding.buttonStartWorkout.setOnClickListener {
            val bundle = Bundle().apply {
                putString("planDayId", todayPlanDayId ?: "")
                putString("dayLabel", todayDayLabel)
            }
            findNavController().navigate(R.id.action_homeFragment_to_activeSessionFragment, bundle)
        }

        binding.buttonViewLeaderboard.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_leaderboardFragment)
        }

        binding.textViewMemberGreeting.text = getGreeting()
        homeViewModel.userName.observe(viewLifecycleOwner) { name ->
            if (name.isNotBlank()) {
                val firstName = name.split(" ").firstOrNull() ?: name
                binding.textViewMemberGreeting.text = "${getGreeting().removeSuffix("!")}, $firstName!"
            } else {
                binding.textViewMemberGreeting.text = getGreeting()
            }
        }

        homeViewModel.userPhotoUrl.observe(viewLifecycleOwner) { url ->
            Glide.with(this)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .into(binding.imageViewMemberAvatar)
        }

        binding.imageViewMemberAvatar.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }

        observeGamification()
        homeViewModel.loadData()
        setupHeatmap()
        heatmapViewModel.load()
    }

    // ── Heatmap ──────────────────────────────────────────────────────────────

    private fun setupHeatmap() {
        val calendar = binding.heatmapCalendar
        val endMonth = YearMonth.now()
        val startMonth = endMonth.minusMonths(5)

        calendar.dayBinder = object : MonthDayBinder<HeatmapDayContainer> {
            override fun create(view: View) = HeatmapDayContainer(view)
            override fun bind(container: HeatmapDayContainer, data: CalendarDay) {
                val intensityMap = heatmapViewModel.heatmapIntensity.value ?: emptyMap()
                val detailMap: Map<LocalDate, HeatmapDayData> = heatmapViewModel.heatmapDetail.value ?: emptyMap()
                val date = data.date
                val intensity = if (data.position == DayPosition.MonthDate) {
                    intensityMap[date] ?: 0f
                } else 0f

                val color = interpolateHeatmapColor(intensity)
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = resources.getDimension(R.dimen.shape_xs)
                    setColor(color)
                }
                container.cellView.background = drawable

                if (data.position == DayPosition.MonthDate) {
                    container.dayText.text = date.dayOfMonth.toString()
                    container.dayText.visibility = View.VISIBLE
                } else {
                    container.dayText.visibility = View.INVISIBLE
                }

                val label = buildCellContentDescription(date, intensityMap, detailMap)
                container.cellView.contentDescription = label

                if (data.position == DayPosition.MonthDate) {
                    container.cellView.setOnClickListener { anchor ->
                        showTooltip(anchor, date, intensityMap, detailMap)
                    }
                } else {
                    container.cellView.setOnClickListener(null)
                }
            }
        }

        calendar.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthHeaderContainer> {
            override fun create(view: View) = MonthHeaderContainer(view)
            override fun bind(container: MonthHeaderContainer, data: CalendarMonth) {
                val fmt = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
                container.textView.text = data.yearMonth.format(fmt)
            }
        }

        calendar.setup(startMonth, endMonth, DayOfWeek.MONDAY)
        calendar.scrollToMonth(endMonth)

        heatmapViewModel.heatmapIntensity.observe(viewLifecycleOwner) { map ->
            calendar.notifyCalendarChanged()
            val hasData = map.values.any { it > 0f }
            binding.textHeatmapEmpty.visibility = if (map.size < 7 || !hasData) View.VISIBLE else View.GONE
        }
    }

    private fun interpolateHeatmapColor(intensity: Float): Int {
        val surfaceVariant = ContextCompat.getColor(requireContext(), R.color.surface_variant)
        val primaryContainer = ContextCompat.getColor(requireContext(), R.color.primary_container)
        val primary = ContextCompat.getColor(requireContext(), R.color.primary)
        return when {
            intensity <= 0f -> surfaceVariant
            intensity < 0.5f -> blendColors(surfaceVariant, primaryContainer, intensity * 2f)
            else -> blendColors(primaryContainer, primary, (intensity - 0.5f) * 2f)
        }
    }

    private fun blendColors(start: Int, end: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val aS = (start ushr 24) and 0xff
        val rS = (start ushr 16) and 0xff
        val gS = (start ushr 8) and 0xff
        val bS = start and 0xff
        val aE = (end ushr 24) and 0xff
        val rE = (end ushr 16) and 0xff
        val gE = (end ushr 8) and 0xff
        val bE = end and 0xff
        return Color.argb(
            (aS + (aE - aS) * f).toInt(),
            (rS + (rE - rS) * f).toInt(),
            (gS + (gE - gS) * f).toInt(),
            (bS + (bE - bS) * f).toInt()
        )
    }

    private fun buildCellContentDescription(
        date: LocalDate,
        intensityMap: Map<LocalDate, Float>,
        detailMap: Map<LocalDate, HeatmapDayData>
    ): String {
        val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        val dateStr = date.format(dateFmt)
        val detail = detailMap[date]
        return if (detail == null || detail.intensity <= 0f) {
            "Date $dateStr. No workout recorded."
        } else {
            val volumeKg = String.format(Locale.getDefault(), "%.1f", detail.volumeKg)
            "Date $dateStr. Volume: ${volumeKg}kg. ${detail.exerciseCount} exercises."
        }
    }

    private fun showTooltip(
        anchor: View,
        date: LocalDate,
        intensityMap: Map<LocalDate, Float>,
        detailMap: Map<LocalDate, HeatmapDayData>
    ) {
        val inflater = LayoutInflater.from(requireContext())
        val tooltipView = inflater.inflate(R.layout.popup_heatmap_tooltip, null)

        val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        val dateStr = date.format(dateFmt)
        val detail = detailMap[date]

        tooltipView.findViewById<TextView>(R.id.text_tooltip_date).text = dateStr

        if (detail == null || detail.intensity <= 0f) {
            tooltipView.findViewById<TextView>(R.id.text_tooltip_volume).text = "No workout recorded"
            tooltipView.findViewById<TextView>(R.id.text_tooltip_exercises).visibility = View.GONE
        } else {
            tooltipView.findViewById<TextView>(R.id.text_tooltip_volume).text =
                "Volume: ${String.format(Locale.getDefault(), "%.0f", detail.volumeKg)} kg"
            tooltipView.findViewById<TextView>(R.id.text_tooltip_exercises).text =
                "${detail.exerciseCount} exercise${if (detail.exerciseCount != 1) "s" else ""}"
        }

        val popup = PopupWindow(
            tooltipView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        popup.elevation = 8f
        popup.isOutsideTouchable = true
        popup.setWindowLayoutMode(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        tooltipView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        popup.showAsDropDown(anchor, 0, -anchor.height - tooltipView.measuredHeight)
    }

    // ── Gamification ─────────────────────────────────────────────────────────

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

        homeViewModel.membershipDisplay.observe(viewLifecycleOwner) { display ->
            if (display == null) {
                binding.cardMembership.visibility = View.GONE
            } else {
                bindMembershipCard(display)
            }
        }

        homeViewModel.monthlyPoints.observe(viewLifecycleOwner) { points ->
            binding.textViewHomeMonthlyPoints.text = if (points > 0) points.toString() else "—"
        }

        homeViewModel.userRank.observe(viewLifecycleOwner) { rank ->
            if (rank != null && rank > 0) {
                binding.textViewHomeUserRank.visibility = View.VISIBLE
                binding.textViewHomeUserRank.text = "Rank #$rank"
            } else {
                binding.textViewHomeUserRank.visibility = View.GONE
            }
        }
    }

    private fun bindRecentPRs(prs: List<PrDisplay>) {
        binding.containerRecentPRs.removeAllViews()
        for (pr in prs) {
            val row = TextView(requireContext()).apply {
                text = "${pr.exerciseName}  ${pr.weight}kg × ${pr.repCount}"
                textSize = 13f
                setTextColor(requireContext().getColor(R.color.on_surface))
                setPadding(0, 4, 0, 4)
            }
            binding.containerRecentPRs.addView(row)
        }
    }

    private fun bindMembershipCard(display: MembershipDisplay) {
        binding.cardMembership.visibility = View.VISIBLE
        binding.textViewMembershipPackage.text = display.packageName

        val daysLeft = display.daysLeft
        binding.textViewMembershipExpiry.text = "Expires: ${display.endDateStr}"
        binding.textViewMembershipDaysLeft.text = when {
            daysLeft < 0 -> "Expired"
            daysLeft == 0L -> "Expires today"
            else -> "$daysLeft days left"
        }

        val cardColor = when {
            daysLeft < 0 -> requireContext().getColor(R.color.miss_red)
            daysLeft <= 3 -> requireContext().getColor(R.color.miss_red)
            daysLeft <= 10 -> requireContext().getColor(R.color.amber)
            else -> requireContext().getColor(R.color.surface)
        }
        binding.cardMembership.setCardBackgroundColor(cardColor)

        val daysTextColor = when {
            daysLeft < 0 -> requireContext().getColor(R.color.error)
            daysLeft <= 3 -> requireContext().getColor(R.color.error)
            daysLeft <= 10 -> requireContext().getColor(R.color.amber)
            else -> requireContext().getColor(R.color.pr_green)
        }
        binding.textViewMembershipDaysLeft.setTextColor(daysTextColor)
    }

    // ── Session recovery ─────────────────────────────────────────────────────

    private fun checkForIncompleteSession() {
        if (!isAdded || view == null) return
        val userId = auth.currentUser?.uid ?: return
        val alreadyActive = sessionViewModel.sessionState.value is SessionState.Active
        if (alreadyActive) return

        viewLifecycleOwner.lifecycleScope.launch {
            val incompleteSession = sessionRepository.getIncompleteSession(userId)
            if (incompleteSession != null && isAdded && view != null) {
                val startDateStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                    .format(Date(incompleteSession.startTime))
                AlertDialog.Builder(requireContext())
                    .setTitle("Unfinished Session")
                    .setMessage(
                        "You have an unfinished session from $startDateStr. " +
                        "Would you like to resume or discard it?"
                    )
                    .setPositiveButton("Resume") { _, _ ->
                        if (!isAdded || view == null) return@setPositiveButton
                        viewLifecycleOwner.lifecycleScope.launch {
                            val recoveredState = sessionRepository.recoverSession(incompleteSession)
                            sessionViewModel.resumeSession(recoveredState)
                            if (isAdded) {
                                findNavController().navigate(R.id.action_homeFragment_to_activeSessionFragment)
                            }
                        }
                    }
                    .setNegativeButton("Discard") { _, _ ->
                        if (!isAdded || view == null) return@setNegativeButton
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
        todayDayLabel = todayDay.dayLabel
        if (isAdded) binding.textViewTodayDayLabel.text = todayDay.dayLabel
    }

    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning!"
            hour < 17 -> "Good afternoon!"
            hour < 21 -> "Good evening!"
            else -> "Good night!"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── ViewContainer helpers ─────────────────────────────────────────────────

    private inner class HeatmapDayContainer(view: View) : ViewContainer(view) {
        val cellView: View = view.findViewById(R.id.view_heatmap_cell)
        val dayText: TextView = view.findViewById(R.id.text_heatmap_day)
    }

    private inner class MonthHeaderContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.text_month_label)
    }
}
