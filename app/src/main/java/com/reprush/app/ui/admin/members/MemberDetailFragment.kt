package com.reprush.app.ui.admin.members

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.data.repository.MemberDetail
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentMemberDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@AndroidEntryPoint
class MemberDetailFragment : Fragment() {

    private var _binding: FragmentMemberDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MemberDetailViewModel by viewModels()
    private var memberUid: String = ""
    private var hasPerformedAction = false
    private var pendingRemove = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarMemberDetail.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        memberUid = arguments?.getString("memberUid") ?: ""
        if (memberUid.isEmpty()) {
            findNavController().navigateUp()
            return
        }

        viewModel.loadMember(memberUid)

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarMemberDetail.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.member.observe(viewLifecycleOwner) { member ->
            member ?: return@observe
            bindMember(member)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        // Only react to results after a user-triggered action
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (!hasPerformedAction) return@observe
            hasPerformedAction = false
            when (result) {
                is Result.Success -> {
                    if (pendingRemove) {
                        pendingRemove = false
                        findNavController().navigateUp()
                    } else {
                        Snackbar.make(binding.root, "Action completed successfully", Snackbar.LENGTH_SHORT).show()
                        // ViewModel already reloads member for suspend/reactivate
                    }
                }
                is Result.Error -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun bindMember(member: MemberDetail) {
        binding.textViewDetailName.text = member.displayName
        binding.textViewDetailEmail.text = member.email
        binding.textViewTotalWorkouts.text = member.totalWorkouts.toString()
        binding.textViewTotalPRs.text = member.totalPRs.toString()
        binding.textViewTotalPoints.text = member.totalPoints.toString()

        // Status chip
        val (statusLabel, chipColor) = when (member.membershipStatus) {
            "active"    -> Pair("Active",    "#4CAF50")
            "expired"   -> Pair("Expired",   "#F44336")
            "suspended" -> Pair("Suspended", "#FF9800")
            "pending"   -> Pair("Pending",   "#2196F3")
            else        -> Pair("Unknown",   "#9E9E9E")
        }
        binding.chipDetailStatus.text = statusLabel
        binding.chipDetailStatus.chipBackgroundColor =
            android.content.res.ColorStateList.valueOf(Color.parseColor(chipColor))
        binding.chipDetailStatus.setTextColor(Color.WHITE)

        // Membership info
        binding.textViewPackageName.text = member.packageName ?: "No package assigned"
        binding.textViewMembershipStart.text =
            if (member.membershipStartDate != null) "Start: ${member.membershipStartDate}" else "Start: —"
        binding.textViewMembershipEnd.text =
            if (member.membershipEndDate != null) "Expires: ${member.membershipEndDate}" else "Expires: —"

        // Days remaining
        if (member.membershipEndDate != null) {
            try {
                val endDate = LocalDate.parse(member.membershipEndDate)
                val today = LocalDate.now()
                val daysLeft = ChronoUnit.DAYS.between(today, endDate)
                binding.textViewDaysRemaining.text = when {
                    daysLeft > 0   -> "$daysLeft days remaining"
                    daysLeft == 0L -> "Expires today"
                    else           -> "Expired ${-daysLeft} days ago"
                }
                binding.textViewDaysRemaining.setTextColor(
                    Color.parseColor(when {
                        daysLeft >= 10 -> "#4CAF50"
                        daysLeft >= 3  -> "#FF9800"
                        else           -> "#F44336"
                    })
                )
            } catch (e: Exception) {
                binding.textViewDaysRemaining.text = "—"
            }
        } else {
            binding.textViewDaysRemaining.text = "—"
        }

        // Button visibility — always recalculate on every bind call
        binding.buttonSuspendMember.visibility = View.GONE
        binding.buttonReactivateMember.visibility = View.GONE

        when (member.membershipStatus) {
            "active" -> binding.buttonSuspendMember.visibility = View.VISIBLE
            "suspended" -> binding.buttonReactivateMember.visibility = View.VISIBLE
        }

        // Button clicks
        binding.buttonSuspendMember.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Suspend Member")
                .setMessage("Suspend ${member.displayName}'s account? They will lose access to the app.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Suspend") { _, _ ->
                    hasPerformedAction = true
                    pendingRemove = false
                    viewModel.suspendMember(member.uid)
                }
                .show()
        }

        binding.buttonReactivateMember.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reactivate Member")
                .setMessage("Reactivate ${member.displayName}'s account?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reactivate") { _, _ ->
                    hasPerformedAction = true
                    pendingRemove = false
                    viewModel.reactivateMember(member.uid)
                }
                .show()
        }

        binding.buttonRemoveMember.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove Member")
                .setMessage("Permanently remove ${member.displayName}? This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove") { _, _ ->
                    hasPerformedAction = true
                    pendingRemove = true
                    viewModel.removeMember(member.uid)
                }
                .show()
        }

        binding.buttonSendReminder.setOnClickListener {
            Snackbar.make(
                binding.root,
                "Reminder sent to ${member.displayName}",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        binding.buttonRecordPayment.setOnClickListener {
            // TODO: Milestone 3
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}