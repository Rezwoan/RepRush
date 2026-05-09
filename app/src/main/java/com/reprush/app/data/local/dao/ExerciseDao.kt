package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.ExerciseEntity

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%'")
    suspend fun searchExercises(query: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE primaryMuscle = :muscle")
    suspend fun getExercisesByMuscle(muscle: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE equipment = :equipment")
    suspend fun getExercisesByEquipment(equipment: String): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int
}
