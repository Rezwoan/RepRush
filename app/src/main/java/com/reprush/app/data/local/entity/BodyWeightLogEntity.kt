package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_weight_logs")
data class BodyWeightLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val weightKg: Double,
    val loggedDate: String,
    val loggedAt: Long
)