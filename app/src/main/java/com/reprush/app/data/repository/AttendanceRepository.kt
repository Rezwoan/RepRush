package com.reprush.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class AttendanceRecord(
    val id: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val date: String = "",
    val markedBy: String = "",
    val createdAt: Long = 0L
)

@Singleton
class AttendanceRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("attendance")

    suspend fun markAttendance(memberId: String, markedBy: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().toString()
            val existing = collection
                .whereEqualTo("memberId", memberId)
                .whereEqualTo("date", today)
                .get()
                .await()

            if (!existing.isEmpty) {
                return@withContext Result.Error("Already checked in today")
            }

            val docRef = collection.document()
            val record = hashMapOf(
                "memberId" to memberId,
                "date" to today,
                "markedBy" to markedBy,
                "createdAt" to System.currentTimeMillis()
            )
            docRef.set(record).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to mark attendance")
        }
    }

    suspend fun getTodayAttendance(): Result<List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().toString()
            val snapshot = collection
                .whereEqualTo("date", today)
                .get()
                .await()
            val records = snapshot.documents.map { doc ->
                AttendanceRecord(
                    id = doc.id,
                    memberId = doc.getString("memberId") ?: "",
                    date = doc.getString("date") ?: "",
                    markedBy = doc.getString("markedBy") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }
            Result.Success(records)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch today's attendance")
        }
    }

    suspend fun getMemberAttendance(memberId: String): Result<List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = collection
                .whereEqualTo("memberId", memberId)
                .get()
                .await()
            val records = snapshot.documents.map { doc ->
                AttendanceRecord(
                    id = doc.id,
                    memberId = doc.getString("memberId") ?: "",
                    date = doc.getString("date") ?: "",
                    markedBy = doc.getString("markedBy") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }.sortedByDescending { it.date }
            Result.Success(records)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch member attendance")
        }
    }

    suspend fun getCheckedInTodayCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().toString()
            val snapshot = collection
                .whereEqualTo("date", today)
                .get()
                .await()
            Result.Success(snapshot.size())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to count check-ins")
        }
    }

    suspend fun getAttendanceInRange(
        startDate: String,
        endDate: String
    ): Result<List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = collection
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()
            val records = snapshot.documents.map { doc ->
                AttendanceRecord(
                    id = doc.id,
                    memberId = doc.getString("memberId") ?: "",
                    date = doc.getString("date") ?: "",
                    markedBy = doc.getString("markedBy") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }
            Result.Success(records)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch attendance range")
        }
    }
}
