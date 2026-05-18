package com.reprush.app.ui.member.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.R
import com.reprush.app.databinding.FragmentFinishWorkoutBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FinishWorkoutFragment : Fragment() {

    private var _binding: FragmentFinishWorkoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinishWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val durationMs = viewModel.getDurationMs()
        val totalSecs = durationMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        binding.textViewSummaryDuration.text = "%02d:%02d".format(mins, secs)

        binding.textViewSummarySetCount.text = viewModel.getWorkingSetCount().toString()

        val volume = viewModel.getTotalVolumeKg()
        binding.textViewSummaryVolume.text = "%.1f kg".format(volume)

        viewModel.sessionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SessionState.Saving -> {
                    binding.buttonConfirmFinish.isEnabled = false
                    binding.buttonConfirmFinish.text = "Saving..."
                }
                is SessionState.Saved -> {
                    val bundle = Bundle().apply { putString("sessionId", state.sessionId) }
                    findNavController().navigate(
                        R.id.action_finishWorkoutFragment_to_postWorkoutFragment,
                        bundle
                    )
                }
                is SessionState.Error -> {
                    binding.buttonConfirmFinish.isEnabled = true
                    binding.buttonConfirmFinish.text = "Confirm Finish"
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        binding.buttonConfirmFinish.setOnClickListener {
            viewModel.finishWorkout()
        }

        binding.buttonKeepTraining.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
