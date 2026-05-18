package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.PlanExerciseEntity

@Dao
interface PlanExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercise(planExercise: PlanExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(planExercises: List<PlanExerciseEntity>)

    @Query("SELECT * FROM plan_exercises WHERE dayId = :dayId ORDER BY orderIndex ASC")
    suspend fun getExercisesForDay(dayId: String): List<PlanExerciseEntity>

    @Update
    suspend fun updatePlanExercise(planExercise: PlanExerciseEntity)

    @Query("DELETE FROM plan_exercises WHERE id = :id")
    suspend fun deletePlanExercise(id: String)

    @Query("DELETE FROM plan_exercises WHERE dayId = :dayId")
    suspend fun deletePlanExercisesForDay(dayId: String)
}
