package com.reprush.app.ui.member.plan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.local.dao.ExerciseDao
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

data class PlanExerciseDetail(
    val planExercise: PlanExerciseEntity,
    val exerciseName: String
)

data class PlanDayDetail(
    val day: PlanDayEntity,
    val exercises: List<PlanExerciseDetail>
)

data class PlanDetailState(
    val plan: WorkoutPlanEntity,
    val days: List<PlanDayDetail>
)

@HiltViewModel
class PlanDetailViewModel @Inject constructor(
    private val workoutPlanDao: WorkoutPlanDao,
    private val planDayDao: PlanDayDao,
    private val planExerciseDao: PlanExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    private val _state = MutableLiveData<PlanDetailState?>()
    val state: LiveData<PlanDetailState?> = _state

    private val _isEditMode = MutableLiveData(false)
    val isEditMode: LiveData<Boolean> = _isEditMode

    fun load(planId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val plan = workoutPlanDao.getPlansForUser(userId).firstOrNull { it.id == planId }
                ?: return@launch
            _state.postValue(buildState(plan))
        }
    }

    private suspend fun buildState(plan: WorkoutPlanEntity): PlanDetailState {
        val days = planDayDao.getDaysForPlan(plan.id)
        val dayDetails = days.map { day ->
            val planExercises = planExerciseDao.getExercisesForDay(day.id)
            val exerciseDetails = planExercises.map { pe ->
                val name = exerciseDao.getExerciseById(pe.exerciseId)?.name ?: "Unknown Exercise"
                PlanExerciseDetail(pe, name)
            }
            PlanDayDetail(day, exerciseDetails)
        }
        return PlanDetailState(plan, dayDetails)
    }

    fun toggleEditMode() {
        _isEditMode.value = !(_isEditMode.value ?: false)
    }

    fun updatePlanName(newName: String) {
        val current = _state.value ?: return
        val updated = current.plan.copy(planName = newName.trim())
        viewModelScope.launch(Dispatchers.IO) {
            workoutPlanDao.updatePlan(updated)
            _state.postValue(buildState(updated))
        }
    }

    fun updateDayLabel(day: PlanDayEntity, newLabel: String) {
        val current = _state.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            planDayDao.updatePlanDay(day.copy(dayLabel = newLabel.trim()))
            _state.postValue(buildState(current.plan))
        }
    }

    fun updateExerciseConfig(planExercise: PlanExerciseEntity, sets: Int, repsRange: String, restSeconds: Int) {
        val current = _state.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            planExerciseDao.updatePlanExercise(
                planExercise.copy(sets = sets, repsRange = repsRange, restSeconds = restSeconds)
            )
            _state.postValue(buildState(current.plan))
        }
    }

    fun deleteExercise(planExercise: PlanExerciseEntity) {
        val current = _state.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            planExerciseDao.deletePlanExercise(planExercise.id)
            _state.postValue(buildState(current.plan))
        }
    }

    fun addExerciseToDay(day: PlanDayEntity, exerciseId: String, exerciseName: String, sets: Int, repsRange: String, restSeconds: Int) {
        val current = _state.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = planExerciseDao.getExercisesForDay(day.id)
            val nextOrder = existing.size
            planExerciseDao.insertPlanExercise(
                PlanExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    dayId = day.id,
                    exerciseId = exerciseId,
                    sets = sets,
                    repsRange = repsRange,
                    restSeconds = restSeconds,
                    orderIndex = nextOrder,
                    notes = null
                )
            )
            _state.postValue(buildState(current.plan))
        }
    }

    fun moveExerciseUp(day: PlanDayDetail, exerciseIndex: Int) {
        if (exerciseIndex <= 0) return
        val current = _state.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val exA = day.exercises[exerciseIndex].planExercise
            val exB = day.exercises[exerciseIndex - 1].planExercise
            planExerciseDao.updatePlanExercise(exA.copy(orderIndex = exB.orderIndex))
            planExerciseDao.updatePlanExercise(exB.copy(orderIndex = exA.orderIndex))
            _state.postValue(buildState(current.plan))
        }
    }
}
