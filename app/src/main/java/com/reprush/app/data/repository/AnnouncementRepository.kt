package com.reprush.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class Announcement(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val postedBy: String = "",
    val createdAt: Long = 0L
)

@Singleton
class AnnouncementRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val announcementsCollection = firestore.collection("announcements")

    suspend fun postAnnouncement(
        title: String,
        body: String,
        postedBy: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docRef = announcementsCollection.document()
            val data = hashMapOf(
                "title" to title,
                "body" to body,
                "postedBy" to postedBy,
                "createdAt" to System.currentTimeMillis()
            )
            docRef.set(data).await()
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to post announcement")
        }
    }

    suspend fun fanOutNotification(
        title: String,
        body: String,
        announcementId: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val activeMembers = firestore.collection("users")
                .whereEqualTo("role", "member")
                .whereEqualTo("membershipStatus", "active")
                .get()
                .await()

            val batch = firestore.batch()
            var count = 0

            for (memberDoc in activeMembers.documents) {
                val notifRef = firestore.collection("notifications")
                    .document(memberDoc.id)
                    .collection("items")
                    .document()

                val notification = hashMapOf(
                    "type" to "announcement",
                    "title" to title,
                    "body" to body,
                    "isRead" to false,
                    "metadata" to "{\"announcementId\":\"$announcementId\"}",
                    "createdAt" to System.currentTimeMillis()
                )
                batch.set(notifRef, notification)
                count++

                if (count % 500 == 0) {
                    batch.commit().await()
                }
            }

            if (count % 500 != 0) {
                batch.commit().await()
            }

            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to send notifications")
        }
    }

    suspend fun getAnnouncements(): Result<List<Announcement>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = announcementsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val announcements = snapshot.documents.map { doc ->
                Announcement(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    body = doc.getString("body") ?: "",
                    postedBy = doc.getString("postedBy") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }
            Result.Success(announcements)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch announcements")
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            announcementsCollection.document(announcementId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete announcement")
        }
    }
}
