package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_logs")
data class AttendanceLogEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val date: String,
    val markedBy: String,
    val createdAt: Long
)