package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.StreakEntity

@Dao
interface StreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: StreakEntity)

    @Query("SELECT * FROM streaks WHERE userId = :userId")
    suspend fun getStreakForUser(userId: String): StreakEntity?

    @Update
    suspend fun updateStreak(streak: StreakEntity)
}
