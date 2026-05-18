package com.reprush.app.service

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpiryNotificationService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val thresholds = listOf(7, 3, 0)

    suspend fun checkAndNotify(): Int = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        var totalSent = 0

        for (daysLeft in thresholds) {
            val targetDate = today.plusDays(daysLeft.toLong()).toString()
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "member")
                .whereEqualTo("membershipStatus", "active")
                .whereEqualTo("membershipEndDate", targetDate)
                .get()
                .await()

            for (memberDoc in snapshot.documents) {
                val dedupId = "expiry_${daysLeft}d_${targetDate}_${memberDoc.id}"

                val existing = firestore.collection("notifications")
                    .document(memberDoc.id)
                    .collection("items")
                    .document(dedupId)
                    .get()
                    .await()

                if (existing.exists()) continue

                val message = when (daysLeft) {
                    0 -> "Your membership expires today. Please renew to continue access."
                    1 -> "Your membership expires tomorrow."
                    else -> "Your membership expires in $daysLeft days."
                }

                val notification = hashMapOf(
                    "type" to "expiry_reminder",
                    "title" to "Membership Expiry Reminder",
                    "body" to message,
                    "isRead" to false,
                    "metadata" to "{\"daysLeft\":$daysLeft,\"expiryDate\":\"$targetDate\"}",
                    "createdAt" to System.currentTimeMillis()
                )

                firestore.collection("notifications")
                    .document(memberDoc.id)
                    .collection("items")
                    .document(dedupId)
                    .set(notification)
                    .await()

                totalSent++
            }
        }

        totalSent
    }
}
