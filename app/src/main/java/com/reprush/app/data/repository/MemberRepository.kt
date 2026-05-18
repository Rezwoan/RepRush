package com.reprush.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
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

data class Member(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val membershipStatus: String = "pending",
    val packageId: String? = null,
    val packageName: String? = null,
    val membershipEndDate: String? = null,
    val createdAt: Long = 0L
)

data class MemberDetail(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val membershipStatus: String = "pending",
    val packageId: String? = null,
    val packageName: String? = null,
    val membershipStartDate: String? = null,
    val membershipEndDate: String? = null,
    val createdAt: Long = 0L,
    val totalPoints: Long = 0L,
    val totalWorkouts: Long = 0L,
    val totalPRs: Long = 0L
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
        startDate: String,
        packageDurationDays: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val start = java.time.LocalDate.parse(startDate)
            val endDate = start.plusDays(packageDurationDays.toLong()).toString()

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

    suspend fun registerMemberManually(
        displayName: String,
        email: String,
        phone: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("users").document()
            val newMember = hashMapOf(
                "displayName" to displayName,
                "email" to email,
                "phone" to phone,
                "photoUrl" to null,
                "role" to "member",
                "membershipStatus" to "pending",
                "packageId" to null,
                "membershipStartDate" to null,
                "membershipEndDate" to null,
                "onboardingComplete" to false,
                "createdAt" to System.currentTimeMillis()
            )
            docRef.set(newMember).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to register member")
        }
    }

    suspend fun registerMemberAsActive(
        displayName: String,
        email: String,
        phone: String,
        packageId: String,
        startDate: String,
        endDate: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("users").document()
            val newMember = hashMapOf(
                "displayName" to displayName,
                "email" to email,
                "phone" to phone,
                "photoUrl" to null,
                "role" to "member",
                "membershipStatus" to "active",
                "packageId" to packageId,
                "membershipStartDate" to startDate,
                "membershipEndDate" to endDate,
                "onboardingComplete" to false,
                "createdAt" to System.currentTimeMillis()
            )
            docRef.set(newMember).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to register member")
        }
    }

    suspend fun getMembers(statusFilter: String? = null): Result<List<Member>> = withContext(Dispatchers.IO) {
        try {
            val query = if (statusFilter == null || statusFilter == "all") {
                firestore.collection("users")
                    .whereEqualTo("role", "member")
                    .orderBy("displayName", Query.Direction.ASCENDING)
            } else {
                firestore.collection("users")
                    .whereEqualTo("role", "member")
                    .whereEqualTo("membershipStatus", statusFilter)
                    .orderBy("displayName", Query.Direction.ASCENDING)
            }

            val snapshot = query.get().await()
            val members = snapshot.documents.map { doc ->
                Member(
                    uid = doc.id,
                    displayName = doc.getString("displayName") ?: "",
                    email = doc.getString("email") ?: "",
                    photoUrl = doc.getString("photoUrl"),
                    membershipStatus = doc.getString("membershipStatus") ?: "pending",
                    packageId = doc.getString("packageId"),
                    membershipEndDate = doc.getString("membershipEndDate"),
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }
            Result.Success(members)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch members")
        }
    }

    suspend fun getMemberById(uid: String): Result<MemberDetail> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (!doc.exists()) return@withContext Result.Error("Member not found")

            val member = MemberDetail(
                uid = doc.id,
                displayName = doc.getString("displayName") ?: "",
                email = doc.getString("email") ?: "",
                photoUrl = doc.getString("photoUrl"),
                membershipStatus = doc.getString("membershipStatus") ?: "pending",
                packageId = doc.getString("packageId"),
                membershipStartDate = doc.getString("membershipStartDate"),
                membershipEndDate = doc.getString("membershipEndDate"),
                createdAt = doc.getLong("createdAt") ?: 0L,
                totalPoints = doc.getLong("totalPoints") ?: 0L,
                totalWorkouts = doc.getLong("totalWorkouts") ?: 0L,
                totalPRs = doc.getLong("totalPRs") ?: 0L
            )
            Result.Success(member)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch member")
        }
    }

    suspend fun suspendMember(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(uid)
                .update("membershipStatus", "suspended")
                .await()

            // Notify member
            val notificationId = java.util.UUID.randomUUID().toString()
            val notification = hashMapOf(
                "type" to "suspension",
                "title" to "Account Suspended",
                "body" to "Your gym membership has been suspended. Please contact the gym for more information.",
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
            Result.Error(e.message ?: "Failed to suspend member")
        }
    }

    suspend fun reactivateMember(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(uid)
                .update("membershipStatus", "active")
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to reactivate member")
        }
    }

    suspend fun removeMember(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(uid).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to remove member")
        }
    }

    suspend fun assignPackage(
        uid: String,
        packageId: String,
        durationDays: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val today = java.time.LocalDate.now()
            val endDate = today.plusDays(durationDays.toLong())
            firestore.collection("users").document(uid).update(
                mapOf(
                    "packageId" to packageId,
                    "membershipStartDate" to today.toString(),
                    "membershipEndDate" to endDate.toString(),
                    "membershipStatus" to "active"
                )
            ).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to assign package")
        }
    }

    suspend fun saveOnboardingProfile(
        uid: String,
        heightCm: Float,
        weightKg: Float,
        fitnessLevel: String,
        lastExercised: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(uid)
                .set(
                    mapOf(
                        "heightCm" to heightCm,
                        "weightKg" to weightKg,
                        "fitnessLevel" to fitnessLevel,
                        "lastExercised" to lastExercised,
                        "onboardingComplete" to true
                    ),
                    SetOptions.merge()
                ).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to save onboarding profile")
        }
    }
}