package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.BodyWeightLogEntity

@Dao
interface BodyWeightLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: BodyWeightLogEntity)

    @Query("SELECT * FROM body_weight_logs WHERE userId = :userId ORDER BY loggedDate ASC")
    suspend fun getWeightLogsForUser(userId: String): List<BodyWeightLogEntity>

    @Query("SELECT * FROM body_weight_logs WHERE userId = :userId AND loggedDate = :date LIMIT 1")
    suspend fun getWeightLogForDate(userId: String, date: String): BodyWeightLogEntity?

    @Update
    suspend fun updateWeightLog(log: BodyWeightLogEntity)
}
