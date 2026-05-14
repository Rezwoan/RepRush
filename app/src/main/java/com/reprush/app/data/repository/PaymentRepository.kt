package com.reprush.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

data class PaymentRecord(
    val id: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val packageId: String = "",
    val packageName: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "",
    val paymentDate: String = "",
    val periodStart: String = "",
    val periodEnd: String = "",
    val isVoided: Boolean = false,
    val voidReason: String? = null,
    val recordedBy: String = "",
    val createdAt: Long = 0L
)

@Singleton
class PaymentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val paymentsCollection = firestore.collection("payments")

    suspend fun recordPayment(
        memberId: String,
        packageId: String,
        amount: Double,
        paymentMethod: String,
        paymentDate: String,
        periodStart: String,
        periodEnd: String,
        recordedBy: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val paymentId = paymentsCollection.document().id
            val paymentData = hashMapOf(
                "memberId" to memberId,
                "packageId" to packageId,
                "amount" to amount,
                "paymentMethod" to paymentMethod,
                "paymentDate" to paymentDate,
                "periodStart" to periodStart,
                "periodEnd" to periodEnd,
                "isVoided" to false,
                "voidReason" to null,
                "recordedBy" to recordedBy,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(memberId)
                transaction.set(paymentsCollection.document(paymentId), paymentData)
                transaction.update(userRef, mapOf(
                    "membershipStartDate" to periodStart,
                    "membershipEndDate" to periodEnd,
                    "membershipStatus" to "active"
                ))
            }.await()

            Result.Success(paymentId)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to record payment")
        }
    }

    suspend fun writeReceiptNotification(
        memberId: String,
        paymentId: String,
        memberName: String,
        packageName: String,
        amount: Double,
        periodStart: String,
        periodEnd: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val notificationId = java.util.UUID.randomUUID().toString()
            val notification = hashMapOf(
                "type" to "receipt",
                "title" to "Payment Receipt",
                "body" to "Payment of ৳${"%.2f".format(amount)} received for $packageName ($periodStart to $periodEnd). Receipt #$paymentId",
                "isRead" to false,
                "metadata" to "{\"paymentId\":\"$paymentId\",\"amount\":$amount,\"packageName\":\"$packageName\"}",
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("notifications")
                .document(memberId)
                .collection("items")
                .document(notificationId)
                .set(notification)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to write receipt notification")
        }
    }

    suspend fun voidPayment(paymentId: String, voidReason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            paymentsCollection.document(paymentId).update(
                mapOf(
                    "isVoided" to true,
                    "voidReason" to voidReason
                )
            ).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to void payment")
        }
    }

    suspend fun getPaymentsForMember(memberId: String): Result<List<PaymentRecord>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = paymentsCollection
                .whereEqualTo("memberId", memberId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val payments = snapshot.documents.map { doc -> docToPaymentRecord(doc) }
            Result.Success(payments)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch payments")
        }
    }

    suspend fun getPaymentLog(
        startDate: String? = null,
        endDate: String? = null,
        memberId: String? = null
    ): Result<List<PaymentRecord>> = withContext(Dispatchers.IO) {
        try {
            var query: Query = paymentsCollection.orderBy("createdAt", Query.Direction.DESCENDING)

            if (memberId != null) {
                query = paymentsCollection
                    .whereEqualTo("memberId", memberId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
            }

            val snapshot = query.get().await()
            var payments = snapshot.documents.map { doc -> docToPaymentRecord(doc) }

            if (startDate != null) {
                payments = payments.filter { it.paymentDate >= startDate }
            }
            if (endDate != null) {
                payments = payments.filter { it.paymentDate <= endDate }
            }

            Result.Success(payments)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch payment log")
        }
    }

    suspend fun getPaymentById(paymentId: String): Result<PaymentRecord> = withContext(Dispatchers.IO) {
        try {
            val doc = paymentsCollection.document(paymentId).get().await()
            if (!doc.exists()) return@withContext Result.Error("Payment not found")
            Result.Success(docToPaymentRecord(doc))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch payment")
        }
    }

    suspend fun getMonthlyRevenue(yearMonth: YearMonth): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val startDate = yearMonth.atDay(1).toString()
            val endDate = yearMonth.atEndOfMonth().toString()
            val snapshot = paymentsCollection
                .whereGreaterThanOrEqualTo("paymentDate", startDate)
                .whereLessThanOrEqualTo("paymentDate", endDate)
                .get()
                .await()
            val total = snapshot.documents
                .filter { doc -> doc.getBoolean("isVoided") != true }
                .sumOf { doc -> doc.getDouble("amount") ?: 0.0 }
            Result.Success(total)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to compute monthly revenue")
        }
    }

    suspend fun getYearlyRevenue(year: Int): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val startDate = LocalDate.of(year, 1, 1).toString()
            val endDate = LocalDate.of(year, 12, 31).toString()
            val snapshot = paymentsCollection
                .whereGreaterThanOrEqualTo("paymentDate", startDate)
                .whereLessThanOrEqualTo("paymentDate", endDate)
                .get()
                .await()
            val total = snapshot.documents
                .filter { doc -> doc.getBoolean("isVoided") != true }
                .sumOf { doc -> doc.getDouble("amount") ?: 0.0 }
            Result.Success(total)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to compute yearly revenue")
        }
    }

    private fun docToPaymentRecord(doc: com.google.firebase.firestore.DocumentSnapshot): PaymentRecord {
        return PaymentRecord(
            id = doc.id,
            memberId = doc.getString("memberId") ?: "",
            packageId = doc.getString("packageId") ?: "",
            amount = doc.getDouble("amount") ?: 0.0,
            paymentMethod = doc.getString("paymentMethod") ?: "",
            paymentDate = doc.getString("paymentDate") ?: "",
            periodStart = doc.getString("periodStart") ?: "",
            periodEnd = doc.getString("periodEnd") ?: "",
            isVoided = doc.getBoolean("isVoided") ?: false,
            voidReason = doc.getString("voidReason"),
            recordedBy = doc.getString("recordedBy") ?: "",
            createdAt = doc.getLong("createdAt") ?: 0L
        )
    }
}
