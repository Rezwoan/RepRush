package com.reprush.app.ui.admin.announcements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.R
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentPostAnnouncementBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PostAnnouncementFragment : Fragment() {

    private var _binding: FragmentPostAnnouncementBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnnouncementViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostAnnouncementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarPostAnnouncement.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbarPostAnnouncement.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_send_announcement) {
                attemptPost()
                true
            } else false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarAnnouncement.visibility = if (loading) View.VISIBLE else View.GONE
            binding.toolbarPostAnnouncement.menu
                .findItem(R.id.action_send_announcement)?.isEnabled = !loading
        }

        viewModel.postResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            viewModel.clearPostResult()
            when (result) {
                is Result.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Announcement sent to ${result.data} members",
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().navigateUp()
                }
                is Result.Error -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun attemptPost() {
        val title = binding.editTextAnnouncementTitle.text.toString().trim()
        val body = binding.editTextAnnouncementBody.text.toString().trim()

        if (title.isEmpty()) {
            binding.textInputLayoutTitle.error = "Title is required"
            return
        }
        binding.textInputLayoutTitle.error = null

        if (body.isEmpty()) {
            binding.textInputLayoutBody.error = "Message is required"
            return
        }
        binding.textInputLayoutBody.error = null

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Send Announcement")
            .setMessage("This will notify all active members. Continue?")
            .setPositiveButton("Send") { _, _ ->
                viewModel.postAnnouncement(title, body)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
