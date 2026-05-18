package com.reprush.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.reprush.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
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

            var batch = firestore.batch()
            var count = 0
            var batchCount = 0

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
                batchCount++

                if (batchCount == 500) {
                    batch.commit().await()
                    batch = firestore.batch()
                    batchCount = 0
                }
            }

            if (batchCount > 0) {
                batch.commit().await()
            }

            sendFcmToTopic(title, body)

            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to send notifications")
        }
    }

    private suspend fun sendFcmToTopic(title: String, body: String) = withContext(Dispatchers.IO) {
        val serverKey = BuildConfig.FCM_SERVER_KEY
        if (serverKey.isBlank()) return@withContext
        try {
            val conn = URL("https://fcm.googleapis.com/fcm/send")
                .openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "key=$serverKey")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val safeTitle = title.replace("\"", "\\\"")
            val safeBody = body.replace("\"", "\\\"")
            val payload = """{"to":"/topics/gym_announcements","notification":{"title":"$safeTitle","body":"$safeBody"},"data":{"type":"announcement"}}"""
            conn.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(payload) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {
            // FCM failure is non-critical — Firestore notification still delivered
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
