package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.AchievementEntity

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY unlockedAt DESC")
    suspend fun getAchievementsForUser(userId: String): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND badgeId = :badgeId LIMIT 1")
    suspend fun getAchievementByBadgeId(userId: String, badgeId: String): AchievementEntity?
}
