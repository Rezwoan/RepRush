package com.reprush.app.ui.admin.packages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.R
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentPackageListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PackageListFragment : Fragment() {

    private var _binding: FragmentPackageListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PackageViewModel by activityViewModels()
    private lateinit var adapter: PackageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackageListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        observeViewModel()
        binding.progressBarPackages.visibility = View.VISIBLE
        viewModel.loadPackages()
    }

    private fun setupRecyclerView() {
        adapter = PackageAdapter(onItemClick = { /* TODO: navigate to detail */ })
        binding.recyclerViewPackages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPackages.adapter = adapter
    }

    private fun setupFab() {
        binding.fabNewPackage.setOnClickListener {
            findNavController().navigate(R.id.action_packageListFragment_to_createPackageFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.packages.observe(viewLifecycleOwner) { packages ->
            binding.progressBarPackages.visibility = View.GONE
            if (packages.isEmpty()) {
                binding.layoutEmptyPackages.visibility = View.VISIBLE
                binding.recyclerViewPackages.visibility = View.GONE
            } else {
                binding.layoutEmptyPackages.visibility = View.GONE
                binding.recyclerViewPackages.visibility = View.VISIBLE
                adapter.submitList(packages)
            }
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            if (result is Result.Error) {
                binding.progressBarPackages.visibility = View.GONE
                Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
