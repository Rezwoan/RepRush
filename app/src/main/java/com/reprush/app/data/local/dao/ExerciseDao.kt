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

    @Query("SELECT * FROM exercises WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getExerciseByName(name: String): ExerciseEntity?

    @Query("""
        SELECT * FROM exercises
        WHERE (:query = '' OR LOWER(name) LIKE '%' || LOWER(:query) || '%')
        AND (:muscle = '' OR LOWER(primaryMuscle) = LOWER(:muscle))
        AND (:equipment = '' OR LOWER(equipment) = LOWER(:equipment))
        ORDER BY name ASC
    """)
    suspend fun getExercisesFiltered(query: String, muscle: String, equipment: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE equipment IN (:equipmentList) ORDER BY name ASC")
    suspend fun getExercisesForEquipmentList(equipmentList: List<String>): List<ExerciseEntity>

    @Query("UPDATE exercises SET imageUrl = :imageUrl WHERE id = :exerciseId")
    suspend fun updateImageUrl(exerciseId: String, imageUrl: String)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int
}
