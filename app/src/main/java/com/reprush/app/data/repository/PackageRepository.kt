package com.reprush.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.reprush.app.data.model.MembershipPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

@Singleton
class PackageRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("membership_packages")

    suspend fun getActivePackages(): Result<List<MembershipPackage>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = collection.whereEqualTo("active", true).get().await()
            val packages = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(MembershipPackage::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            Result.Success(packages)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load active packages")
        }
    }

    suspend fun getAllPackages(): Result<List<MembershipPackage>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = collection.get().await()
            val packages = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(MembershipPackage::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            Result.Success(packages)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to load packages")
        }
    }

    suspend fun createPackage(pkg: MembershipPackage): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docRef = collection.document()
            val newPkg = pkg.copy(id = docRef.id, createdAt = System.currentTimeMillis())
            docRef.set(newPkg).await()
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to create package")
        }
    }

    suspend fun updatePackage(pkg: MembershipPackage): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "name" to pkg.name,
                "price" to pkg.price,
                "durationDays" to pkg.durationDays,
                "description" to pkg.description
            )
            collection.document(pkg.id).update(updates).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update package")
        }
    }

    suspend fun deactivatePackage(packageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            collection.document(packageId).update("active", false).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to deactivate package")
        }
    }
}
