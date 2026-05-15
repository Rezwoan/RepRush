package com.reprush.app.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.reprush.app.data.local.AppDatabase
import com.reprush.app.data.local.dao.ExerciseDao
import com.reprush.app.data.local.dao.LoggedSetDao
import com.reprush.app.data.local.dao.PlanExerciseDao
import com.reprush.app.data.local.dao.WorkoutSessionDao
import com.reprush.app.data.local.entity.LoggedSetEntity
import com.reprush.app.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SessionSet(
    val id: String = UUID.randomUUID().toString(),
    val setNumber: Int,
    val weight: Double? = null,
    val reps: Int? = null,
    val isWarmup: Boolean = false,
    val isCompleted: Boolean = false,
    val loggedAt: Long? = null,
    val weightUserEdited: Boolean = false,
    val repsUserEdited: Boolean = false,
    val isBodyweight: Boolean = false
)

data class SessionExercise(
    val exerciseId: String,
    val exerciseName: String,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val plannedSets: Int,
    val plannedRepsRange: String,
    val plannedRestSeconds: Int,
    val sets: MutableList<SessionSet> = mutableListOf()
)

data class ActiveSessionState(
    val sessionId: String,
    val userId: String,
    val planDayId: String?,
    val startTime: Long,
    val exercises: MutableList<SessionExercise> = mutableListOf(),
    var notes: String = ""
)

