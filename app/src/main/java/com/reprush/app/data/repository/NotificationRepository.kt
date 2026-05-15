package com.reprush.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationItem(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val isRead: Boolean = false,
    val metadata: String = "",
    val createdAt: Long = 0L
)

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun getNotifications(uid: String): Result<List<NotificationItem>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("notifications")
                .document(uid)
                .collection("items")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val items = snapshot.documents.map { doc ->
                NotificationItem(
                    id = doc.id,
                    type = doc.getString("type") ?: "",
                    title = doc.getString("title") ?: "",
                    body = doc.getString("body") ?: "",
                    isRead = doc.getBoolean("isRead") ?: false,
                    metadata = doc.getString("metadata") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }
            Result.Success(items)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch notifications")
        }
    }

    suspend fun markAsRead(uid: String, notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("notifications")
                .document(uid)
                .collection("items")
                .document(notificationId)
                .update("isRead", true)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to mark notification as read")
        }
    }

    suspend fun markAllAsRead(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val unread = firestore.collection("notifications")
                .document(uid)
                .collection("items")
                .whereEqualTo("isRead", false)
                .get()
                .await()

            if (unread.isEmpty) return@withContext Result.Success(Unit)

            val batch = firestore.batch()
            for (doc in unread.documents) {
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to mark all as read")
        }
    }

    suspend fun getUnreadCount(uid: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("notifications")
                .document(uid)
                .collection("items")
                .whereEqualTo("isRead", false)
                .get()
                .await()
            Result.Success(snapshot.size())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to count unread notifications")
        }
    }
}
