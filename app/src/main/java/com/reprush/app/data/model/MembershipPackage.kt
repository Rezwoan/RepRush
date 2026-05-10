package com.reprush.app.data.model

data class MembershipPackage(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val durationDays: Int = 0,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0L
)
