package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val wgerId: Int?,
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: String?,
    val equipment: String,
    val category: String,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val muscleImageUrl: String?,
    val isCustom: Int = 0,
    val isVerified: Int = 1
)