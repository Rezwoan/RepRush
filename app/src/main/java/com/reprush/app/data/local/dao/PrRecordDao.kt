package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.PrRecordEntity

@Dao
interface PrRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPr(pr: PrRecordEntity)

    @Query("SELECT * FROM pr_records WHERE userId = :userId AND exerciseId = :exerciseId ORDER BY achievedAt DESC")
    suspend fun getPrsForExercise(userId: String, exerciseId: String): List<PrRecordEntity>

    @Query("SELECT * FROM pr_records WHERE userId = :userId AND exerciseId = :exerciseId AND repCount = :repCount ORDER BY weight DESC LIMIT 1")
    suspend fun getBestPrForExerciseAtRepCount(userId: String, exerciseId: String, repCount: Int): PrRecordEntity?

    @Query("SELECT COUNT(*) FROM pr_records WHERE userId = :userId")
    suspend fun getTotalPrCount(userId: String): Int

    @Query("SELECT * FROM pr_records WHERE userId = :userId ORDER BY achievedAt DESC LIMIT :limit")
    suspend fun getRecentPRs(userId: String, limit: Int): List<PrRecordEntity>

    @Query("SELECT * FROM pr_records WHERE userId = :userId AND exerciseId = :exerciseId ORDER BY oneRepMax DESC LIMIT 1")
    suspend fun getBestOneRepMax(userId: String, exerciseId: String): PrRecordEntity?

    @Query("SELECT * FROM pr_records WHERE userId = :userId AND exerciseId = :exerciseId ORDER BY achievedAt ASC")
    suspend fun getPrHistoryForExercise(userId: String, exerciseId: String): List<PrRecordEntity>
}
