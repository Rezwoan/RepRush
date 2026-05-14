package com.reprush.app.ui.admin.attendance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentAttendanceBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class AttendanceFragment : Fragment() {

    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AttendanceViewModel by viewModels()
    private lateinit var adapter: AttendanceMemberAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarAttendance.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.textViewAttendanceDate.text = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))

        adapter = AttendanceMemberAdapter { member ->
            viewModel.markAttendance(member.uid)
        }
        binding.recyclerViewAttendance.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAttendance.adapter = adapter

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarAttendance.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.activeMembers.observe(viewLifecycleOwner) { updateList() }
        viewModel.todayCheckedIn.observe(viewLifecycleOwner) { updateList() }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            viewModel.clearOperationResult()
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Attendance marked", Snackbar.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.loadData()
    }

    private fun updateList() {
        val members = viewModel.activeMembers.value ?: return
        val checkedIn = viewModel.todayCheckedIn.value ?: emptySet()

        if (members.isEmpty()) {
            binding.layoutEmptyAttendance.visibility = View.VISIBLE
            binding.recyclerViewAttendance.visibility = View.GONE
            return
        }

        binding.layoutEmptyAttendance.visibility = View.GONE
        binding.recyclerViewAttendance.visibility = View.VISIBLE

        val items = members.map { member ->
            AttendanceMemberItem(member, checkedIn.contains(member.uid))
        }
        adapter.submitList(items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
