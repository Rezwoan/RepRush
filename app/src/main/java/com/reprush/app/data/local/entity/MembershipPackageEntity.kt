package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "membership_packages")
data class MembershipPackageEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double,
    val durationDays: Int,
    val description: String?,
    val isActive: Int = 1,
    val createdAt: Long
)