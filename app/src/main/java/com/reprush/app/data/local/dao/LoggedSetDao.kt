package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.LoggedSetEntity

@Dao
interface LoggedSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: LoggedSetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<LoggedSetEntity>)

    @Query("SELECT * FROM logged_sets WHERE sessionId = :sessionId")
    suspend fun getSetsForSession(sessionId: String): List<LoggedSetEntity>

    @Query("""
        SELECT DATE(loggedAt / 1000, 'unixepoch') as workoutDate,
               SUM(weight * reps) as totalVolume
        FROM logged_sets
        WHERE sessionId IN (SELECT id FROM workout_sessions WHERE userId = :userId AND isCompleted = 1)
          AND isWarmup = 0
          AND isCompleted = 1
          AND loggedAt >= :sixMonthsAgoTimestamp
        GROUP BY workoutDate
        ORDER BY workoutDate ASC
    """)
    suspend fun getVolumePerDayForHeatmap(userId: String, sixMonthsAgoTimestamp: Long): List<DailyVolume>

    @Update
    suspend fun updateSet(set: LoggedSetEntity)

    @Query("SELECT * FROM logged_sets WHERE sessionId = :sessionId AND isWarmup = 0 AND isCompleted = 1")
    suspend fun getWorkingSetsForSession(sessionId: String): List<LoggedSetEntity>

    @Query("UPDATE logged_sets SET isPersonalRecord = :flag WHERE id = :setId")
    suspend fun updatePersonalRecordFlag(setId: String, flag: Int)

    @Query("DELETE FROM logged_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsForSession(sessionId: String)
}

data class DailyVolume(
    val workoutDate: String,
    val totalVolume: Double
)
