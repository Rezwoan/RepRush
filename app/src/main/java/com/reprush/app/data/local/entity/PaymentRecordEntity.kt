package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_records")
data class PaymentRecordEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val packageId: String,
    val amount: Double,
    val paymentMethod: String,
    val paymentDate: String,
    val periodStart: String,
    val periodEnd: String,
    val isVoided: Int = 0,
    val voidReason: String?,
    val recordedBy: String,
    val createdAt: Long
)