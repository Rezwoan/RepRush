package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val planName: String,
    val goal: String,
    val totalWeeks: Int,
    val daysPerWeek: Int,
    val schemaVersion: Int = 1,
    val isActive: Int = 0,
    val createdAt: Long
)