package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pr_records")
data class PrRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val exerciseId: String,
    val repCount: Int,
    val weight: Double,
    val oneRepMax: Double,
    val achievedAt: Long,
    val sessionId: String
)