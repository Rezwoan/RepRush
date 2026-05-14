package com.reprush.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class GymSettings(
    val autoSuspensionEnabled: Boolean = false,
    val gracePeriodDays: Int = 3
)

@Singleton
class GymSettingsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val settingsDoc = firestore.collection("settings").document("gym")

    suspend fun getSettings(): Result<GymSettings> = withContext(Dispatchers.IO) {
        try {
            val doc = settingsDoc.get().await()
            if (!doc.exists()) {
                return@withContext Result.Success(GymSettings())
            }
            val settings = GymSettings(
                autoSuspensionEnabled = doc.getBoolean("autoSuspensionEnabled") ?: false,
                gracePeriodDays = (doc.getLong("gracePeriodDays") ?: 3).toInt()
            )
            Result.Success(settings)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load settings")
        }
    }

    suspend fun updateSettings(settings: GymSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "autoSuspensionEnabled" to settings.autoSuspensionEnabled,
                "gracePeriodDays" to settings.gracePeriodDays
            )
            settingsDoc.set(data).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to save settings")
        }
    }

    suspend fun runAutoSuspension(gracePeriodDays: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val cutoffDate = java.time.LocalDate.now().minusDays(gracePeriodDays.toLong()).toString()
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "member")
                .whereEqualTo("membershipStatus", "active")
                .whereLessThan("membershipEndDate", cutoffDate)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return@withContext Result.Success(0)
            }

            val batch = firestore.batch()
            var count = 0

            for (doc in snapshot.documents) {
                batch.update(doc.reference, "membershipStatus", "suspended")

                val notifRef = firestore.collection("notifications")
                    .document(doc.id)
                    .collection("items")
                    .document()

                val notification = hashMapOf(
                    "type" to "auto_suspension",
                    "title" to "Membership Expired",
                    "body" to "Your membership has expired and your account has been automatically suspended. Please renew to continue access.",
                    "isRead" to false,
                    "metadata" to "",
                    "createdAt" to System.currentTimeMillis()
                )
                batch.set(notifRef, notification)
                count++
            }

            batch.commit().await()
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to run auto-suspension")
        }
    }
}
