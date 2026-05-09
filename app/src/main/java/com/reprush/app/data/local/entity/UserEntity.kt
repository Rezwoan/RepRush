package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val role: String,
    val fitnessLevel: String?,
    val primaryGoal: String?,
    val availableEquipment: String?,
    val injuries: String?,
    val membershipStatus: String = "pending",
    val packageId: String?,
    val membershipStartDate: String?,
    val membershipEndDate: String?,
    val onboardingComplete: Int = 0,
    val createdAt: Long
)