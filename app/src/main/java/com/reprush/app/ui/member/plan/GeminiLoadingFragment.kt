package com.reprush.app.ui.member.plan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.reprush.app.R
import com.reprush.app.databinding.FragmentGeminiLoadingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeminiLoadingFragment : Fragment() {

    private var _binding: FragmentGeminiLoadingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlanGenerationViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGeminiLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }

        viewModel.generationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                GenerationState.IDLE -> viewModel.generatePlan()
                GenerationState.GENERATING -> {
                    binding.progressBarGemini.visibility = View.VISIBLE
                    binding.textViewGeneratingMessage.text = "Generating your personalized plan..."
                    binding.textViewErrorMessage.visibility = View.GONE
                    binding.buttonRetryGenerate.visibility = View.GONE
                }
                GenerationState.DONE -> {
                    findNavController().navigate(R.id.action_geminiLoadingFragment_to_planSummaryFragment)
                }
                GenerationState.ERROR -> {
                    binding.progressBarGemini.visibility = View.GONE
                    binding.textViewGeneratingMessage.text = "Something went wrong."
                    binding.textViewErrorMessage.visibility = View.VISIBLE
                    binding.textViewErrorMessage.text = viewModel.errorMessage.value ?: "Unknown error"
                    binding.buttonRetryGenerate.visibility = View.VISIBLE
                }
                null -> {}
            }
        }

        binding.buttonRetryGenerate.setOnClickListener {
            viewModel.resetState()
            viewModel.generatePlan()
        }

        if (viewModel.generationState.value == GenerationState.IDLE) {
            viewModel.generatePlan()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
