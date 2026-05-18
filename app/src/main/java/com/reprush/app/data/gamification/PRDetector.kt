package com.reprush.app.data.gamification

import com.reprush.app.data.local.dao.LoggedSetDao
import com.reprush.app.data.local.dao.PrRecordDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PRDetector @Inject constructor(
    private val prRecordDao: PrRecordDao,
    private val loggedSetDao: LoggedSetDao
) {
    suspend fun detectNewPRs(
        userId: String,
        sessionId: String,
        exerciseNames: Map<String, String>
    ): List<NewPR> {
        val workingSets = loggedSetDao.getWorkingSetsForSession(sessionId)
        val newPRs = mutableListOf<NewPR>()

        for (set in workingSets) {
            val existing = prRecordDao.getBestPrForExerciseAtRepCount(userId, set.exerciseId, set.reps)
            val isNewPR = existing == null || set.weight > existing.weight
            if (isNewPR) {
                newPRs.add(
                    NewPR(
                        setId = set.id,
                        exerciseName = exerciseNames[set.exerciseId] ?: "Unknown",
                        exerciseId = set.exerciseId,
                        reps = set.reps,
                        weight = set.weight
                    )
                )
            }
        }
        return newPRs
    }
}
