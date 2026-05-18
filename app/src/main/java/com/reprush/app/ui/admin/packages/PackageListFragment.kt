package com.reprush.app.ui.admin.packages

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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackageListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setEditTarget(null)

        adapter = PackageAdapter(
            onEdit = { pkg ->
                viewModel.setEditTarget(pkg)
                findNavController().navigate(R.id.action_packageListFragment_to_createPackageFragment)
            },
            onDelete = { pkg ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Package")
                    .setMessage("Delete \"${pkg.name}\"? This cannot be undone.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deletePackage(pkg.id)
                    }
                    .show()
            }
        )

        binding.recyclerViewPackages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPackages.adapter = adapter

        binding.fabNewPackage.setOnClickListener {
            findNavController().navigate(R.id.action_packageListFragment_to_createPackageFragment)
        }

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
            result ?: return@observe
            viewModel.clearOperationResult()
            if (result is Result.Error) {
                Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.progressBarPackages.visibility = View.VISIBLE
        viewModel.loadPackages()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
