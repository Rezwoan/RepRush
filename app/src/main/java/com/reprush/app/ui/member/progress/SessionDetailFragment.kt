package com.reprush.app.ui.member.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.reprush.app.databinding.FragmentSessionDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class SessionDetailFragment : Fragment() {

    private var _binding: FragmentSessionDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionDetailViewModel by viewModels()
    private val exerciseAdapter = SessionExerciseAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerSessionExercises.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSessionExercises.adapter = exerciseAdapter

        val sessionId = arguments?.getString("sessionId") ?: return

        viewModel.header.observe(viewLifecycleOwner) { header ->
            binding.textSessionDate.text = header.dateFormatted
            binding.textSessionDayLabel.text = header.dayLabel
            binding.textSessionDuration.text = header.durationText
            binding.textSessionVolume.text =
                String.format(Locale.getDefault(), "%.0f kg", header.volumeKg)
            binding.textSessionPoints.text = "${header.totalPoints} pts"
        }

        viewModel.exerciseGroups.observe(viewLifecycleOwner) { groups ->
            exerciseAdapter.submitList(groups)
        }

        viewModel.load(sessionId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
