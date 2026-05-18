package com.reprush.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.repository.Result
import com.reprush.app.databinding.FragmentAdminSettingsBinding
import com.reprush.app.ui.auth.SignInActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AdminSettingsFragment : Fragment() {

    private var _binding: FragmentAdminSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminSettingsViewModel by viewModels()

    @Inject lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarSettings.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.settings.observe(viewLifecycleOwner) { settings ->
            binding.switchAutoSuspension.isChecked = settings.autoSuspensionEnabled
            binding.editTextGracePeriod.setText(settings.gracePeriodDays.toString())
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            viewModel.clearSaveResult()
            when (result) {
                is Result.Success -> Snackbar.make(binding.root, "Settings saved", Snackbar.LENGTH_SHORT).show()
                is Result.Error -> Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.suspensionResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            viewModel.clearSuspensionResult()
            when (result) {
                is Result.Success -> {
                    val count = result.data
                    val msg = if (count == 0) "No expired members found" else "$count member(s) suspended"
                    Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                }
                is Result.Error -> Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.buttonSaveSettings.setOnClickListener {
            val enabled = binding.switchAutoSuspension.isChecked
            val graceDays = binding.editTextGracePeriod.text.toString().toIntOrNull() ?: 3
            viewModel.saveSettings(enabled, graceDays)
        }

        binding.buttonRunSuspensionNow.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Run Auto-Suspension")
                .setMessage("This will suspend all active members whose membership expired beyond the grace period. Continue?")
                .setPositiveButton("Run") { _, _ -> viewModel.runAutoSuspensionNow() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.buttonSignOut.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    auth.signOut()
                    val intent = Intent(requireContext(), SignInActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewModel.loadSettings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
