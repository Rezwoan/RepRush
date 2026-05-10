package com.reprush.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class PendingMember(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = 0L
)

@Singleton
class MemberRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun getPendingMembers(): Result<List<PendingMember>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("membershipStatus", "pending")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            val members = snapshot.documents.map { doc ->
                PendingMember(
                    uid = doc.id,
                    displayName = doc.getString("displayName") ?: "",
                    email = doc.getString("email") ?: "",
                    photoUrl = doc.getString("photoUrl"),
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }
            Result.Success(members)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch pending members")
        }
    }

    suspend fun approveMember(
        uid: String,
        packageId: String,
        packageDurationDays: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val today = java.time.LocalDate.now()
            val startDate = today.toString()                                // yyyy-MM-dd
            val endDate = today.plusDays(packageDurationDays.toLong()).toString()

            firestore.collection("users").document(uid).update(
                mapOf(
                    "membershipStatus" to "active",
                    "packageId" to packageId,
                    "membershipStartDate" to startDate,
                    "membershipEndDate" to endDate
                )
            ).await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to approve member")
        }
    }

    suspend fun rejectMember(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Update user status to rejected
            firestore.collection("users").document(uid)
                .update("membershipStatus", "rejected")
                .await()

            // Write rejection notification to the member's inbox
            val notificationId = java.util.UUID.randomUUID().toString()
            val notification = hashMapOf(
                "type" to "rejection",
                "title" to "Registration Update",
                "body" to "Your registration request was not approved. Please contact the gym for more information.",
                "isRead" to false,
                "metadata" to "",
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("notifications")
                .document(uid)
                .collection("items")
                .document(notificationId)
                .set(notification)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to reject member")
        }
    }
}