package com.reprush.app.ui.member.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.local.dao.ExerciseDao
import com.reprush.app.data.local.dao.LoggedSetDao
import com.reprush.app.data.local.dao.PlanDayDao
import com.reprush.app.data.local.dao.WorkoutSessionDao
import com.reprush.app.data.local.entity.WorkoutSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class SessionHeader(
    val dateFormatted: String,
    val dayLabel: String,
    val durationText: String,
    val volumeKg: Double,
    val totalPoints: Int
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val workoutSessionDao: WorkoutSessionDao,
    private val loggedSetDao: LoggedSetDao,
    private val exerciseDao: ExerciseDao,
    private val planDayDao: PlanDayDao
) : ViewModel() {

    private val _header = MutableLiveData<SessionHeader>()
    val header: LiveData<SessionHeader> = _header

    private val _exerciseGroups = MutableLiveData<List<ExerciseSetGroup>>()
    val exerciseGroups: LiveData<List<ExerciseSetGroup>> = _exerciseGroups

    fun load(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = workoutSessionDao.getSessionById(sessionId) ?: return@launch
            val sets = loggedSetDao.getSetsForSession(sessionId)

            val volume = sets.filter { it.isWarmup == 0 && it.isCompleted == 1 }
                .sumOf { it.weight * it.reps }

            val dayLabel = session.planDayId
                ?.takeIf { it.isNotBlank() }
                ?.let { planDayDao.getPlanDayById(it)?.dayLabel }
                ?: "Free Session"

            val duration = session.endTime?.let { (it - session.startTime) / 60000L } ?: 0L
            val durationText = when {
                duration >= 60 -> "${duration / 60}h ${duration % 60}min"
                duration > 0 -> "${duration}min"
                else -> "—"
            }

            val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
            val date = Instant.ofEpochMilli(session.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            _header.postValue(
                SessionHeader(
                    dateFormatted = date.format(dateFmt),
                    dayLabel = dayLabel,
                    durationText = durationText,
                    volumeKg = volume,
                    totalPoints = session.totalPoints
                )
            )

            val grouped = sets.groupBy { it.exerciseId }
                .mapNotNull { (exerciseId, exSets) ->
                    val exercise = exerciseDao.getExerciseById(exerciseId) ?: return@mapNotNull null
                    val setDisplays = exSets.sortedBy { it.setNumber }.map { s ->
                        SetDisplay(
                            setNumber = s.setNumber,
                            weight = s.weight,
                            reps = s.reps,
                            isWarmup = s.isWarmup == 1,
                            isPR = s.isPersonalRecord == 1
                        )
                    }
                    ExerciseSetGroup(exercise.name, setDisplays)
                }
            _exerciseGroups.postValue(grouped)
        }
    }
}
