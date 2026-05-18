package com.reprush.app.data.repository

import androidx.room.withTransaction
import com.reprush.app.data.gamification.NewPR
import com.reprush.app.data.local.AppDatabase
import com.reprush.app.data.local.dao.LoggedSetDao
import com.reprush.app.data.local.dao.PrRecordDao
import com.reprush.app.data.local.entity.PrRecordEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PRRecordRepository @Inject constructor(
    private val db: AppDatabase,
    private val prRecordDao: PrRecordDao,
    private val loggedSetDao: LoggedSetDao
) {
    suspend fun savePRs(userId: String, sessionId: String, newPRs: List<NewPR>) {
        db.withTransaction {
            for (pr in newPRs) {
                val oneRepMax = pr.weight * (1.0 + pr.reps / 30.0)
                prRecordDao.insertPr(
                    PrRecordEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        exerciseId = pr.exerciseId,
                        repCount = pr.reps,
                        weight = pr.weight,
                        oneRepMax = oneRepMax,
                        achievedAt = System.currentTimeMillis(),
                        sessionId = sessionId
                    )
                )
                loggedSetDao.updatePersonalRecordFlag(pr.setId, 1)
            }
        }
    }
}
