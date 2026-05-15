package com.reprush.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.reprush.app.R
import com.reprush.app.databinding.FragmentStatusBlockedBinding

class StatusBlockedFragment : Fragment(R.layout.fragment_status_blocked) {

    private var _binding: FragmentStatusBlockedBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatusBlockedBinding.bind(view)

        val isRejected = arguments?.getBoolean("rejected", false) ?: false

        if (isRejected) {
            binding.textViewBlockedTitle.text = "Registration Not Approved"
            binding.textViewBlockedMessage.text =
                "Your registration request was not approved. Please contact the gym for more information."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
