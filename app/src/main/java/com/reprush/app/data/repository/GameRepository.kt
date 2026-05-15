package com.reprush.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class LeaderboardEntry(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val points: Int = 0,
    val totalPRs: Int = 0,
    val leaderboardOptIn: Boolean = true
)

@Singleton
class GameRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun monthKey(): String = SimpleDateFormat("yyyy_MM", Locale.US).format(Date())

    private fun leaderboardEntryRef(uid: String) = firestore
        .collection("leaderboard")
        .document("monthly_${monthKey()}")
        .collection("entries")
        .document(uid)

    suspend fun writeLeaderboardEntry(
        uid: String,
        displayName: String,
        photoUrl: String?,
        pointsToAdd: Int,
        totalPRs: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ref = leaderboardEntryRef(uid)
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                val existing = if (snap.exists()) snap.getLong("points")?.toInt() ?: 0 else 0
                tx.set(
                    ref,
                    mapOf(
                        "uid" to uid,
                        "displayName" to displayName,
                        "photoUrl" to photoUrl,
                        "points" to existing + pointsToAdd,
                        "totalPRs" to totalPRs,
                        "leaderboardOptIn" to true
                    ),
                    SetOptions.merge()
                )
            }.await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to write leaderboard")
        }
    }

    suspend fun updateUserDoc(
        uid: String,
        pointsToAdd: Int,
        totalWorkouts: Int,
        totalPRs: Int,
        currentStreak: Int,
        longestStreak: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ref = firestore.collection("users").document(uid)
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                val existingTotal = if (snap.exists()) snap.getLong("totalPoints")?.toInt() ?: 0 else 0
                val existingMonthly = if (snap.exists()) snap.getLong("monthlyPoints")?.toInt() ?: 0 else 0
                tx.set(
                    ref,
                    mapOf(
                        "totalPoints" to existingTotal + pointsToAdd,
                        "monthlyPoints" to existingMonthly + pointsToAdd,
                        "totalWorkouts" to totalWorkouts,
                        "totalPRs" to totalPRs,
                        "currentStreak" to currentStreak,
                        "longestStreak" to longestStreak
                    ),
                    SetOptions.merge()
                )
            }.await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update user doc")
        }
    }

    suspend fun getLeaderboard(): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore
                .collection("leaderboard")
                .document("monthly_${monthKey()}")
                .collection("entries")
                .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val entries = snapshot.documents.mapNotNull { doc ->
                doc.toObject(LeaderboardEntry::class.java)?.copy(uid = doc.id)
            }.filter { it.leaderboardOptIn }
            Result.Success(entries)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load leaderboard")
        }
    }
}
