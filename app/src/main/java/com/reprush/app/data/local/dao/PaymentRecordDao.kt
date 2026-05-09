package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.PaymentRecordEntity

@Dao
interface PaymentRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecordEntity)

    @Query("SELECT * FROM payment_records WHERE memberId = :memberId ORDER BY createdAt DESC")
    suspend fun getPaymentsForMember(memberId: String): List<PaymentRecordEntity>

    @Query("SELECT * FROM payment_records ORDER BY createdAt DESC")
    suspend fun getAllPayments(): List<PaymentRecordEntity>

    @Update
    suspend fun updatePayment(payment: PaymentRecordEntity)
}
