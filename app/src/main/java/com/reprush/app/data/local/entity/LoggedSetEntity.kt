package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logged_sets")
data class LoggedSetEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isWarmup: Int = 0,
    val isCompleted: Int = 0,
    val isPersonalRecord: Int = 0,
    val loggedAt: Long
)