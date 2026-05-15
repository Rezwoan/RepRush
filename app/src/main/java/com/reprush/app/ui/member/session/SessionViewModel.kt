package com.reprush.app.ui.member.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.repository.ActiveSessionState
import com.reprush.app.data.repository.Result
import com.reprush.app.data.repository.SessionExercise
import com.reprush.app.data.repository.SessionRepository
import com.reprush.app.data.repository.SessionSet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class SessionState {
    object Idle : SessionState()
    object Active : SessionState()
    object Saving : SessionState()
    data class Saved(val sessionId: String) : SessionState()
    data class Error(val message: String) : SessionState()
}

data class RestTimerData(
    val exerciseName: String,
    val durationSeconds: Int
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _sessionState = MutableLiveData<SessionState>(SessionState.Idle)
    val sessionState: LiveData<SessionState> = _sessionState

    private val _activeSession = MutableLiveData<ActiveSessionState?>()
    val activeSession: LiveData<ActiveSessionState?> = _activeSession

    private val _elapsedTime = MutableLiveData("00:00")
    val elapsedTime: LiveData<String> = _elapsedTime

    private val _restTimerEvent = MutableLiveData<RestTimerData?>()
    val restTimerEvent: LiveData<RestTimerData?> = _restTimerEvent

    private var elapsedJob: Job? = null
    private var autoSaveJob: Job? = null

    fun startFromPlan(planDayId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val exercises = sessionRepository.loadPlanDayExercises(planDayId)
            val sessionId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()
            val state = ActiveSessionState(
                sessionId = sessionId,
                userId = userId,
                planDayId = planDayId,
                startTime = startTime,
                exercises = exercises.toMutableList()
            )
            sessionRepository.startSession(state)
            _activeSession.value = state
            _sessionState.value = SessionState.Active
            startElapsedTimer(startTime)
            startAutoSave()
        }
    }

    fun startBlankSession() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()
            val state = ActiveSessionState(
                sessionId = sessionId,
                userId = userId,
                planDayId = null,
                startTime = startTime,
                exercises = mutableListOf()
            )
            sessionRepository.startSession(state)
            _activeSession.value = state
            _sessionState.value = SessionState.Active
            startElapsedTimer(startTime)
            startAutoSave()
        }
    }

    fun resumeSession(state: ActiveSessionState) {
        _activeSession.value = state
        _sessionState.value = SessionState.Active
        startElapsedTimer(state.startTime)
        startAutoSave()
    }

    fun addExercise(exercise: SessionExercise) {
        val current = _activeSession.value ?: return
        val updated = current.copy(exercises = (current.exercises + exercise).toMutableList())
        _activeSession.value = updated
    }

    fun removeExercise(index: Int) {
        val current = _activeSession.value ?: return
        if (index < 0 || index >= current.exercises.size) return
        val updated = current.exercises.toMutableList().also { it.removeAt(index) }
        _activeSession.value = current.copy(exercises = updated)
    }

    fun addSet(exerciseIndex: Int) {
        val current = _activeSession.value ?: return
        if (exerciseIndex < 0 || exerciseIndex >= current.exercises.size) return
        val exercises = current.exercises.toMutableList()
        val ex = exercises[exerciseIndex]
        val lastSet = ex.sets.lastOrNull()
        val newSet = SessionSet(
            setNumber = ex.sets.size + 1,
            weight = lastSet?.weight ?: 0.0,
            reps = lastSet?.reps ?: 0
        )
        val updatedEx = ex.copy(sets = (ex.sets + newSet).toMutableList())
        exercises[exerciseIndex] = updatedEx
        _activeSession.value = current.copy(exercises = exercises)
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val current = _activeSession.value ?: return
        if (exerciseIndex < 0 || exerciseIndex >= current.exercises.size) return
        val exercises = current.exercises.toMutableList()
        val ex = exercises[exerciseIndex]
        if (setIndex < 0 || setIndex >= ex.sets.size) return
        val updatedSets = ex.sets.toMutableList().also { it.removeAt(setIndex) }
            .mapIndexed { i, s -> s.copy(setNumber = i + 1) }.toMutableList()
        exercises[exerciseIndex] = ex.copy(sets = updatedSets)
        _activeSession.value = current.copy(exercises = exercises)
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: Double) {
        updateSet(exerciseIndex, setIndex) { it.copy(weight = weight) }
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        updateSet(exerciseIndex, setIndex) { it.copy(reps = reps) }
    }

    fun toggleWarmup(exerciseIndex: Int, setIndex: Int) {
        updateSet(exerciseIndex, setIndex) { it.copy(isWarmup = !it.isWarmup) }
    }

    fun completeSet(exerciseIndex: Int, setIndex: Int) {
        val current = _activeSession.value ?: return
        if (exerciseIndex < 0 || exerciseIndex >= current.exercises.size) return
        val exercises = current.exercises.toMutableList()
        val ex = exercises[exerciseIndex]
        if (setIndex < 0 || setIndex >= ex.sets.size) return
        val set = ex.sets[setIndex]
        val updatedSets = ex.sets.toMutableList()
        updatedSets[setIndex] = set.copy(
            isCompleted = true,
            loggedAt = System.currentTimeMillis()
        )
        exercises[exerciseIndex] = ex.copy(sets = updatedSets)
        _activeSession.value = current.copy(exercises = exercises)

        _restTimerEvent.value = RestTimerData(
            exerciseName = ex.exerciseName,
            durationSeconds = ex.plannedRestSeconds
        )
    }

    fun clearRestTimerEvent() {
        _restTimerEvent.value = null
    }

    fun updateNotes(notes: String) {
        val current = _activeSession.value ?: return
        _activeSession.value = current.copy(notes = notes)
    }

    fun finishWorkout() {
        val current = _activeSession.value ?: return
        val userId = auth.currentUser?.uid ?: return
        _sessionState.value = SessionState.Saving
        viewModelScope.launch {
            val endTime = System.currentTimeMillis()
            val result = sessionRepository.finishSession(current, endTime)
            when (result) {
                is Result.Success -> {
                    stopElapsedTimer()
                    stopAutoSave()
                    _sessionState.value = SessionState.Saved(result.data)
                }
                is Result.Error -> {
                    _sessionState.value = SessionState.Error(result.message)
                }
            }
        }
    }

    fun clearSession() {
        stopElapsedTimer()
        stopAutoSave()
        _activeSession.value = null
        _sessionState.value = SessionState.Idle
        _elapsedTime.value = "00:00"
    }

    fun getWorkingSetCount(): Int {
        return _activeSession.value?.exercises?.sumOf { ex ->
            ex.sets.count { it.isCompleted && !it.isWarmup }
        } ?: 0
    }

    fun getTotalVolumeKg(): Double {
        return _activeSession.value?.exercises?.sumOf { ex ->
            ex.sets.filter { it.isCompleted && !it.isWarmup }
                .sumOf { it.weight * it.reps }
        } ?: 0.0
    }

    fun getDurationMs(): Long {
        val state = _activeSession.value ?: return 0L
        return System.currentTimeMillis() - state.startTime
    }

    private fun startElapsedTimer(startTime: Long) {
        elapsedJob?.cancel()
        elapsedJob = viewModelScope.launch {
            while (isActive) {
                val elapsedMs = System.currentTimeMillis() - startTime
                val totalSecs = elapsedMs / 1000
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                _elapsedTime.postValue("%02d:%02d".format(mins, secs))
                delay(1000)
            }
        }
    }

    private fun stopElapsedTimer() {
        elapsedJob?.cancel()
        elapsedJob = null
    }

    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                val state = _activeSession.value ?: continue
                sessionRepository.autoSave(state)
            }
        }
    }

    private fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }

    private fun updateSet(exerciseIndex: Int, setIndex: Int, transform: (SessionSet) -> SessionSet) {
        val current = _activeSession.value ?: return
        if (exerciseIndex < 0 || exerciseIndex >= current.exercises.size) return
        val exercises = current.exercises.toMutableList()
        val ex = exercises[exerciseIndex]
        if (setIndex < 0 || setIndex >= ex.sets.size) return
        val updatedSets = ex.sets.toMutableList()
        updatedSets[setIndex] = transform(updatedSets[setIndex])
        exercises[exerciseIndex] = ex.copy(sets = updatedSets)
        _activeSession.value = current.copy(exercises = exercises)
    }

    override fun onCleared() {
        super.onCleared()
        stopElapsedTimer()
        stopAutoSave()
    }
}
