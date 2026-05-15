package com.reprush.app.data.gamification

import com.reprush.app.data.local.dao.AchievementDao
import com.reprush.app.data.local.dao.LoggedSetDao
import com.reprush.app.data.local.dao.PlanExerciseDao
import com.reprush.app.data.local.dao.PrRecordDao
import com.reprush.app.data.local.dao.WorkoutPlanDao
import com.reprush.app.data.local.dao.WorkoutSessionDao
import com.reprush.app.data.local.entity.AchievementEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementChecker @Inject constructor(
    private val achievementDao: AchievementDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val prRecordDao: PrRecordDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val loggedSetDao: LoggedSetDao,
    private val planExerciseDao: PlanExerciseDao
) {
    suspend fun checkAndUnlock(
        userId: String,
        sessionId: String,
        currentStreak: Int,
        sessionExerciseNames: Map<String, String>,
        planDayId: String?
    ): List<String> {
        val newlyUnlocked = mutableListOf<String>()

        val totalSessions = workoutSessionDao.getCompletedSessionCount(userId)
        val totalPRs = prRecordDao.getTotalPrCount(userId)
        val planCount = workoutPlanDao.getPlanCount(userId)
        val previousSession = workoutSessionDao.getLastCompletedSessionBefore(userId, sessionId)
        val workingSets = loggedSetDao.getWorkingSetsForSession(sessionId)

        val checks = listOf(
            "first_rep" to (totalSessions >= 1),
            "on_a_roll" to (currentStreak >= 7),
            "unstoppable" to (currentStreak >= 30),
            "century" to (totalSessions >= 100),
            "pr_machine" to (totalPRs >= 10),
            "plan_master" to (planCount >= 1),
            "heavy_bench" to workingSets.any { set ->
                set.weight >= 100.0 &&
                    (sessionExerciseNames[set.exerciseId] ?: "").contains("bench", ignoreCase = true)
            },
            "heavy_squat" to workingSets.any { set ->
                set.weight >= 100.0 &&
                    (sessionExerciseNames[set.exerciseId] ?: "").contains("squat", ignoreCase = true)
            },
            "heavy_deadlift" to workingSets.any { set ->
                set.weight >= 100.0 &&
                    (sessionExerciseNames[set.exerciseId] ?: "").contains("deadlift", ignoreCase = true)
            },
            "comeback" to run {
                if (previousSession?.endTime == null) false
                else (System.currentTimeMillis() - previousSession.endTime) / 86400000L >= 14
            },
            "full_house" to (planDayId != null && checkFullHouse(sessionId, planDayId))
        )

        for ((badgeId, condition) in checks) {
            if (!condition) continue
            val existing = achievementDao.getAchievementByBadgeId(userId, badgeId)
            if (existing == null) {
                achievementDao.insertAchievement(
                    AchievementEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        badgeId = badgeId,
                        unlockedAt = System.currentTimeMillis()
                    )
                )
                newlyUnlocked.add(badgeId)
            }
        }

        return newlyUnlocked
    }

    private suspend fun checkFullHouse(sessionId: String, planDayId: String): Boolean {
        val planExercises = planExerciseDao.getExercisesForDay(planDayId)
        if (planExercises.isEmpty()) return false
        val completedSets = loggedSetDao.getSetsForSession(sessionId)
            .filter { it.isCompleted == 1 && it.isWarmup == 0 }
        return planExercises.all { pe ->
            completedSets.count { it.exerciseId == pe.exerciseId } >= pe.sets
        }
    }
}
