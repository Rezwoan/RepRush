package com.reprush.app.data.repository

import android.content.Context
import com.reprush.app.data.local.datastore.AppPreferences
import com.reprush.app.data.local.dao.ExerciseDao
import com.reprush.app.data.local.entity.ExerciseEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseDao: ExerciseDao,
    private val appPreferences: AppPreferences
) {
    // Maps user-visible filter chip labels to the exact primaryMuscle values stored in Room.
    // All DB values are title-cased during sync (e.g. "lats" → "Lats").
    val MUSCLE_FILTER_MAP: Map<String, List<String>> = mapOf(
        "Chest"     to listOf("Chest"),
        "Back"      to listOf("Lats", "Middle Back", "Lower Back", "Traps"),
        "Shoulders" to listOf("Shoulders"),
        "Biceps"    to listOf("Biceps"),
        "Triceps"   to listOf("Triceps"),
        "Legs"      to listOf("Quadriceps", "Hamstrings", "Glutes", "Calves", "Adductors", "Abductors"),
        "Core"      to listOf("Abdominals"),
        "Forearms"  to listOf("Forearms"),
        "Neck"      to listOf("Neck")
    )

    suspend fun isLibrarySynced(): Boolean = appPreferences.isLibrarySynced.first()

    suspend fun syncExercises(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonText = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonText)

            val exercises = mutableListOf<ExerciseEntity>()

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

                val imageUrl = if (paths.isNotEmpty()) "exercise_images/${paths[0]}" else null
                val thumbnailUrl = when {
                    paths.size >= 2 -> "exercise_images/${paths[1]}"
                    paths.isNotEmpty() -> "exercise_images/${paths[0]}"
                    else -> null
                }

                exercises.add(
                    ExerciseEntity(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        primaryMuscle = primaryMuscle,
                        secondaryMuscles = secondaryMuscles,
                        equipment = equipment,
                        category = category,
                        imageUrl = imageUrl,
                        thumbnailUrl = thumbnailUrl,
                        muscleImageUrl = null,
                        isCustom = 0,
                        isVerified = 1
                    )
                )
            }

            exerciseDao.insertAll(exercises)
            appPreferences.setLibrarySynced(true)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Sync failed")
        }
    }

    /**
     * Returns exercises filtered by name query, muscle chip label, and equipment label.
     * Muscle filtering expands chip labels (e.g. "Back") to all matching DB values
     * (e.g. Lats, Middle Back, Lower Back, Traps) and checks both primaryMuscle and
     * secondaryMuscles (comma-separated) so compound exercises surface under both groups.
     */
    suspend fun getExercisesFiltered(
        query: String = "",
        muscle: String = "",
        equipment: String = ""
    ): List<ExerciseEntity> = withContext(Dispatchers.IO) {
        val all = exerciseDao.getAllExercises()

        val muscleTargets: List<String> = when {
            muscle.isBlank() || muscle == "All" -> emptyList()
            else -> MUSCLE_FILTER_MAP[muscle] ?: listOf(muscle)
        }

        val equipmentLower = equipment.trim().lowercase()

        all.filter { ex ->
            // Name search
            (query.isBlank() || ex.name.contains(query.trim(), ignoreCase = true)) &&
            // Muscle filter: match primaryMuscle OR any mapped name in the comma-separated secondaryMuscles
            (muscleTargets.isEmpty() ||
                muscleTargets.any { target ->
                    ex.primaryMuscle.equals(target, ignoreCase = true) ||
                    ex.secondaryMuscles?.split(",")?.any { sec ->
                        sec.trim().equals(target, ignoreCase = true)
                    } == true
                }) &&
            // Equipment filter
            (equipmentLower.isBlank() || equipmentLower == "all" ||
                ex.equipment.equals(equipmentLower, ignoreCase = true))
        }
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
                    "Full Gym" to emptyList<String>()
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
