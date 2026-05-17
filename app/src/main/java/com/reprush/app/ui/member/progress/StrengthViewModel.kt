package com.reprush.app.ui.member.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.local.dao.ExerciseDao
import com.reprush.app.data.local.dao.LoggedSetDao
import com.reprush.app.data.local.dao.PrRecordDao
import com.reprush.app.data.local.dao.WeeklyVolume
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class LiftCardItem(
    val groupLabel: String,
    val exerciseName: String,
    val currentOneRm: Double,
    val delta30Day: Double?,
    val recentHistory: List<Float>
)

data class StrengthData(
    val score: Double,
    val level: String,
    val liftCount: Int,
    val liftCards: List<LiftCardItem>,
    val weeklyVolume: List<WeeklyVolume>
)

@HiltViewModel
class StrengthViewModel @Inject constructor(
    private val prRecordDao: PrRecordDao,
    private val exerciseDao: ExerciseDao,
    private val loggedSetDao: LoggedSetDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _strengthData = MutableLiveData<StrengthData>()
    val strengthData: LiveData<StrengthData> = _strengthData

    private val keyLifts = listOf("Bench Press", "Squat", "Deadlift")

    fun load() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            var totalScore = 0.0
            var liftCount = 0
            val liftCards = mutableListOf<LiftCardItem>()
            val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)

            for (groupLabel in keyLifts) {
                val matchingExercises = exerciseDao.getExercisesByNameContaining(groupLabel)
                if (matchingExercises.isEmpty()) continue

                // Find the exercise with the best 1RM across all name variants in this group
                var bestOneRm = 0.0
                var bestExerciseName = groupLabel
                var bestExerciseId = ""

                for (exercise in matchingExercises) {
                    val pr = prRecordDao.getBestOneRepMax(uid, exercise.id) ?: continue
                    if (pr.oneRepMax > bestOneRm) {
                        bestOneRm = pr.oneRepMax
                        bestExerciseName = exercise.name
                        bestExerciseId = exercise.id
                    }
                }

                if (bestOneRm <= 0.0) continue

                totalScore += bestOneRm
                liftCount++

                val history = prRecordDao.getPrHistoryForExercise(uid, bestExerciseId)
                val sparkline = history.map { it.oneRepMax.toFloat() }
                val delta = history.filter { it.achievedAt <= thirtyDaysAgo }
                    .lastOrNull()
                    ?.let { bestOneRm - it.oneRepMax }

                liftCards.add(
                    LiftCardItem(
                        groupLabel = groupLabel,
                        exerciseName = bestExerciseName,
                        currentOneRm = bestOneRm,
                        delta30Day = delta,
                        recentHistory = sparkline.takeLast(10)
                    )
                )
            }

            val twelveWeeksAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(84)
            val weeklyVol = loggedSetDao.getTwelveWeekVolume(uid, twelveWeeksAgo)

            _strengthData.postValue(
                StrengthData(
                    score = totalScore,
                    level = strengthLevel(totalScore),
                    liftCount = liftCount,
                    liftCards = liftCards,
                    weeklyVolume = weeklyVol
                )
            )
        }
    }

    private fun strengthLevel(score: Double): String = when {
        score < 200 -> "Beginner"
        score < 400 -> "Intermediate"
        score < 600 -> "Advanced"
        else -> "Elite"
    }
}
