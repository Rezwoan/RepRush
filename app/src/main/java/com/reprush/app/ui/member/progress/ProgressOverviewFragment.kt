package com.reprush.app.ui.member.progress

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.reprush.app.R
import com.reprush.app.databinding.FragmentProgressOverviewBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class ProgressOverviewFragment : Fragment() {

    private var _binding: FragmentProgressOverviewBinding? = null
    private val binding get() = _binding!!

    private val heatmapViewModel: HeatmapViewModel by activityViewModels()
    private val overviewViewModel: ProgressOverviewViewModel by viewModels()

    private val historyAdapter = WorkoutHistoryAdapter { sessionId ->
        val bundle = Bundle().apply { putString("sessionId", sessionId) }
        findNavController().navigate(R.id.action_progressFragment_to_sessionDetailFragment, bundle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerWorkoutHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerWorkoutHistory.adapter = historyAdapter

        setupHeatmap()
        observeViewModels()

        heatmapViewModel.load()
        overviewViewModel.load()
    }

    private fun setupHeatmap() {
        val calendar = binding.heatmapCalendarOverview
        val endMonth = YearMonth.now()
        val startMonth = endMonth.minusMonths(5)

        calendar.dayBinder = object : MonthDayBinder<HeatmapDayContainer> {
            override fun create(view: View) = HeatmapDayContainer(view)
            override fun bind(container: HeatmapDayContainer, data: CalendarDay) {
                val intensityMap = heatmapViewModel.heatmapIntensity.value ?: emptyMap()
                val intensity = if (data.position == DayPosition.MonthDate) {
                    intensityMap[data.date] ?: 0f
                } else 0f

                val color = interpolateColor(intensity)
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = resources.getDimension(R.dimen.shape_xs)
                    setColor(color)
                }
                container.cellView.background = drawable
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
            binding.textHeatmapEmptyOverview.visibility =
                if (map.size < 7 || !hasData) View.VISIBLE else View.GONE
        }
    }

    private fun interpolateColor(intensity: Float): Int {
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

    private fun observeViewModels() {
        overviewViewModel.workoutHistory.observe(viewLifecycleOwner) { items ->
            historyAdapter.submitList(items)
            binding.textHistoryEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerWorkoutHistory.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        }

        overviewViewModel.weeklyMuscleVolume.observe(viewLifecycleOwner) { entries ->
            if (entries.isEmpty()) {
                binding.chartMuscleVolume.visibility = View.GONE
                binding.textMuscleEmpty.visibility = View.VISIBLE
            } else {
                binding.chartMuscleVolume.visibility = View.VISIBLE
                binding.textMuscleEmpty.visibility = View.GONE
                setupMuscleChart(entries)
            }
        }
    }

    private fun setupMuscleChart(data: List<MuscleVolumeEntry>) {
        val chart = binding.chartMuscleVolume
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val labelColor = ContextCompat.getColor(requireContext(), R.color.on_surface_variant)

        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawValueAboveBar(false)
            setNoDataText("No data")
            axisRight.isEnabled = false
            axisLeft.apply {
                textColor = labelColor
                gridColor = Color.parseColor("#43474E")
                textSize = 10f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = labelColor
                gridColor = Color.TRANSPARENT
                textSize = 9f
                setDrawGridLines(false)
                valueFormatter = IndexAxisValueFormatter(data.map { it.muscle.take(6) })
                granularity = 1f
                labelCount = data.size
                labelRotationAngle = -30f
            }
            setBackgroundColor(Color.TRANSPARENT)
        }

        val entries = data.mapIndexed { i, e -> BarEntry(i.toFloat(), e.totalVolume.toFloat()) }
        val dataSet = BarDataSet(entries, "").apply {
            color = primaryColor
            setDrawValues(false)
        }
        chart.data = BarData(dataSet).apply { barWidth = 0.5f }
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class HeatmapDayContainer(view: View) : ViewContainer(view) {
        val cellView: View = view.findViewById(R.id.view_heatmap_cell)
    }

    private inner class MonthHeaderContainer(view: View) : ViewContainer(view) {
        val textView = view.findViewById<android.widget.TextView>(R.id.text_month_label)
    }
}
