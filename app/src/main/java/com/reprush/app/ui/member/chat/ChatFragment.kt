package com.reprush.app.ui.member.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.reprush.app.databinding.FragmentChatBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply { duration = 300L }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply { duration = 300L }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        chatAdapter = ChatAdapter()
        binding.recyclerViewChat.layoutManager = layoutManager
        binding.recyclerViewChat.adapter = chatAdapter

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.recyclerViewChat.scrollToPosition(messages.size - 1)
                }
            }
            binding.textViewChatEmpty.visibility =
                if (messages.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isTyping.observe(viewLifecycleOwner) { typing ->
            binding.layoutTypingIndicator.visibility = if (typing) View.VISIBLE else View.GONE
            binding.buttonSendChat.isEnabled = !typing
        }

        binding.buttonChatBack.setOnClickListener { findNavController().popBackStack() }

        binding.buttonSendChat.setOnClickListener { sendMessage() }

        binding.editTextChatInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }

        viewModel.loadHistory()
    }

    private fun sendMessage() {
        val text = binding.editTextChatInput.text?.toString()?.trim() ?: return
        if (text.isBlank()) return
        binding.editTextChatInput.text?.clear()
        viewModel.sendMessage(text)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
