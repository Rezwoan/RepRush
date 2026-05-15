package com.reprush.app.ui.admin.announcements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.R
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentAnnouncementListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnnouncementListFragment : Fragment() {

    private var _binding: FragmentAnnouncementListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnnouncementViewModel by viewModels()
    private lateinit var adapter: AnnouncementAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnnouncementListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarAnnouncementList.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = AnnouncementAdapter { announcement ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Announcement")
                .setMessage("Delete \"${announcement.title}\"?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteAnnouncement(announcement.id)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.recyclerViewAnnouncements.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAnnouncements.adapter = adapter

        binding.fabNewAnnouncement.setOnClickListener {
            findNavController().navigate(R.id.action_announcementListFragment_to_postAnnouncementFragment)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarAnnouncementList.visibility =
                if (loading) View.VISIBLE else View.GONE
        }

        viewModel.announcements.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                binding.layoutEmptyAnnouncements.visibility = View.VISIBLE
                binding.recyclerViewAnnouncements.visibility = View.GONE
            } else {
                binding.layoutEmptyAnnouncements.visibility = View.GONE
                binding.recyclerViewAnnouncements.visibility = View.VISIBLE
                adapter.submitList(list)
            }
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            viewModel.clearDeleteResult()
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Announcement deleted", Snackbar.LENGTH_SHORT).show()
                    viewModel.loadAnnouncements()
                }
                is Result.Error -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.loadAnnouncements()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
