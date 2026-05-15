package com.reprush.app.data.repository

import android.content.Context
import android.util.Log
import com.reprush.app.data.local.datastore.AppPreferences
import com.reprush.app.data.local.dao.ExerciseDao
import com.reprush.app.data.local.entity.ExerciseEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SyncProgress(val currentCount: Int, val totalCount: Int)

@Singleton
class ExerciseRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseDao: ExerciseDao,
    private val appPreferences: AppPreferences
) {
    private val imageBaseUrl = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"
    private val imageDir: File get() {
        val dir = File(context.filesDir, "exercise_images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun isLibrarySynced(): Boolean = appPreferences.isLibrarySynced.first()

    suspend fun syncExercises(
        onProgress: (SyncProgress) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonText = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonText)

            val exercises = mutableListOf<ExerciseEntity>()
            val imagePaths = mutableListOf<Pair<String, List<String>>>() // exerciseId to imagePaths

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = toTitleCase(obj.getString("name").trim())

                val primaryMusclesArr = obj.optJSONArray("primaryMuscles")
                val primaryMuscle = if (primaryMusclesArr != null && primaryMusclesArr.length() > 0) {
                    toTitleCase(primaryMusclesArr.getString(0))
                } else "Unknown"

                val secondaryMusclesArr = obj.optJSONArray("secondaryMuscles")
                val secondaryMuscles = if (secondaryMusclesArr != null && secondaryMusclesArr.length() > 0) {
                    (0 until secondaryMusclesArr.length()).map { j ->
                        toTitleCase(secondaryMusclesArr.getString(j))
                    }.joinToString(",")
                } else null

                val equipment = run {
                    val raw = obj.optString("equipment", "").ifEmpty { null }
                    when {
                        raw == null || raw.equals("body only", ignoreCase = true) || raw.isEmpty() -> "Bodyweight"
                        else -> toTitleCase(raw)
                    }
                }

                val category = obj.optString("category", "strength").let { toTitleCase(it) }

                val imagesArr = obj.optJSONArray("images")
                val paths = if (imagesArr != null) {
                    (0 until imagesArr.length()).map { j -> imagesArr.getString(j) }
                } else emptyList()

                val id = UUID.randomUUID().toString()
                exercises.add(
                    ExerciseEntity(
                        id = id,
                        name = name,
                        primaryMuscle = primaryMuscle,
                        secondaryMuscles = secondaryMuscles,
                        equipment = equipment,
                        category = category,
                        imageUrl = null,
                        thumbnailUrl = null,
                        muscleImageUrl = null,
                        isCustom = 0,
                        isVerified = 1
                    )
                )
                if (paths.isNotEmpty()) {
                    imagePaths.add(Pair(id, paths))
                }
            }

            exerciseDao.insertAll(exercises)

            val resumeFrom = appPreferences.syncProgressCount.first()
            val imageItems = imagePaths
            val totalImages = imageItems.size

            onProgress(SyncProgress(resumeFrom, totalImages))

            for (idx in resumeFrom until imageItems.size) {
                val (exerciseId, paths) = imageItems[idx]
                if (paths.isEmpty()) continue

                val imagePath = paths[0]
                val localFile = File(imageDir, "${exerciseId}.jpg")
                try {
                    val url = URL(imageBaseUrl + imagePath)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 30_000
                    conn.readTimeout = 30_000
                    conn.connect()
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        conn.inputStream.use { input ->
                            FileOutputStream(localFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        exerciseDao.updateImageUrl(exerciseId, localFile.absolutePath)
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    Log.w("ExerciseSync", "Failed to download image for $exerciseId: ${e.message}")
                }

                appPreferences.setSyncProgressCount(idx + 1)
                onProgress(SyncProgress(idx + 1, totalImages))
            }

            appPreferences.setLibrarySynced(true)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Sync failed")
        }
    }

    suspend fun getExercisesFiltered(
        query: String = "",
        muscle: String = "",
        equipment: String = ""
    ): List<ExerciseEntity> = withContext(Dispatchers.IO) {
        exerciseDao.getExercisesFiltered(
            query = if (query.isBlank()) "" else query.trim(),
            muscle = if (muscle.isBlank() || muscle == "All") "" else muscle,
            equipment = if (equipment.isBlank() || equipment == "All") "" else equipment
        )
    }

    suspend fun getExerciseById(id: String): ExerciseEntity? =
        withContext(Dispatchers.IO) { exerciseDao.getExerciseById(id) }

    suspend fun getExerciseByName(name: String): ExerciseEntity? =
        withContext(Dispatchers.IO) { exerciseDao.getExerciseByName(name) }

    suspend fun insertCustomExercise(
        name: String,
        primaryMuscle: String,
        equipment: String,
        category: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedName = toTitleCase(name.trim())
            val existing = exerciseDao.getExerciseByName(normalizedName)
            if (existing != null) return@withContext Result.Error("An exercise with this name already exists.")
            exerciseDao.insertExercise(
                ExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    name = normalizedName,
                    primaryMuscle = primaryMuscle,
                    secondaryMuscles = null,
                    equipment = equipment,
                    category = category,
                    imageUrl = null,
                    thumbnailUrl = null,
                    muscleImageUrl = null,
                    isCustom = 1,
                    isVerified = 1
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to save exercise")
        }
    }

    suspend fun insertUnverifiedExercise(name: String): ExerciseEntity = withContext(Dispatchers.IO) {
        val existing = exerciseDao.getExerciseByName(name)
        if (existing != null) return@withContext existing
        val entity = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            primaryMuscle = "Unknown",
            secondaryMuscles = null,
            equipment = "Unknown",
            category = "Unknown",
            imageUrl = null,
            thumbnailUrl = null,
            muscleImageUrl = null,
            isCustom = 0,
            isVerified = 0
        )
        exerciseDao.insertExercise(entity)
        entity
    }

    suspend fun getExercisesForEquipment(equipment: String): List<ExerciseEntity> =
        withContext(Dispatchers.IO) {
            if (equipment == "Full Gym" || equipment.isBlank()) {
                exerciseDao.getAllExercises()
            } else {
                val equipmentMap = mapOf(
                    "Barbell+Dumbbells" to listOf("Barbell", "Dumbbell"),
                    "Dumbbells Only" to listOf("Dumbbell"),
                    "Bodyweight" to listOf("Bodyweight"),
                    "Full Gym" to emptyList()
                )
                val equipmentList = equipmentMap[equipment] ?: emptyList()
                if (equipmentList.isEmpty()) {
                    exerciseDao.getAllExercises()
                } else {
                    exerciseDao.getExercisesForEquipmentList(equipmentList)
                }
            }
        }

    private fun toTitleCase(input: String): String {
        return input.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }
}
