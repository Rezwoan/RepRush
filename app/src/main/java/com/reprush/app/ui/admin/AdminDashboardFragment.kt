package com.reprush.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.reprush.app.R
import com.reprush.app.databinding.FragmentAdminDashboardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardPendingMembers.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_pendingMembersFragment)
        }

        binding.cardMemberDirectory.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_memberDirectoryFragment)
        }

        binding.cardPackages.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_packageListFragment)
        }

        binding.cardAnnouncements.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_announcementListFragment)
        }

        binding.cardRegisterMember.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_manualRegisterFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
