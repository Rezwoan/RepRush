package com.reprush.app.ui.member.session

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.reprush.app.R
import com.reprush.app.data.repository.SessionExercise
import com.reprush.app.data.repository.SessionSet
import com.reprush.app.databinding.FragmentActiveSessionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActiveSessionFragment : Fragment() {

    private var _binding: FragmentActiveSessionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActiveSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val planDayId = arguments?.getString("planDayId")
        val sessionAlreadyActive = viewModel.sessionState.value is SessionState.Active

        if (!sessionAlreadyActive) {
            if (!planDayId.isNullOrBlank()) {
                viewModel.startFromPlan(planDayId)
            } else {
                viewModel.startBlankSession()
            }
        }

        viewModel.elapsedTime.observe(viewLifecycleOwner) { time ->
            binding.textViewElapsedTimer.text = time
        }

        viewModel.activeSession.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe
            rebuildExerciseCards(state.exercises)
            binding.layoutEmptySession.visibility =
                if (state.exercises.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.restTimerEvent.observe(viewLifecycleOwner) { timerData ->
            if (timerData == null) return@observe
            showRestTimerSheet(timerData)
            viewModel.clearRestTimerEvent()
        }

        binding.editTextSessionNotes.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateNotes(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.fabAddExercise.setOnClickListener {
            val sheet = ExercisePickerBottomSheet.newInstance()
            sheet.onExerciseSelected = { exercise -> viewModel.addExercise(exercise) }
            sheet.show(childFragmentManager, "ExercisePicker")
        }

        binding.buttonFinish.setOnClickListener {
            if (viewModel.getWorkingSetCount() == 0) {
                AlertDialog.Builder(requireContext())
                    .setTitle("No working sets logged")
                    .setMessage(
                        "You have not logged any working sets. " +
                        "Sessions without working sets earn 0 points. " +
                        "Finish anyway or keep training?"
                    )
                    .setPositiveButton("Finish Anyway") { _, _ -> navigateToFinish() }
                    .setNegativeButton("Keep Training", null)
                    .show()
            } else {
                navigateToFinish()
            }
        }
    }

    private fun navigateToFinish() {
        findNavController().navigate(R.id.action_activeSessionFragment_to_finishWorkoutFragment)
    }

    private fun rebuildExerciseCards(exercises: List<SessionExercise>) {
        val container = binding.layoutExerciseContainer
        container.removeAllViews()
        exercises.forEachIndexed { exIndex, exercise ->
            val cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_session_exercise, container, false)
            bindExerciseCard(cardView, exIndex, exercise)
            container.addView(cardView)
        }
    }

    private fun bindExerciseCard(cardView: View, exIndex: Int, exercise: SessionExercise) {
        val imageView = cardView.findViewById<android.widget.ImageView>(R.id.imageView_exercisePhoto)
        val nameText = cardView.findViewById<TextView>(R.id.textView_exerciseName)
        val collapseBtn = cardView.findViewById<ImageButton>(R.id.imageButton_collapse)
        val contentLayout = cardView.findViewById<LinearLayout>(R.id.layout_exerciseContent)
        val setContainer = cardView.findViewById<LinearLayout>(R.id.layout_setContainer)
        val addSetBtn = cardView.findViewById<View>(R.id.button_addSet)

        nameText.text = exercise.exerciseName

        Glide.with(this)
            .load(exercise.imageUrl)
            .centerCrop()
            .placeholder(android.R.color.darker_gray)
            .into(imageView)

        var isCollapsed = false
        collapseBtn.setOnClickListener {
            isCollapsed = !isCollapsed
            contentLayout.visibility = if (isCollapsed) View.GONE else View.VISIBLE
            collapseBtn.setImageResource(
                if (isCollapsed) android.R.drawable.arrow_down_float
                else android.R.drawable.arrow_up_float
            )
        }

        setContainer.removeAllViews()
        exercise.sets.forEachIndexed { setIndex, set ->
            val setRow = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_set_row, setContainer, false)
            bindSetRow(setRow, exIndex, setIndex, exercise, set)
            setContainer.addView(setRow)
        }

        addSetBtn.setOnClickListener { viewModel.addSet(exIndex) }
    }

    private fun bindSetRow(
        rowView: View,
        exIndex: Int,
        setIndex: Int,
        exercise: SessionExercise,
        set: SessionSet
    ) {
        val setNumberText = rowView.findViewById<TextView>(R.id.textView_setNumber)
        val weightEdit = rowView.findViewById<TextInputEditText>(R.id.editText_weight)
        val repsEdit = rowView.findViewById<TextInputEditText>(R.id.editText_reps)
        val warmupChip = rowView.findViewById<Chip>(R.id.chip_warmup)
        val completeBtn = rowView.findViewById<ImageButton>(R.id.imageButton_completeSet)

        setNumberText.text = set.setNumber.toString()
        rowView.alpha = if (set.isWarmup) 0.5f else 1.0f

        if (set.weight > 0) {
            weightEdit.setText(
                if (set.weight == set.weight.toLong().toDouble()) set.weight.toLong().toString()
                else set.weight.toString()
            )
        }
        if (set.reps > 0) repsEdit.setText(set.reps.toString())
        if (exercise.plannedRepsRange.isNotBlank()) repsEdit.hint = exercise.plannedRepsRange

        warmupChip.isChecked = set.isWarmup
        warmupChip.setOnCheckedChangeListener { _, _ -> viewModel.toggleWarmup(exIndex, setIndex) }

        completeBtn.setImageResource(
            if (set.isCompleted) android.R.drawable.checkbox_on_background
            else android.R.drawable.checkbox_off_background
        )
        completeBtn.alpha = if (set.isCompleted) 1.0f else 0.6f

        weightEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val w = s?.toString()?.toDoubleOrNull() ?: return
                viewModel.updateSetWeight(exIndex, setIndex, w)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        repsEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val r = s?.toString()?.toIntOrNull() ?: return
                viewModel.updateSetReps(exIndex, setIndex, r)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        completeBtn.setOnClickListener {
            if (set.isCompleted) return@setOnClickListener
            val weightText = weightEdit.text?.toString()
            val repsText = repsEdit.text?.toString()
            if (weightText.isNullOrBlank()) { shakeView(weightEdit); return@setOnClickListener }
            if (repsText.isNullOrBlank()) { shakeView(repsEdit); return@setOnClickListener }
            val weight = weightText.toDoubleOrNull()
            val reps = repsText.toIntOrNull()
            if (weight == null) { shakeView(weightEdit); return@setOnClickListener }
            if (reps == null || reps <= 0) { shakeView(repsEdit); return@setOnClickListener }
            viewModel.completeSet(exIndex, setIndex)
        }
    }

    private fun shakeView(view: View) {
        val shake = TranslateAnimation(-8f, 8f, 0f, 0f).apply {
            duration = 50
            repeatCount = 5
            repeatMode = Animation.REVERSE
        }
        view.startAnimation(shake)
    }

    private fun showRestTimerSheet(timerData: RestTimerData) {
        val sheet = RestTimerBottomSheet.newInstance(
            timerData.exerciseName,
            timerData.durationSeconds
        )
        sheet.show(childFragmentManager, "RestTimerSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
