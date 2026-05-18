package com.reprush.app.ui.member.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.reprush.app.R
import com.reprush.app.databinding.FragmentNotificationsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationsViewModel by activityViewModels()
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        adapter = NotificationAdapter { notification ->
            if (findNavController().currentDestination?.id != R.id.notificationsFragment) return@NotificationAdapter
            if (!notification.isRead) viewModel.markAsRead(notification.id)
            findNavController().navigate(
                R.id.action_notificationsFragment_to_notificationDetailFragment,
                bundleOf(
                    "title" to notification.title,
                    "body" to notification.body,
                    "createdAt" to notification.createdAt
                )
            )
        }

        binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotifications.adapter = adapter

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarNotifications.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.notifications.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                binding.layoutEmptyNotifications.visibility = View.VISIBLE
                binding.recyclerViewNotifications.visibility = View.GONE
            } else {
                binding.layoutEmptyNotifications.visibility = View.GONE
                binding.recyclerViewNotifications.visibility = View.VISIBLE
                adapter.submitList(list)
            }
        }

        viewModel.loadNotifications()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
