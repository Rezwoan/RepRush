package com.reprush.app.ui.member.session

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.reprush.app.R
import com.reprush.app.data.repository.ExerciseRepository
import com.reprush.app.data.repository.SessionExercise
import com.reprush.app.data.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExercisePickerBottomSheet : BottomSheetDialogFragment() {

    @Inject lateinit var exerciseRepository: ExerciseRepository
    @Inject lateinit var sessionRepository: SessionRepository

    var onExerciseSelected: ((SessionExercise) -> Unit)? = null

    private var selectedMuscle = ""
    private var selectedEquipment = ""

    private val muscleChipLabels = listOf("All", "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Core")
    private val equipmentChipLabels = listOf("All", "Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight")

    companion object {
        fun newInstance() = ExercisePickerBottomSheet()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.sheet_exercise_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchEdit = view.findViewById<TextInputEditText>(R.id.editText_pickerSearch)
        val listContainer = view.findViewById<LinearLayout>(R.id.layout_pickerList)
        val muscleGroup = view.findViewById<ChipGroup>(R.id.chipGroup_pickerMuscle)
        val equipmentGroup = view.findViewById<ChipGroup>(R.id.chipGroup_pickerEquipment)

        fun loadExercises() {
            viewLifecycleOwner.lifecycleScope.launch {
                val query = searchEdit.text?.toString() ?: ""
                val exercises = exerciseRepository.getExercisesFiltered(
                    query = query,
                    muscle = selectedMuscle,
                    equipment = selectedEquipment
                )
                listContainer.removeAllViews()
                exercises.take(30).forEach { ex ->
                    val itemView = TextView(requireContext()).apply {
                        text = ex.name
                        textSize = 16f
                        setTextColor(resources.getColor(R.color.on_surface, null))
                        setPadding(
                            resources.getDimensionPixelSize(R.dimen.spacing_md),
                            resources.getDimensionPixelSize(R.dimen.spacing_md),
                            resources.getDimensionPixelSize(R.dimen.spacing_md),
                            resources.getDimensionPixelSize(R.dimen.spacing_md)
                        )
                        isClickable = true
                        isFocusable = true
                        setBackgroundResource(android.R.drawable.list_selector_background)
                        setOnClickListener {
                            viewLifecycleOwner.lifecycleScope.launch {
                                val sessionEx = sessionRepository.loadExerciseById(ex.id)
                                if (sessionEx != null) {
                                    onExerciseSelected?.invoke(sessionEx)
                                }
                                dismiss()
                            }
                        }
                    }
                    listContainer.addView(itemView)
                }
            }
        }

        // Muscle chips
        muscleChipLabels.forEach { label ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                id = View.generateViewId()
            }
            muscleGroup.addView(chip)
        }
        muscleGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = if (checkedIds.isEmpty()) null else group.findViewById<Chip>(checkedIds[0])
            selectedMuscle = when {
                chip == null || chip.text == "All" -> ""
                else -> chip.text.toString()
            }
            loadExercises()
        }

        // Equipment chips
        equipmentChipLabels.forEach { label ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                id = View.generateViewId()
            }
            equipmentGroup.addView(chip)
        }
        equipmentGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = if (checkedIds.isEmpty()) null else group.findViewById<Chip>(checkedIds[0])
            selectedEquipment = when {
                chip == null || chip.text == "All" -> ""
                else -> chip.text.toString()
            }
            loadExercises()
        }

        loadExercises()

        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { loadExercises() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}
