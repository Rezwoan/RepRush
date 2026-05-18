package com.reprush.app.ui.member.exercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.reprush.app.R
import com.reprush.app.databinding.FragmentLibrarySyncBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LibrarySyncFragment : Fragment() {

    private var _binding: FragmentLibrarySyncBinding? = null
    private val binding get() = _binding!!
    private val syncViewModel: ExerciseSyncViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLibrarySyncBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        syncViewModel.syncState.observe(viewLifecycleOwner) { state ->
            when (state) {
                SyncState.IDLE -> syncViewModel.startSync()
                SyncState.SYNCING -> {
                    binding.textViewSyncStatus.text = "Downloading exercises..."
                    binding.buttonRetrySync.visibility = View.GONE
                    binding.textViewSyncError.visibility = View.GONE
                    binding.progressBarSync.isIndeterminate = false
                }
                SyncState.DONE -> {
                    findNavController().navigate(R.id.action_librarySyncFragment_to_homeFragment)
                }
                SyncState.ERROR -> {
                    binding.textViewSyncStatus.text = "Could not load exercise library."
                    binding.textViewSyncError.visibility = View.VISIBLE
                    binding.buttonRetrySync.visibility = View.VISIBLE
                    binding.progressBarSync.visibility = View.GONE
                }
                null -> {}
            }
        }

        syncViewModel.syncProgress.observe(viewLifecycleOwner) { progress ->
            if (progress.totalCount > 0) {
                binding.progressBarSync.max = progress.totalCount
                binding.progressBarSync.progress = progress.currentCount
                binding.textViewSyncCount.text = "Downloading images: ${progress.currentCount} / ${progress.totalCount}"
            } else {
                binding.textViewSyncCount.text = "Preparing exercise library..."
            }
        }

        syncViewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (msg != null) binding.textViewSyncError.text = msg
        }

        binding.buttonRetrySync.setOnClickListener { syncViewModel.startSync() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
