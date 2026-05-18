package com.reprush.app.ui.member.session

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.reprush.app.R
import com.reprush.app.data.repository.SessionExercise
import com.reprush.app.data.repository.SessionSet
import com.reprush.app.databinding.FragmentActiveSessionBinding
import com.reprush.app.utils.AssetImageLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

        // Close button and back-press show the exit dialog
        binding.buttonCloseSession.setOnClickListener { showExitDialog() }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { showExitDialog() }
            }
        )

        // Notes: done end-icon and IME Done dismiss keyboard; focus expands/collapses field
        binding.textInputLayoutSessionNotes.setEndIconOnClickListener {
            binding.editTextSessionNotes.clearFocus()
            hideKeyboard()
        }
        binding.editTextSessionNotes.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.editTextSessionNotes.clearFocus()
                hideKeyboard()
                true
            } else false
        }
        // Bug 7: expand to 4 lines on focus, collapse to 1 line on blur
        binding.editTextSessionNotes.setOnFocusChangeListener { _, hasFocus ->
            binding.editTextSessionNotes.maxLines = if (hasFocus) 4 else 1
            if (!hasFocus) hideKeyboard()
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

        // Bug 6: collect SharedFlow with repeatOnLifecycle so events are never missed
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.restTimerEvent.collect { timerData ->
                    Log.d("RestTimer", "Fragment: collected event for ${timerData.exerciseName}")
                    showRestTimerSheet(timerData)
                }
            }
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

    // Bug 8: renamed "Save & Exit" → "Save for Later"
    private fun showExitDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("End Session?")
            .setMessage("Your progress will be saved. You can resume this session later.")
            .setPositiveButton("Save for Later") { _, _ ->
                viewModel.clearSession()
                findNavController().navigate(R.id.action_activeSessionFragment_to_homeFragment)
            }
            .setNegativeButton("Discard") { _, _ ->
                viewModel.discardCurrentSession {
                    if (isAdded) {
                        findNavController().navigate(R.id.action_activeSessionFragment_to_homeFragment)
                    }
                }
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
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

        AssetImageLoader.load(requireContext(), exercise.imageUrl, imageView)

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
        val weightEdit = rowView.findViewById<EditText>(R.id.editText_weight)
        val repsEdit = rowView.findViewById<EditText>(R.id.editText_reps)
        val warmupChip = rowView.findViewById<Chip>(R.id.chip_warmup)
        val bwChip = rowView.findViewById<Chip>(R.id.chip_bodyweight)
        val completeBtn = rowView.findViewById<ImageButton>(R.id.imageButton_completeSet)

        setNumberText.text = set.setNumber.toString()
        // Bug 3: alpha applied immediately from current state
        rowView.alpha = if (set.isWarmup) 0.5f else 1.0f

        // Bug 5: BW toggles weight field to disabled/greyed "BW" label — no GONE, no layout shift
        if (set.isBodyweight) {
            weightEdit.isEnabled = false
            weightEdit.alpha = 0.38f
            weightEdit.setText("BW")
        } else {
            weightEdit.isEnabled = true
            weightEdit.alpha = 1.0f
            // Bug 1: null means not yet entered — show nothing, let hint show
            if (set.weight != null && set.weight > 0) {
                weightEdit.setText(
                    if (set.weight == set.weight.toLong().toDouble()) set.weight.toLong().toString()
                    else set.weight.toString()
                )
            }
        }
        // Bug 1: null means not yet entered — show nothing, let hint show
        if (set.reps != null && set.reps > 0) repsEdit.setText(set.reps.toString())
        if (exercise.plannedRepsRange.isNotBlank()) repsEdit.hint = exercise.plannedRepsRange

        warmupChip.isChecked = set.isWarmup
        // Bug 3: set alpha immediately on user toggle, don't wait for LiveData rebuild
        warmupChip.setOnCheckedChangeListener { _, isChecked ->
            rowView.alpha = if (isChecked) 0.5f else 1.0f
            viewModel.toggleWarmup(exIndex, setIndex)
        }

        bwChip.isChecked = set.isBodyweight
        // Bug 5: listener fires after isChecked is set, so no spurious call on bind
        bwChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                weightEdit.isEnabled = false
                weightEdit.alpha = 0.38f
                weightEdit.setText("BW")
            } else {
                weightEdit.isEnabled = true
                weightEdit.alpha = 1.0f
                weightEdit.text?.clear()
            }
            viewModel.toggleBodyweight(exIndex, setIndex)
        }

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
        weightEdit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) viewModel.cascadeSetWeight(exIndex, setIndex)
        }

        repsEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val r = s?.toString()?.toIntOrNull() ?: return
                viewModel.updateSetReps(exIndex, setIndex, r)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        repsEdit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) viewModel.cascadeSetReps(exIndex, setIndex)
        }
        // Bug 4: IME Done on reps dismisses keyboard
        repsEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                repsEdit.clearFocus()
                hideKeyboard()
                true
            } else false
        }

        completeBtn.setOnClickListener {
            if (set.isCompleted) return@setOnClickListener
            if (!set.isBodyweight) {
                val weightText = weightEdit.text?.toString()
                if (weightText.isNullOrBlank()) { shakeView(weightEdit); return@setOnClickListener }
                val weight = weightText.toDoubleOrNull()
                if (weight == null) { shakeView(weightEdit); return@setOnClickListener }
            }
            val repsText = repsEdit.text?.toString()
            if (repsText.isNullOrBlank()) { shakeView(repsEdit); return@setOnClickListener }
            val reps = repsText.toIntOrNull()
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
