package com.reprush.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.local.dao.ExerciseDao
import com.reprush.app.data.local.dao.PlanDayDao
import com.reprush.app.data.local.dao.PlanExerciseDao
import com.reprush.app.data.local.dao.WorkoutPlanDao
import com.reprush.app.data.local.entity.ExerciseEntity
import com.reprush.app.data.local.entity.PlanDayEntity
import com.reprush.app.data.local.entity.PlanExerciseEntity
import com.reprush.app.data.local.entity.WorkoutPlanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ImportedPlan(
    val planId: String,
    val planName: String,
    val goal: String,
    val weeks: Int,
    val daysPerWeek: Int,
    val days: List<ImportedDay>,
    val unverifiedExercises: List<String>
)

data class ImportedDay(
    val dayLabel: String,
    val exercises: List<ImportedPlanExercise>
)

data class ImportedPlanExercise(
    val exerciseName: String,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val notes: String?
)

@Singleton
class PlanImportRepository @Inject constructor(
    private val workoutPlanDao: WorkoutPlanDao,
    private val planDayDao: PlanDayDao,
    private val planExerciseDao: PlanExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseRepository: ExerciseRepository,
    private val auth: FirebaseAuth
) {
    suspend fun importPlan(plan: GeminiPlan): Result<ImportedPlan> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.Error("Not signed in.")
            val unverifiedNames = mutableListOf<String>()

            workoutPlanDao.deactivateAllPlans(userId)

            val planId = UUID.randomUUID().toString()
            val planEntity = WorkoutPlanEntity(
                id = planId,
                userId = userId,
                planName = plan.planName,
                goal = plan.goal,
                totalWeeks = plan.weeks,
                daysPerWeek = plan.daysPerWeek,
                schemaVersion = plan.schemaVersion,
                isActive = 1,
                createdAt = System.currentTimeMillis()
            )
            workoutPlanDao.insertPlan(planEntity)

            val importedDays = mutableListOf<ImportedDay>()

            for (day in plan.schedule) {
                val dayId = UUID.randomUUID().toString()
                planDayDao.insertPlanDay(
                    PlanDayEntity(id = dayId, planId = planId, dayNumber = day.dayNumber, dayLabel = day.dayLabel)
                )

                val importedExercises = mutableListOf<ImportedPlanExercise>()
                for ((orderIdx, ex) in day.exercises.withIndex()) {
                    val exercise = resolveExercise(ex.exerciseName, unverifiedNames)
                    planExerciseDao.insertPlanExercise(
                        PlanExerciseEntity(
                            id = UUID.randomUUID().toString(),
                            dayId = dayId,
                            exerciseId = exercise.id,
                            sets = ex.sets,
                            repsRange = ex.reps,
                            restSeconds = ex.restSeconds,
                            orderIndex = orderIdx,
                            notes = ex.notes
                        )
                    )
                    importedExercises.add(ImportedPlanExercise(ex.exerciseName, ex.sets, ex.reps, ex.restSeconds, ex.notes))
                }
                importedDays.add(ImportedDay(day.dayLabel, importedExercises))
            }

            Result.Success(ImportedPlan(planId, plan.planName, plan.goal, plan.weeks, plan.daysPerWeek, importedDays, unverifiedNames))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to import plan.")
        }
    }

    private suspend fun resolveExercise(name: String, unverifiedNames: MutableList<String>): ExerciseEntity {
        val existing = exerciseDao.getExerciseByName(name)
        if (existing != null) return existing
        unverifiedNames.add(name)
        return exerciseRepository.insertUnverifiedExercise(name)
    }

    suspend fun getPlansForUser(): List<WorkoutPlanEntity> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return withContext(Dispatchers.IO) { workoutPlanDao.getPlansForUser(userId) }
    }

    suspend fun setActivePlan(planId: String, userId: String) = withContext(Dispatchers.IO) {
        workoutPlanDao.deactivateAllPlans(userId)
        val plan = workoutPlanDao.getPlansForUser(userId).find { it.id == planId } ?: return@withContext
        workoutPlanDao.updatePlan(plan.copy(isActive = 1))
    }

    suspend fun deletePlan(plan: WorkoutPlanEntity) = withContext(Dispatchers.IO) {
        workoutPlanDao.deletePlan(plan)
    }
}