@Singleton
class SessionRepository @Inject constructor(
    private val db: AppDatabase,
    private val planExerciseDao: PlanExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val loggedSetDao: LoggedSetDao
) {

    suspend fun loadPlanDayExercises(dayId: String): List<SessionExercise> =
        withContext(Dispatchers.IO) {
            val planExercises = planExerciseDao.getExercisesForDay(dayId)
            planExercises.mapNotNull { pe ->
                val ex = exerciseDao.getExerciseById(pe.exerciseId) ?: return@mapNotNull null
                val isBw = ex.equipment.equals("Bodyweight", ignoreCase = true)
                val restSeconds = if (pe.restSeconds > 0) pe.restSeconds else 60
                val sets = (1..pe.sets).map { i ->
                    SessionSet(setNumber = i, isBodyweight = isBw)
                }.toMutableList()
                SessionExercise(
                    exerciseId = ex.id,
                    exerciseName = ex.name,
                    imageUrl = ex.imageUrl,
                    thumbnailUrl = ex.thumbnailUrl,
                    plannedSets = pe.sets,
                    plannedRepsRange = pe.repsRange,
                    plannedRestSeconds = restSeconds,
                    sets = sets
                )
            }
        }

    suspend fun loadExerciseById(exerciseId: String): SessionExercise? =
        withContext(Dispatchers.IO) {
            val ex = exerciseDao.getExerciseById(exerciseId) ?: return@withContext null
            val isBw = ex.equipment.equals("Bodyweight", ignoreCase = true)
            SessionExercise(
                exerciseId = ex.id,
                exerciseName = ex.name,
                imageUrl = ex.imageUrl,
                thumbnailUrl = ex.thumbnailUrl,
                plannedSets = 3,
                plannedRepsRange = "8-12",
                plannedRestSeconds = 90,
                sets = mutableListOf(SessionSet(setNumber = 1, isBodyweight = isBw))
            )
        }

    suspend fun startSession(state: ActiveSessionState) = withContext(Dispatchers.IO) {
        val session = WorkoutSessionEntity(
            id = state.sessionId,
            userId = state.userId,
            planDayId = state.planDayId,
            startTime = state.startTime,
            endTime = null,
            notes = null,
            totalPoints = 0,
            isCompleted = 0
        )
        workoutSessionDao.insertSession(session)
    }

    suspend fun autoSave(state: ActiveSessionState) = withContext(Dispatchers.IO) {
        try {
            val session = WorkoutSessionEntity(
                id = state.sessionId,
                userId = state.userId,
                planDayId = state.planDayId,
                startTime = state.startTime,
                endTime = null,
                notes = state.notes.ifBlank { null },
                totalPoints = 0,
                isCompleted = 0
            )
            workoutSessionDao.updateSession(session)

            val completedSets = state.exercises.flatMap { ex ->
                ex.sets.filter { it.isCompleted }.map { set ->
                    LoggedSetEntity(
                        id = set.id,
                        sessionId = state.sessionId,
                        exerciseId = ex.exerciseId,
                        setNumber = set.setNumber,
                        weight = set.weight ?: 0.0,
                        reps = set.reps ?: 0,
                        isWarmup = if (set.isWarmup) 1 else 0,
                        isCompleted = 1,
                        isPersonalRecord = 0,
                        loggedAt = set.loggedAt ?: System.currentTimeMillis()
                    )
                }
            }
            if (completedSets.isNotEmpty()) {
                loggedSetDao.insertAll(completedSets)
            }
        } catch (e: Exception) {
            Log.w("SessionRepository", "Auto-save failed: ${e.message}")
        }
    }

    suspend fun finishSession(
        state: ActiveSessionState,
        endTime: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val allCompletedSets = state.exercises.flatMap { ex ->
                ex.sets.filter { it.isCompleted }.map { set ->
                    LoggedSetEntity(
                        id = set.id,
                        sessionId = state.sessionId,
                        exerciseId = ex.exerciseId,
                        setNumber = set.setNumber,
                        weight = set.weight ?: 0.0,
                        reps = set.reps ?: 0,
                        isWarmup = if (set.isWarmup) 1 else 0,
                        isCompleted = 1,
                        isPersonalRecord = 0,
                        loggedAt = set.loggedAt ?: System.currentTimeMillis()
                    )
                }
            }
            db.withTransaction {
                workoutSessionDao.updateSession(
                    WorkoutSessionEntity(
                        id = state.sessionId,
                        userId = state.userId,
                        planDayId = state.planDayId,
                        startTime = state.startTime,
                        endTime = endTime,
                        notes = state.notes.ifBlank { null },
                        totalPoints = 0,
                        isCompleted = 1
                    )
                )
                if (allCompletedSets.isNotEmpty()) {
                    loggedSetDao.insertAll(allCompletedSets)
                }
            }
            Result.Success(state.sessionId)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to save session")
        }
    }

    suspend fun getIncompleteSession(userId: String): WorkoutSessionEntity? =
        withContext(Dispatchers.IO) { workoutSessionDao.getIncompleteSession(userId) }

    suspend fun recoverSession(session: WorkoutSessionEntity): ActiveSessionState =
        withContext(Dispatchers.IO) {
            val savedSets = loggedSetDao.getSetsForSession(session.id)

            val exerciseMap = linkedMapOf<String, MutableList<LoggedSetEntity>>()
            savedSets.forEach { set ->
                exerciseMap.getOrPut(set.exerciseId) { mutableListOf() }.add(set)
            }

            val exercises = exerciseMap.map { (exerciseId, sets) ->
                val ex = exerciseDao.getExerciseById(exerciseId)
                val sessionSets = sets.sortedBy { it.setNumber }.map { set ->
                    SessionSet(
                        id = set.id,
                        setNumber = set.setNumber,
                        weight = set.weight,
                        reps = set.reps,
                        isWarmup = set.isWarmup == 1,
                        isCompleted = set.isCompleted == 1,
                        loggedAt = set.loggedAt
                    )
                }.toMutableList()
                SessionExercise(
                    exerciseId = exerciseId,
                    exerciseName = ex?.name ?: "Unknown Exercise",
                    imageUrl = ex?.imageUrl,
                    thumbnailUrl = ex?.thumbnailUrl,
                    plannedSets = sessionSets.size,
                    plannedRepsRange = "",
                    plannedRestSeconds = 90,
                    sets = sessionSets
                )
            }.toMutableList()

            ActiveSessionState(
                sessionId = session.id,
                userId = session.userId,
                planDayId = session.planDayId,
                startTime = session.startTime,
                exercises = exercises,
                notes = session.notes ?: ""
            )
        }

    suspend fun discardIncompleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            db.withTransaction {
                loggedSetDao.deleteSetsForSession(sessionId)
                workoutSessionDao.deleteSession(sessionId)
            }
        } catch (e: Exception) {
            Log.w("SessionRepository", "Discard session failed: ${e.message}")
        }
    }
}
