package com.reprush.app.ui.member.plan

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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlanExercisePickerBottomSheet : BottomSheetDialogFragment() {

    @Inject lateinit var exerciseRepository: ExerciseRepository

    var onExerciseSelected: ((exerciseId: String, exerciseName: String) -> Unit)? = null

    private var selectedMuscle = ""
    private var selectedEquipment = ""

    private val muscleChipLabels = listOf("All", "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Core")
    private val equipmentChipLabels = listOf("All", "Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight")

    companion object {
        fun newInstance() = PlanExercisePickerBottomSheet()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.sheet_exercise_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchEdit = view.findViewById<TextInputEditText>(R.id.editText_pickerSearch)
        val listContainer = view.findViewById<LinearLayout>(R.id.layout_pickerList)
        val muscleGroup = view.findViewById<ChipGroup>(R.id.chipGroup_pickerMuscle)
        val equipmentGroup = view.findViewById<ChipGroup>(R.id.chipGroup_pickerEquipment)

        fun loadExercises() {
            viewLifecycleOwner.lifecycleScope.launch {
                val exercises = exerciseRepository.getExercisesFiltered(
                    query = searchEdit.text?.toString() ?: "",
                    muscle = selectedMuscle,
                    equipment = selectedEquipment
                )
                listContainer.removeAllViews()
                exercises.take(50).forEach { ex ->
                    val item = TextView(requireContext()).apply {
                        text = ex.name
                        textSize = 15f
                        setTextColor(resources.getColor(R.color.on_surface, null))
                        val pad = resources.getDimensionPixelSize(R.dimen.spacing_md)
                        setPadding(pad, pad, pad, pad)
                        isClickable = true
                        isFocusable = true
                        setBackgroundResource(android.R.drawable.list_selector_background)
                        setOnClickListener {
                            onExerciseSelected?.invoke(ex.id, ex.name)
                            dismiss()
                        }
                    }
                    listContainer.addView(item)
                }
            }
        }

        muscleChipLabels.forEach { label ->
            muscleGroup.addView(Chip(requireContext()).apply {
                text = label; isCheckable = true; id = View.generateViewId()
            })
        }
        muscleGroup.setOnCheckedStateChangeListener { g, ids ->
            val chip = ids.firstOrNull()?.let { g.findViewById<Chip>(it) }
            selectedMuscle = if (chip == null || chip.text == "All") "" else chip.text.toString()
            loadExercises()
        }

        equipmentChipLabels.forEach { label ->
            equipmentGroup.addView(Chip(requireContext()).apply {
                text = label; isCheckable = true; id = View.generateViewId()
            })
        }
        equipmentGroup.setOnCheckedStateChangeListener { g, ids ->
            val chip = ids.firstOrNull()?.let { g.findViewById<Chip>(it) }
            selectedEquipment = if (chip == null || chip.text == "All") "" else chip.text.toString()
            loadExercises()
        }

        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { loadExercises() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadExercises()
    }
}
