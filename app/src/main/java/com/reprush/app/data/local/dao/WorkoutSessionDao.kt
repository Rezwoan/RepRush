package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.WorkoutSessionEntity

@Dao
interface WorkoutSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getSessionsForUser(userId: String): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND isCompleted = 0 LIMIT 1")
    suspend fun getIncompleteSession(userId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): WorkoutSessionEntity?

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)
}
