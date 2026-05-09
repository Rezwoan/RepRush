package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.WorkoutPlanEntity

@Dao
interface WorkoutPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlanEntity)

    @Query("SELECT * FROM workout_plans WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getPlansForUser(userId: String): List<WorkoutPlanEntity>

    @Query("SELECT * FROM workout_plans WHERE userId = :userId AND isActive = 1 LIMIT 1")
    suspend fun getActivePlan(userId: String): WorkoutPlanEntity?

    @Query("UPDATE workout_plans SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateAllPlans(userId: String)

    @Update
    suspend fun updatePlan(plan: WorkoutPlanEntity)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlanEntity)
}
