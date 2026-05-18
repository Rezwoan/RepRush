package com.reprush.app.ui.member.progress

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.R
import com.reprush.app.databinding.FragmentBodyBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class BodyFragment : Fragment() {

    private var _binding: FragmentBodyBinding? = null
    private val binding get() = _binding!!

    private val bodyViewModel: BodyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBodyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChart()
        observeViewModels()

        binding.buttonLogWeight.setOnClickListener {
            val input = binding.editWeightInput.text?.toString()?.trim()
            val value = input?.toDoubleOrNull()
            if (value == null || value <= 0) {
                Snackbar.make(binding.root, "Enter a valid weight in kg", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            bodyViewModel.logWeight(value)
            binding.editWeightInput.text?.clear()
        }

        bodyViewModel.load()
    }

    private fun setupChart() {
        val labelColor = ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
        binding.chartBodyWeight.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(false)
            axisRight.isEnabled = false
            setNoDataText("No weight entries yet.")
            setNoDataTextColor(labelColor)
            axisLeft.apply {
                textColor = labelColor
                gridColor = Color.parseColor("#43474E")
                textSize = 10f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = labelColor
                setDrawGridLines(false)
                granularity = 1f
            }
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun observeViewModels() {
        bodyViewModel.weightEntries.observe(viewLifecycleOwner) { entries ->
            val showChart = entries.size >= 3
            binding.chartBodyWeight.visibility = if (showChart) View.VISIBLE else View.GONE
            binding.textWeightInsufficient.visibility = if (showChart) View.GONE else View.VISIBLE

            if (showChart) {
                drawWeightChart(entries)
            }
        }

        bodyViewModel.bodyStats.observe(viewLifecycleOwner) { stats ->
            fun fmt(v: Double?) = if (v != null) String.format(Locale.getDefault(), "%.1f kg", v) else "—"
            fun fmtDelta(v: Double?) = if (v != null) {
                val sign = if (v >= 0) "+" else ""
                "$sign${String.format(Locale.getDefault(), "%.1f kg", v)}"
            } else "—"

            binding.textStatStarting.text = fmt(stats.startingWeight)
            binding.textStatCurrent.text = fmt(stats.currentWeight)
            binding.textStatMonthly.text = fmtDelta(stats.monthlyChange)
            binding.textStatAlltime.text = fmtDelta(stats.allTimeChange)
            binding.textStatLowest.text = fmt(stats.lowestWeight)

            val monthlyColor = when {
                stats.monthlyChange == null -> ContextCompat.getColor(requireContext(), R.color.on_surface)
                stats.monthlyChange < 0 -> ContextCompat.getColor(requireContext(), R.color.pr_green)
                stats.monthlyChange > 0 -> ContextCompat.getColor(requireContext(), R.color.miss_red)
                else -> ContextCompat.getColor(requireContext(), R.color.on_surface)
            }
            binding.textStatMonthly.setTextColor(monthlyColor)
        }

        bodyViewModel.logSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Snackbar.make(binding.root, "Weight logged", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun drawWeightChart(entries: List<WeightEntry>) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val secondaryColor = ContextCompat.getColor(requireContext(), R.color.secondary)

        val rawEntries = entries.mapIndexed { i, e -> Entry(i.toFloat(), e.weightKg.toFloat()) }
        val rawSet = LineDataSet(rawEntries, "Weight").apply {
            color = primaryColor
            lineWidth = 2f
            setDrawCircles(true)
            circleRadius = 3f
            setCircleColor(primaryColor)
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }

        val avgEntries = bodyViewModel.rollingAverage.value?.mapIndexed { i, e ->
            Entry(i.toFloat(), e.weightKg.toFloat())
        } ?: emptyList()

        val sets = if (avgEntries.size >= 3) {
            val avgSet = LineDataSet(avgEntries, "7-day avg").apply {
                color = secondaryColor
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                enableDashedLine(10f, 5f, 0f)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            listOf(rawSet, avgSet)
        } else listOf(rawSet)

        binding.chartBodyWeight.data = LineData(sets)
        binding.chartBodyWeight.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
