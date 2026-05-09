package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey val userId: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastWorkoutDate: String?
)