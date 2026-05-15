package com.reprush.app.ui.member.postsession

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.gamification.AchievementChecker
import com.reprush.app.data.gamification.PRDetector
import com.reprush.app.data.gamification.PointsCalculator
import com.reprush.app.data.gamification.PostWorkoutResult
import com.reprush.app.data.gamification.StreakCalculator
import com.reprush.app.data.local.dao.ExerciseDao
import com.reprush.app.data.local.dao.LoggedSetDao
import com.reprush.app.data.local.dao.PrRecordDao
import com.reprush.app.data.local.dao.StreakDao
import com.reprush.app.data.local.dao.WorkoutSessionDao
import com.reprush.app.data.repository.GameRepository
import com.reprush.app.data.repository.PRRecordRepository
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostWorkoutViewModel @Inject constructor(
    private val prDetector: PRDetector,
    private val prRecordRepository: PRRecordRepository,
    private val pointsCalculator: PointsCalculator,
    private val streakCalculator: StreakCalculator,
    private val achievementChecker: AchievementChecker,
    private val gameRepository: GameRepository,
    private val workoutSessionDao: WorkoutSessionDao,
    private val loggedSetDao: LoggedSetDao,
    private val exerciseDao: ExerciseDao,
    private val prRecordDao: PrRecordDao,
    private val streakDao: StreakDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _result = MutableLiveData<Result<PostWorkoutResult>?>()
    val result: LiveData<Result<PostWorkoutResult>?> = _result

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    fun runPipeline(sessionId: String) {
        val uid = auth.currentUser?.uid ?: run {
            _result.value = Result.Error("User not authenticated")
            _isLoading.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val session = workoutSessionDao.getSessionById(sessionId)
                    ?: throw Exception("Session not found")

                val durationMs = (session.endTime ?: System.currentTimeMillis()) - session.startTime
                val durationMinutes = durationMs / 60000L

                // Build exerciseId → name map for this session
                val workingSets = loggedSetDao.getWorkingSetsForSession(sessionId)
                val exerciseIds = workingSets.map { it.exerciseId }.distinct()
                val exerciseNames = exerciseIds.associateWith { id ->
                    exerciseDao.getExerciseById(id)?.name ?: "Unknown"
                }

                // Step 1: Detect PRs
                val newPRs = prDetector.detectNewPRs(uid, sessionId, exerciseNames)

                // Step 2: Save PRs to Room
                if (newPRs.isNotEmpty()) {
                    prRecordRepository.savePRs(uid, sessionId, newPRs)
                }

                // Get current streak before updating (for points bonus)
                val currentStreakBeforeUpdate = streakDao.getStreakForUser(uid)?.currentStreak ?: 0

                // Step 3: Calculate points
                val exerciseCount = exerciseIds.size
                val workingSetCount = workingSets.size
                val breakdown = pointsCalculator.calculate(
                    durationMinutes = durationMinutes,
                    exerciseCount = exerciseCount,
                    workingSetCount = workingSetCount,
                    prCount = newPRs.size,
                    currentStreak = currentStreakBeforeUpdate
                )

                // Step 4: Persist points on session
                workoutSessionDao.updateTotalPoints(sessionId, breakdown.totalPoints)

                // Step 5: Update streak
                val streakUpdate = streakCalculator.updateStreak(
                    userId = uid,
                    sessionEndTime = session.endTime ?: System.currentTimeMillis(),
                    isPlanSession = session.planDayId != null
                )

                // Step 6: Check achievements
                val newAchievements = achievementChecker.checkAndUnlock(
                    userId = uid,
                    sessionId = sessionId,
                    currentStreak = streakUpdate.currentStreak,
                    sessionExerciseNames = exerciseNames,
                    planDayId = session.planDayId
                )

                // Steps 7 & 8: Firestore writes (best-effort — don't fail pipeline)
                val totalSessions = workoutSessionDao.getCompletedSessionCount(uid)
                val totalPRsCount = prRecordDao.getTotalPrCount(uid)
                val user = auth.currentUser

                gameRepository.writeLeaderboardEntry(
                    uid = uid,
                    displayName = user?.displayName ?: "",
                    photoUrl = user?.photoUrl?.toString(),
                    pointsToAdd = breakdown.totalPoints,
                    totalPRs = totalPRsCount
                )

                gameRepository.updateUserDoc(
                    uid = uid,
                    pointsToAdd = breakdown.totalPoints,
                    totalWorkouts = totalSessions,
                    totalPRs = totalPRsCount,
                    currentStreak = streakUpdate.currentStreak,
                    longestStreak = streakUpdate.longestStreak
                )

                _result.postValue(
                    Result.Success(
                        PostWorkoutResult(
                            sessionId = sessionId,
                            pointsBreakdown = breakdown,
                            newPRs = newPRs,
                            newAchievements = newAchievements,
                            streakUpdate = streakUpdate
                        )
                    )
                )
            } catch (e: Exception) {
                _result.postValue(Result.Error(e.message ?: "Pipeline failed"))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun clearResult() {
        _result.value = null
    }
}
