package com.reprush.app.ui.member.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import com.reprush.app.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private var updatingFromViewModel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply { duration = 300L }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply { duration = 300L }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSeekBar()
        observeViewModel()
        setupListeners()

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupSeekBar() {
        // SeekBar max=270 maps to 300s (30 + progress = actual value, step 15)
        // progress 0 = 30s, progress 270 = 300s
        binding.seekBarRestTimer.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val snapped = (progress / 15) * 15
                val seconds = 30 + snapped
                binding.textViewRestTimerValue.text = "${seconds}s"
                if (fromUser && !updatingFromViewModel) {
                    viewModel.setRestTimerDuration(seconds)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val snapped = (seekBar.progress / 15) * 15
                if (seekBar.progress != snapped) seekBar.progress = snapped
            }
        })
    }

    private fun observeViewModel() {
        viewModel.restTimerDuration.observe(viewLifecycleOwner) { seconds ->
            updatingFromViewModel = true
            val progress = (seconds - 30).coerceIn(0, 270)
            binding.seekBarRestTimer.progress = progress
            binding.textViewRestTimerValue.text = "${seconds}s"
            updatingFromViewModel = false
        }

        viewModel.autoTimerEnabled.observe(viewLifecycleOwner) { enabled ->
            updatingFromViewModel = true
            binding.switchAutoTimer.isChecked = enabled
            updatingFromViewModel = false
        }

        viewModel.weightUnit.observe(viewLifecycleOwner) { unit ->
            updatingFromViewModel = true
            if (unit == "lbs") {
                binding.radioButtonLbs.isChecked = true
            } else {
                binding.radioButtonKg.isChecked = true
            }
            updatingFromViewModel = false
        }

        viewModel.leaderboardOptIn.observe(viewLifecycleOwner) { optIn ->
            updatingFromViewModel = true
            binding.switchLeaderboardOptIn.isChecked = optIn
            updatingFromViewModel = false
        }
    }

    private fun setupListeners() {
        binding.switchAutoTimer.setOnCheckedChangeListener { _, isChecked ->
            if (!updatingFromViewModel) viewModel.setAutoTimerEnabled(isChecked)
        }

        binding.radioGroupWeightUnit.setOnCheckedChangeListener { _, checkedId ->
            if (!updatingFromViewModel) {
                val unit = if (checkedId == binding.radioButtonLbs.id) "lbs" else "kg"
                viewModel.setWeightUnit(unit)
            }
        }

        binding.switchLeaderboardOptIn.setOnCheckedChangeListener { _, isChecked ->
            if (!updatingFromViewModel) viewModel.setLeaderboardOptIn(isChecked)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
