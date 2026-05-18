package com.reprush.app.ui.member.plan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.local.dao.PlanDayDao
import com.reprush.app.data.local.dao.PlanExerciseDao
import com.reprush.app.data.local.dao.WorkoutPlanDao
import com.reprush.app.data.local.entity.PlanDayEntity
import com.reprush.app.data.local.entity.PlanExerciseEntity
import com.reprush.app.data.local.entity.WorkoutPlanEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DraftExercise(
    val exerciseId: String,
    val exerciseName: String,
    var sets: Int = 3,
    var repsRange: String = "8-12",
    var restSeconds: Int = 90
)

data class DraftDay(
    val dayNumber: Int,
    var dayLabel: String,
    val exercises: MutableList<DraftExercise> = mutableListOf()
)

@HiltViewModel
class ManualPlanBuilderViewModel @Inject constructor(
    private val workoutPlanDao: WorkoutPlanDao,
    private val planDayDao: PlanDayDao,
    private val planExerciseDao: PlanExerciseDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _days = MutableLiveData<List<DraftDay>>(emptyList())
    val days: LiveData<List<DraftDay>> = _days

    private val _saveSuccess = MutableLiveData<Boolean?>(null)
    val saveSuccess: LiveData<Boolean?> = _saveSuccess

    fun initDays(count: Int) {
        val defaultLabels = mapOf(
            1 to "Full Body", 2 to "Upper Body", 3 to "Lower Body",
            4 to "Push Day", 5 to "Pull Day", 6 to "Leg Day", 7 to "Core Day"
        )
        val existing = _days.value ?: emptyList()
        val newDays = (1..count).map { n ->
            existing.getOrNull(n - 1)?.copy(dayNumber = n)
                ?: DraftDay(n, defaultLabels[n] ?: "Day $n")
        }
        _days.value = newDays
    }

    fun updateDayLabel(dayIndex: Int, label: String) {
        val list = _days.value?.toMutableList() ?: return
        list[dayIndex] = list[dayIndex].copy(dayLabel = label)
        _days.value = list
    }

    fun addExercise(dayIndex: Int, exercise: DraftExercise) {
        val list = _days.value?.map { it.copy(exercises = it.exercises.toMutableList()) }?.toMutableList() ?: return
        list[dayIndex].exercises.add(exercise)
        _days.value = list
    }

    fun removeExercise(dayIndex: Int, exerciseIndex: Int) {
        val list = _days.value?.map { it.copy(exercises = it.exercises.toMutableList()) }?.toMutableList() ?: return
        list[dayIndex].exercises.removeAt(exerciseIndex)
        _days.value = list
    }

    fun updateExercise(dayIndex: Int, exerciseIndex: Int, sets: Int, repsRange: String, restSeconds: Int) {
        val list = _days.value?.map { it.copy(exercises = it.exercises.toMutableList()) }?.toMutableList() ?: return
        val ex = list[dayIndex].exercises[exerciseIndex]
        list[dayIndex].exercises[exerciseIndex] = ex.copy(sets = sets, repsRange = repsRange, restSeconds = restSeconds)
        _days.value = list
    }

    fun isReadyToSave(planName: String, goal: String): Boolean {
        if (planName.isBlank() || goal.isBlank()) return false
        val days = _days.value ?: return false
        if (days.isEmpty()) return false
        return days.all { it.exercises.isNotEmpty() }
    }

    fun savePlan(planName: String, goal: String, weeks: Int) {
        val userId = auth.currentUser?.uid ?: return
        val days = _days.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                workoutPlanDao.deactivateAllPlans(userId)
                val planId = UUID.randomUUID().toString()
                workoutPlanDao.insertPlan(
                    WorkoutPlanEntity(
                        id = planId,
                        userId = userId,
                        planName = planName.trim(),
                        goal = goal,
                        totalWeeks = weeks,
                        daysPerWeek = days.size,
                        schemaVersion = 1,
                        isActive = 1,
                        createdAt = System.currentTimeMillis()
                    )
                )
                for (day in days) {
                    val dayId = UUID.randomUUID().toString()
                    planDayDao.insertPlanDay(
                        PlanDayEntity(id = dayId, planId = planId, dayNumber = day.dayNumber, dayLabel = day.dayLabel)
                    )
                    day.exercises.forEachIndexed { idx, ex ->
                        planExerciseDao.insertPlanExercise(
                            PlanExerciseEntity(
                                id = UUID.randomUUID().toString(),
                                dayId = dayId,
                                exerciseId = ex.exerciseId,
                                sets = ex.sets,
                                repsRange = ex.repsRange,
                                restSeconds = ex.restSeconds,
                                orderIndex = idx,
                                notes = null
                            )
                        )
                    }
                }
                _saveSuccess.postValue(true)
            } catch (e: Exception) {
                _saveSuccess.postValue(false)
            }
        }
    }

    fun clearSaveResult() { _saveSuccess.value = null }
}
