package com.reprush.app.ui.member.progress

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.reprush.app.R
import com.reprush.app.databinding.FragmentStrengthBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class StrengthFragment : Fragment() {

    private var _binding: FragmentStrengthBinding? = null
    private val binding get() = _binding!!

    private val strengthViewModel: StrengthViewModel by viewModels()
    private val liftAdapter = LiftCardAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStrengthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerLiftCards.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLiftCards.adapter = liftAdapter

        setupVolumeChart()
        observeViewModels()

        strengthViewModel.load()
    }

    private fun setupVolumeChart() {
        val labelColor = ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
        binding.chartWeeklyVolume.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setNoDataText("No workout data yet.")
            setNoDataTextColor(labelColor)
            axisRight.isEnabled = false
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
        strengthViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressStrength.visibility = if (loading) View.VISIBLE else View.GONE
            binding.layoutStrengthContent.visibility = if (loading) View.GONE else View.VISIBLE
        }

        strengthViewModel.strengthData.observe(viewLifecycleOwner) { data ->
            val insufficient = data.liftCount < 2
            binding.textStrengthInsufficient.visibility = if (insufficient) View.VISIBLE else View.GONE
            binding.cardStrengthScore.visibility = if (insufficient) View.GONE else View.VISIBLE
            binding.recyclerLiftCards.visibility = if (insufficient) View.GONE else View.VISIBLE

            if (!insufficient) {
                binding.chipStrengthLevel.text = data.level
                liftAdapter.submitList(data.liftCards)
                animateStrengthScore(data.score)
            }

            val weeklyVol = data.weeklyVolume
            if (weeklyVol.isNotEmpty()) {
                val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
                val entries = weeklyVol.mapIndexed { i, w ->
                    BarEntry(i.toFloat(), w.totalVolume.toFloat())
                }
                val dataSet = BarDataSet(entries, "").apply {
                    color = primaryColor
                    setDrawValues(false)
                }
                binding.chartWeeklyVolume.data = BarData(dataSet).apply { barWidth = 0.6f }
                binding.chartWeeklyVolume.xAxis.labelCount = minOf(weeklyVol.size, 6)
                binding.chartWeeklyVolume.animateY(600)
                binding.chartWeeklyVolume.invalidate()
            }
        }
    }

    private fun animateStrengthScore(target: Double) {
        ValueAnimator.ofFloat(0f, target.toFloat()).apply {
            duration = 600
            addUpdateListener { anim ->
                binding.textStrengthScore.text =
                    String.format(Locale.getDefault(), "%.0f", anim.animatedValue as Float)
            }
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
