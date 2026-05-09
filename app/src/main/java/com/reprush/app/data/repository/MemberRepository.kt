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
}