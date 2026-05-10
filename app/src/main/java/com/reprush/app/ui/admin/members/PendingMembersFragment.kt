package com.reprush.app.ui.admin.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.databinding.FragmentPendingMembersBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PendingMembersFragment : Fragment() {

    private var _binding: FragmentPendingMembersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PendingMembersViewModel by activityViewModels()
    private lateinit var adapter: PendingMembersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPendingMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarPendingMembers.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = PendingMembersAdapter(
            onApproveClick = { member ->
                // TODO: Step 6 — open approval bottom sheet with package selection
            },
            onRejectClick = { member ->
                // TODO: Step 5 — call rejection logic
            }
        )

        binding.recyclerViewPendingMembers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPendingMembers.adapter = adapter

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarPendingMembers.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.pendingMembers.observe(viewLifecycleOwner) { members ->
            adapter.submitList(members)
            if (members.isEmpty()) {
                binding.layoutEmptyPending.visibility = View.VISIBLE
                binding.recyclerViewPendingMembers.visibility = View.GONE
            } else {
                binding.layoutEmptyPending.visibility = View.GONE
                binding.recyclerViewPendingMembers.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.loadPendingMembers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}