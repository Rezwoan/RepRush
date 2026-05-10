package com.reprush.app.ui.admin.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.data.repository.Result
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
                ApprovalBottomSheet.newInstance(member.uid, member.displayName)
                    .show(parentFragmentManager, "approval_sheet")
            },
            onRejectClick = { member ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Reject Registration")
                    .setMessage("Reject ${member.displayName}'s registration? They will be notified.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Reject") { _, _ ->
                        viewModel.rejectMember(member.uid)
                    }
                    .show()
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

        // Null-safe observer — only reacts when result is non-null
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Action completed", Snackbar.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
            viewModel.clearOperationResult()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPendingMembers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}