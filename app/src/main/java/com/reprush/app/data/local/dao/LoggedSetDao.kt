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

    @Query("""
        SELECT DATE(loggedAt / 1000, 'unixepoch') as workoutDate,
               SUM(weight * reps) as totalVolume,
               COUNT(DISTINCT exerciseId) as exerciseCount
        FROM logged_sets
        WHERE sessionId IN (SELECT id FROM workout_sessions WHERE userId = :userId AND isCompleted = 1)
          AND isWarmup = 0
          AND isCompleted = 1
          AND loggedAt >= :sixMonthsAgoTimestamp
        GROUP BY workoutDate
        ORDER BY workoutDate ASC
    """)
    suspend fun getVolumeWithCountPerDayForHeatmap(userId: String, sixMonthsAgoTimestamp: Long): List<DailyVolumeWithCount>

    @Query("""
        SELECT DATE(loggedAt / 1000, 'unixepoch') as workoutDate,
               MAX(weight) as maxWeight
        FROM logged_sets
        WHERE sessionId IN (SELECT id FROM workout_sessions WHERE userId = :userId AND isCompleted = 1)
          AND isWarmup = 0
          AND isCompleted = 1
          AND exerciseId = :exerciseId
        GROUP BY workoutDate
        ORDER BY workoutDate ASC
    """)
    suspend fun getMaxWeightOverTime(userId: String, exerciseId: String): List<ExerciseMaxWeightEntry>

    @Query("""
        SELECT strftime('%Y-%W', ls.loggedAt / 1000, 'unixepoch') as isoWeek,
               e.primaryMuscle as muscle,
               SUM(ls.weight * ls.reps) as totalVolume
        FROM logged_sets ls
        INNER JOIN exercises e ON ls.exerciseId = e.id
        WHERE ls.sessionId IN (SELECT id FROM workout_sessions WHERE userId = :userId AND isCompleted = 1)
          AND ls.isWarmup = 0
          AND ls.isCompleted = 1
          AND ls.loggedAt >= :twelveWeeksAgoTimestamp
        GROUP BY isoWeek, muscle
        ORDER BY isoWeek ASC
    """)
    suspend fun getWeeklyVolumePerMuscle(userId: String, twelveWeeksAgoTimestamp: Long): List<WeeklyMuscleVolume>

    @Query("""
        SELECT strftime('%Y-%W', loggedAt / 1000, 'unixepoch') as isoWeek,
               SUM(weight * reps) as totalVolume
        FROM logged_sets
        WHERE sessionId IN (SELECT id FROM workout_sessions WHERE userId = :userId AND isCompleted = 1)
          AND isWarmup = 0
          AND isCompleted = 1
          AND loggedAt >= :twelveWeeksAgoTimestamp
        GROUP BY isoWeek
        ORDER BY isoWeek ASC
    """)
    suspend fun getTwelveWeekVolume(userId: String, twelveWeeksAgoTimestamp: Long): List<WeeklyVolume>
}

data class DailyVolume(
    val workoutDate: String,
    val totalVolume: Double
)

data class DailyVolumeWithCount(
    val workoutDate: String,
    val totalVolume: Double,
    val exerciseCount: Int
)

data class ExerciseMaxWeightEntry(
    val workoutDate: String,
    val maxWeight: Double
)

data class WeeklyMuscleVolume(
    val isoWeek: String,
    val muscle: String,
    val totalVolume: Double
)

data class WeeklyVolume(
    val isoWeek: String,
    val totalVolume: Double
)
