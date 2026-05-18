package com.reprush.app.ui.member.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.local.dao.ExerciseDao
import com.reprush.app.data.local.dao.LoggedSetDao
import com.reprush.app.data.local.dao.PlanDayDao
import com.reprush.app.data.local.dao.WeeklyMuscleVolume
import com.reprush.app.data.local.dao.WorkoutSessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class WorkoutHistoryItem(
    val sessionId: String,
    val dateFormatted: String,
    val dayLabel: String,
    val durationText: String,
    val volumeKg: Double,
    val totalPoints: Int
)

data class MuscleVolumeEntry(val muscle: String, val totalVolume: Double)

@HiltViewModel
class ProgressOverviewViewModel @Inject constructor(
    private val workoutSessionDao: WorkoutSessionDao,
    private val loggedSetDao: LoggedSetDao,
    private val planDayDao: PlanDayDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _workoutHistory = MutableLiveData<List<WorkoutHistoryItem>>()
    val workoutHistory: LiveData<List<WorkoutHistoryItem>> = _workoutHistory

    private val _weeklyMuscleVolume = MutableLiveData<List<MuscleVolumeEntry>>()
    val weeklyMuscleVolume: LiveData<List<MuscleVolumeEntry>> = _weeklyMuscleVolume

    fun load() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            loadHistory(uid)
            loadMuscleVolume(uid)
        }
    }

    private suspend fun loadHistory(uid: String) {
        val sessions = workoutSessionDao.getSessionsForUser(uid)
            .filter { it.isCompleted == 1 }
            .take(30)

        val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        val items = sessions.map { session ->
            val sets = loggedSetDao.getWorkingSetsForSession(session.id)
            val volume = sets.sumOf { it.weight * it.reps }

            val dayLabel = session.planDayId
                ?.takeIf { it.isNotBlank() }
                ?.let { planDayDao.getPlanDayById(it)?.dayLabel }
                ?: "Free Session"

            val duration = session.endTime
                ?.let { end -> (end - session.startTime) / 60000L }
                ?: 0L
            val durationText = when {
                duration >= 60 -> "${duration / 60}h ${duration % 60}min"
                duration > 0 -> "${duration}min"
                else -> "—"
            }

            val date = Instant.ofEpochMilli(session.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            WorkoutHistoryItem(
                sessionId = session.id,
                dateFormatted = date.format(dateFmt),
                dayLabel = dayLabel,
                durationText = durationText,
                volumeKg = volume,
                totalPoints = session.totalPoints
            )
        }

        _workoutHistory.postValue(items)
    }

    private suspend fun loadMuscleVolume(uid: String) {
        val twelveWeeksAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(84)
        val raw: List<WeeklyMuscleVolume> =
            loggedSetDao.getWeeklyVolumePerMuscle(uid, twelveWeeksAgo)

        val aggregated = raw.groupBy { it.muscle }
            .map { (muscle, entries) ->
                MuscleVolumeEntry(muscle, entries.sumOf { it.totalVolume })
            }
            .sortedByDescending { it.totalVolume }
            .take(7)

        _weeklyMuscleVolume.postValue(aggregated)
    }
}
