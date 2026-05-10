package com.reprush.app.ui.admin.members

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.R
import com.reprush.app.databinding.FragmentMemberDirectoryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MemberDirectoryFragment : Fragment() {

    private var _binding: FragmentMemberDirectoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MemberDirectoryViewModel by viewModels()
    private lateinit var adapter: MemberAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberDirectoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarMemberDirectory.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = MemberAdapter { member ->
            val bundle = Bundle().apply { putString("memberUid", member.uid) }
            findNavController().navigate(
                R.id.action_memberDirectoryFragment_to_memberDetailFragment,
                bundle
            )
        }
        binding.recyclerViewMembers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewMembers.adapter = adapter

        // Search
        binding.editTextSearchMembers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onSearchQueryChanged(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Filter chips
        val chipMap = mapOf(
            binding.chipFilterAll       to "all",
            binding.chipFilterActive    to "active",
            binding.chipFilterPending   to "pending",
            binding.chipFilterExpired   to "expired",
            binding.chipFilterSuspended to "suspended"
        )
        chipMap.forEach { (chip, status) ->
            chip.setOnClickListener {
                chipMap.keys.forEach { it.isChecked = false }
                chip.isChecked = true
                viewModel.onStatusFilterChanged(status)
            }
        }

        // Clear filters
        binding.buttonClearFilters.setOnClickListener {
            binding.editTextSearchMembers.setText("")
            chipMap.keys.forEach { it.isChecked = false }
            binding.chipFilterAll.isChecked = true
            viewModel.onStatusFilterChanged("all")
        }

        // FAB
        binding.fabRegisterMember.setOnClickListener {
            findNavController().navigate(
                R.id.action_memberDirectoryFragment_to_manualRegisterFragment
            )
        }

        // Observers
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarMembers.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.filteredMembers.observe(viewLifecycleOwner) { members ->
            adapter.submitList(members)
            if (members.isEmpty()) {
                binding.layoutEmptyMembers.visibility = View.VISIBLE
                binding.recyclerViewMembers.visibility = View.GONE
            } else {
                binding.layoutEmptyMembers.visibility = View.GONE
                binding.recyclerViewMembers.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.loadMembers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}