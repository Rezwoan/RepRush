package com.reprush.app.ui.member.progress

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.reprush.app.R
import com.reprush.app.databinding.ItemLiftCardBinding
import java.util.Locale

class LiftCardAdapter : ListAdapter<LiftCardItem, LiftCardAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLiftCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemLiftCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LiftCardItem) {
            binding.textLiftName.text = item.groupLabel

            if (item.exerciseName != item.groupLabel) {
                binding.textLiftExerciseName.visibility = View.VISIBLE
                binding.textLiftExerciseName.text = item.exerciseName
            } else {
                binding.textLiftExerciseName.visibility = View.GONE
            }

            binding.textLiftOneRm.text =
                String.format(Locale.getDefault(), "%.1f kg", item.currentOneRm)

            val delta = item.delta30Day
            if (delta != null) {
                binding.textLiftDelta.visibility = View.VISIBLE
                val sign = if (delta >= 0) "+" else ""
                binding.textLiftDelta.text =
                    String.format(Locale.getDefault(), "%s%.1f kg", sign, delta)
                binding.textLiftDelta.setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        if (delta >= 0) R.color.pr_green else R.color.miss_red
                    )
                )
            } else {
                binding.textLiftDelta.visibility = View.GONE
            }

            setupSparkline(item.recentHistory)
        }

        private fun setupSparkline(history: List<Float>) {
            val chart = binding.chartLiftSparkline
            chart.apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false)
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                xAxis.isEnabled = false
                setViewPortOffsets(0f, 0f, 0f, 0f)
                setNoDataText("")
            }

            if (history.size < 2) return

            val entries = history.mapIndexed { i, v -> Entry(i.toFloat(), v) }
            val primaryColor = ContextCompat.getColor(chart.context, R.color.primary)
            val dataSet = LineDataSet(entries, "").apply {
                color = primaryColor
                lineWidth = 1.5f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = primaryColor
                fillAlpha = 40
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LiftCardItem>() {
            override fun areItemsTheSame(a: LiftCardItem, b: LiftCardItem) =
                a.groupLabel == b.groupLabel
            override fun areContentsTheSame(a: LiftCardItem, b: LiftCardItem) = a == b
        }
    }
}
