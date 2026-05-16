package com.reprush.app.ui.member.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.reprush.app.R
import com.reprush.app.databinding.FragmentTimerSheetBinding
import com.reprush.app.service.RestTimerService

class RestTimerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentTimerSheetBinding? = null
    private val binding get() = _binding!!

    private var totalDuration = 0
    private var broadcastReceiver: BroadcastReceiver? = null

    companion object {
        const val ARG_EXERCISE_NAME = "exercise_name"
        const val ARG_DURATION = "duration"

        fun newInstance(exerciseName: String, durationSeconds: Int): RestTimerBottomSheet {
            return RestTimerBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_EXERCISE_NAME, exerciseName)
                    putInt(ARG_DURATION, durationSeconds)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Prevent swipe-down dismissal and back-button dismissal
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            isHideable = false
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)

        val exerciseName = arguments?.getString(ARG_EXERCISE_NAME) ?: ""
        val durationSeconds = arguments?.getInt(ARG_DURATION) ?: 90
        totalDuration = durationSeconds

        binding.textViewRestExerciseName.text = exerciseName
        binding.progressBarRestTimer.max = durationSeconds
        binding.progressBarRestTimer.progress = durationSeconds
        updateCountdown(durationSeconds)

        // Start the foreground service
        val serviceIntent = Intent(requireContext(), RestTimerService::class.java).apply {
            putExtra(RestTimerService.EXTRA_EXERCISE_NAME, exerciseName)
            putExtra(RestTimerService.EXTRA_DURATION, durationSeconds)
        }
        requireContext().startService(serviceIntent)

        // Register receiver for timer ticks
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    RestTimerService.ACTION_TICK -> {
                        val remaining = intent.getIntExtra(RestTimerService.EXTRA_REMAINING, 0)
                        updateCountdown(remaining)
                        binding.progressBarRestTimer.progress = remaining
                    }
                    RestTimerService.ACTION_FINISH -> {
                        if (isAdded) dismiss()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(RestTimerService.ACTION_TICK)
            addAction(RestTimerService.ACTION_FINISH)
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(broadcastReceiver!!, filter)

        binding.buttonAddThirtySeconds.setOnClickListener {
            val addIntent = Intent(requireContext(), RestTimerService::class.java).apply {
                action = RestTimerService.ACTION_ADD_THIRTY
            }
            requireContext().startService(addIntent)
        }

        binding.buttonSkipRest.setOnClickListener {
            stopTimerService()
            dismiss()
        }
    }

    private fun updateCountdown(remainingSeconds: Int) {
        val mins = remainingSeconds / 60
        val secs = remainingSeconds % 60
        binding.textViewRestCountdown.text = "%02d:%02d".format(mins, secs)
    }

    private fun stopTimerService() {
        val stopIntent = Intent(requireContext(), RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_SKIP
        }
        requireContext().startService(stopIntent)
    }

    override fun onDestroyView() {
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(it)
        }
        super.onDestroyView()
        _binding = null
    }
}
