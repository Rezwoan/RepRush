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

    // In-memory backing updated silently on every TextWatcher keystroke.
    // LiveData only posts on cascade (focus loss) and structural changes.
    private var sessionData: ActiveSessionState? = null

    private var elapsedJob: Job? = null
    private var autoSaveJob: Job? = null

    private fun publishSession(state: ActiveSessionState) {
        sessionData = state
        _activeSession.value = state
    }

    private fun updateSet(exerciseIndex: Int, setIndex: Int, transform: (SessionSet) -> SessionSet) {
        val current = sessionData ?: return
        if (exerciseIndex < 0 || exerciseIndex >= current.exercises.size) return
        val exercises = current.exercises.toMutableList()
        val ex = exercises[exerciseIndex]
        if (setIndex < 0 || setIndex >= ex.sets.size) return
        val updatedSets = ex.sets.toMutableList()
        updatedSets[setIndex] = transform(updatedSets[setIndex])
        exercises[exerciseIndex] = ex.copy(sets = updatedSets)
        publishSession(current.copy(exercises = exercises))
    }

    private fun updateSetSilent(exerciseIndex: Int, setIndex: Int, transform: (SessionSet) -> SessionSet) {
        val current = sessionData ?: return
        if (exerciseIndex < 0 || exerciseIndex >= current.exercises.size) return
        val exercises = current.exercises.toMutableList()
        val ex = exercises[exerciseIndex]
        if (setIndex < 0 || setIndex >= ex.sets.size) return
        val updatedSets = ex.sets.toMutableList()
        updatedSets[setIndex] = transform(updatedSets[setIndex])
        exercises[exerciseIndex] = ex.copy(sets = updatedSets)
        sessionData = current.copy(exercises = exercises)
    }

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
            publishSession(state)
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
            publishSession(state)
            _sessionState.value = SessionState.Active
            startElapsedTimer(startTime)
            startAutoSave()
        }
    }

    fun resumeSession(state: ActiveSessionState) {
        publishSession(state)
        _sessionState.value = SessionState.Active
        startElapsedTimer(state.startTime)
        startAutoSave()
    }

    fun addExercise(exercise: SessionExercise) {
        val current = sessionData ?: return
        publishSession(current.copy(exercises = (current.exercises + exercise).toMutableList()))
    }

    fun removeExercise(index: Int) {
        val current = sessionData ?: return
        if (index < 0 || index >= current.exercises.size) return
        val updated = current.exercises.toMutableList().also { it.removeAt(index) }
        publishSession(current.copy(exercises = updated))
    }

    fun addSet(exerciseIndex: Int) {
        val current = sessionData ?: return
        if (exerciseIndex < 0 || exerciseIndex >= current.exercises.size) return
        val exercises = current.exercises.toMutableList()
        val ex = exercises[exerciseIndex]
        val lastSet = ex.sets.lastOrNull()
        val newSet = SessionSet(
            setNumber = ex.sets.size + 1,
            weight = lastSet?.weight ?: 0.0,
            reps = lastSet?.reps ?: 0,
            isBodyweight = lastSet?.isBodyweight ?: false
        )
        val updatedEx = ex.copy(sets = (ex.sets + newSet).toMutableList())
        exercises[exerciseIndex] = updatedEx
        publishSession(current.copy(exercises = exercises))
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val current = sessionData ?: return
        if (exerciseIndex < 0 || exerciseIndex >= current.exercises.size) return
        val exercises = current.exercises.toMutableList()
        val ex = exercises[exerciseIndex]
        if (setIndex < 0 || setIndex >= ex.sets.size) return
        val updatedSets = ex.sets.toMutableList().also { it.removeAt(setIndex) }
            .mapIndexed { i, s -> s.copy(setNumber = i + 1) }.toMutableList()
        exercises[exerciseIndex] = ex.copy(sets = updatedSets)
        publishSession(current.copy(exercises = exercises))
    }

    // Called from TextWatcher — silent, no LiveData post, marks field as user-edited
    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: Double) {
        updateSetSilent(exerciseIndex, setIndex) { it.copy(weight = weight, weightUserEdited = true) }
    }

    // Called from TextWatcher — silent, no LiveData post, marks field as user-edited
    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        updateSetSilent(exerciseIndex, setIndex) { it.copy(reps = reps, repsUserEdited = true) }
    }

    // Called on weight field focus loss — cascades to unedited, non-completed, non-BW sibling sets
    fun cascadeSetWeight(exerciseIndex: Int, setIndex: Int) {
        val current = sessionData ?: return
        if (exerciseIndex < 0 || exerciseIndex >= current.exercises.size) return
        val ex = current.exercises[exerciseIndex]
        if (setIndex < 0 || setIndex >= ex.sets.size) return
        val sourceWeight = ex.sets[setIndex].weight
        if (sourceWeight <= 0.0) return
        val exercises = current.exercises.toMutableList()
        val updatedSets = ex.sets.toMutableList()
        updatedSets.forEachIndexed { i, set ->
            if (i != setIndex && !set.isCompleted && !set.weightUserEdited && !set.isBodyweight) {
                updatedSets[i] = set.copy(weight = sourceWeight)
            }
        }
        exercises[exerciseIndex] = ex.copy(sets = updatedSets)
        publishSession(current.copy(exercises = exercises))
    }

    // Called on reps field focus loss — cascades to unedited, non-completed sibling sets
    fun cascadeSetReps(exerciseIndex: Int, setIndex: Int) {
        val current = sessionData ?: return
        if (exerciseIndex < 0 || exerciseIndex >= current.exercises.size) return
        val ex = current.exercises[exerciseIndex]
        if (setIndex < 0 || setIndex >= ex.sets.size) return
        val sourceReps = ex.sets[setIndex].reps
        if (sourceReps <= 0) return
        val exercises = current.exercises.toMutableList()
        val updatedSets = ex.sets.toMutableList()
        updatedSets.forEachIndexed { i, set ->
            if (i != setIndex && !set.isCompleted && !set.repsUserEdited) {
                updatedSets[i] = set.copy(reps = sourceReps)
            }
        }
        exercises[exerciseIndex] = ex.copy(sets = updatedSets)
        publishSession(current.copy(exercises = exercises))
    }

    fun toggleWarmup(exerciseIndex: Int, setIndex: Int) {
        updateSet(exerciseIndex, setIndex) { it.copy(isWarmup = !it.isWarmup) }
    }

    fun toggleBodyweight(exerciseIndex: Int, setIndex: Int) {
        val current = sessionData ?: return
        if (exerciseIndex < 0 || exerciseIndex >= current.exercises.size) return
        val exercises = current.exercises.toMutableList()
        val ex = exercises[exerciseIndex]
        if (setIndex < 0 || setIndex >= ex.sets.size) return
        val set = ex.sets[setIndex]
        val nowBw = !set.isBodyweight
        val updatedSets = ex.sets.toMutableList()
        updatedSets[setIndex] = set.copy(isBodyweight = nowBw, weight = if (nowBw) 0.0 else set.weight)
        exercises[exerciseIndex] = ex.copy(sets = updatedSets)
        publishSession(current.copy(exercises = exercises))
    }

    fun completeSet(exerciseIndex: Int, setIndex: Int) {
        val current = sessionData ?: return
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
        publishSession(current.copy(exercises = exercises))

        val restSeconds = if (ex.plannedRestSeconds > 0) ex.plannedRestSeconds else 60
        _restTimerEvent.value = RestTimerData(
            exerciseName = ex.exerciseName,
            durationSeconds = restSeconds
        )
    }

    fun clearRestTimerEvent() {
        _restTimerEvent.value = null
    }

    fun updateNotes(notes: String) {
        val current = sessionData ?: return
        sessionData = current.copy(notes = notes)
    }

    fun finishWorkout() {
        val current = sessionData ?: return
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

    fun discardCurrentSession(onComplete: () -> Unit) {
        val current = sessionData ?: run { clearSession(); onComplete(); return }
        viewModelScope.launch {
            sessionRepository.discardIncompleteSession(current.sessionId)
            clearSession()
            onComplete()
        }
    }

    fun clearSession() {
        stopElapsedTimer()
        stopAutoSave()
        sessionData = null
        _activeSession.value = null
        _sessionState.value = SessionState.Idle
        _elapsedTime.value = "00:00"
    }

    fun getWorkingSetCount(): Int {
        return sessionData?.exercises?.sumOf { ex ->
            ex.sets.count { it.isCompleted && !it.isWarmup }
        } ?: 0
    }

    fun getTotalVolumeKg(): Double {
        return sessionData?.exercises?.sumOf { ex ->
            ex.sets.filter { it.isCompleted && !it.isWarmup }
                .sumOf { it.weight * it.reps }
        } ?: 0.0
    }

    fun getDurationMs(): Long {
        val state = sessionData ?: return 0L
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
                val state = sessionData ?: continue
                sessionRepository.autoSave(state)
            }
        }
    }

    private fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopElapsedTimer()
        stopAutoSave()
    }
}
