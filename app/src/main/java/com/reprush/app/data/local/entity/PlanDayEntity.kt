package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_days")
data class PlanDayEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val dayNumber: Int,
    val dayLabel: String
)