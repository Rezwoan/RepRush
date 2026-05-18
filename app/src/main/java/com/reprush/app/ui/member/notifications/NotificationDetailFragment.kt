package com.reprush.app.ui.member.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.reprush.app.databinding.FragmentNotificationDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationDetailFragment : Fragment() {

    private var _binding: FragmentNotificationDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        val title = arguments?.getString("title") ?: ""
        val body = arguments?.getString("body") ?: ""
        val createdAt = arguments?.getLong("createdAt") ?: 0L

        binding.textViewDetailTitle.text = title
        binding.textViewDetailBody.text = body
        binding.textViewDetailTime.text = SimpleDateFormat(
            "MMMM d, yyyy 'at' h:mm a", Locale.getDefault()
        ).format(Date(createdAt))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
