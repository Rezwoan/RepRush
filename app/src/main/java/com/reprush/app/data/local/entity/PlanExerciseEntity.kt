package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_exercises")
data class PlanExerciseEntity(
    @PrimaryKey val id: String,
    val dayId: String,
    val exerciseId: String,
    val sets: Int,
    val repsRange: String,
    val restSeconds: Int,
    val orderIndex: Int,
    val notes: String?
)