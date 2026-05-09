package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val planDayId: String?,
    val startTime: Long,
    val endTime: Long?,
    val notes: String?,
    val totalPoints: Int = 0,
    val isCompleted: Int = 0
)