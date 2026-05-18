package com.reprush.app.data.debug

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.reprush.app.data.local.AppDatabase
import com.reprush.app.data.local.dao.AchievementDao
import com.reprush.app.data.local.dao.BodyWeightLogDao
import com.reprush.app.data.local.dao.ExerciseDao
import com.reprush.app.data.local.dao.LoggedSetDao
import com.reprush.app.data.local.dao.PlanDayDao
import com.reprush.app.data.local.dao.PlanExerciseDao
import com.reprush.app.data.local.dao.PrRecordDao
import com.reprush.app.data.local.dao.StreakDao
import com.reprush.app.data.local.dao.WorkoutPlanDao
import com.reprush.app.data.local.dao.WorkoutSessionDao
import com.reprush.app.data.local.entity.AchievementEntity
import com.reprush.app.data.local.entity.BodyWeightLogEntity
import com.reprush.app.data.local.entity.ExerciseEntity
import com.reprush.app.data.local.entity.LoggedSetEntity
import com.reprush.app.data.local.entity.PlanDayEntity
import com.reprush.app.data.local.entity.PlanExerciseEntity
import com.reprush.app.data.local.entity.PrRecordEntity
import com.reprush.app.data.local.entity.StreakEntity
import com.reprush.app.data.local.entity.WorkoutPlanEntity
import com.reprush.app.data.local.entity.WorkoutSessionEntity
import com.reprush.app.data.repository.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoDataSeeder @Inject constructor(
    private val appDatabase: AppDatabase,
    private val exerciseDao: ExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val loggedSetDao: LoggedSetDao,
    private val prRecordDao: PrRecordDao,
    private val streakDao: StreakDao,
    private val achievementDao: AchievementDao,
    private val bodyWeightLogDao: BodyWeightLogDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val planDayDao: PlanDayDao,
    private val planExerciseDao: PlanExerciseDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val tag = "DemoDataSeeder"

    suspend fun seedAll(): Result<String> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.Error("Must be signed in to seed demo data")

        val existingStreak = streakDao.getStreakForUser(uid)
        if (existingStreak != null && existingStreak.currentStreak >= 14) {
            return@withContext Result.Error("Demo data already seeded for this account")
        }

        var adminSeedError: String? = null
        try {
            seedAdminFirestoreData()
        } catch (e: Exception) {
            Log.e(tag, "Admin Firestore seed failed: ${e.message}", e)
            adminSeedError = e.message
        }

        try {
            seedMemberRoomData(uid)
        } catch (e: Exception) {
            Log.e(tag, "Member Room seed failed: ${e.message}", e)
            return@withContext Result.Error("Member data seed failed: ${e.message}")
        }

        if (adminSeedError != null) {
            Result.Success("Member data seeded. Admin data requires network: $adminSeedError")
        } else {
            Result.Success("All demo data seeded successfully")
        }
    }

    // ─── Firestore Admin Data ────────────────────────────────────────────────

    private suspend fun seedAdminFirestoreData() {
        val pkg1Id = seedPackage(
            name = "Monthly Basic",
            price = 1500.0,
            durationDays = 30,
            description = "Full gym access, 1 month"
        )
        val pkg2Id = seedPackage(
            name = "Quarterly Pro",
            price = 3500.0,
            durationDays = 90,
            description = "Full gym access, 3 months + locker"
        )

        val today = LocalDate.now()
        val memberIds = listOf(
            seedMember("Rafiqul Islam",  "rafiqul.demo@reprush.app",  pkg1Id, today, 30),
            seedMember("Nusrat Jahan",   "nusrat.demo@reprush.app",   pkg2Id, today, 80),
            seedMember("Tanjim Hossain", "tanjim.demo@reprush.app",   pkg1Id, today, 25),
            seedMember("Sharmin Akter",  "sharmin.demo@reprush.app",  pkg2Id, today, 70)
        )

        seedPayments(memberIds, listOf(pkg1Id, pkg2Id, pkg1Id, pkg2Id, pkg1Id))
        seedAttendance(memberIds, today)
        seedAnnouncement()
    }

    private suspend fun seedPackage(
        name: String, price: Double, durationDays: Int, description: String
    ): String {
        val existing = firestore.collection("membership_packages")
            .whereEqualTo("name", name).get().await()
        if (!existing.isEmpty) return existing.documents[0].id

        val docRef = firestore.collection("membership_packages").document()
        docRef.set(
            mapOf(
                "name" to name,
                "price" to price,
                "durationDays" to durationDays,
                "description" to description,
                "active" to true,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        Log.d(tag, "Seeded package: $name (${docRef.id})")
        return docRef.id
    }

    private suspend fun seedMember(
        name: String, email: String, packageId: String,
        today: LocalDate, daysUntilExpiry: Int
    ): String {
        val existing = firestore.collection("users").whereEqualTo("email", email).get().await()
        if (!existing.isEmpty) return existing.documents[0].id

        val docRef = firestore.collection("users").document()
        val startDate = today.minusDays(30).toString()
        val endDate = today.plusDays(daysUntilExpiry.toLong()).toString()
        docRef.set(
            mapOf(
                "displayName" to name,
                "email" to email,
                "role" to "member",
                "membershipStatus" to "active",
                "packageId" to packageId,
                "membershipStartDate" to startDate,
                "membershipEndDate" to endDate,
                "onboardingComplete" to 1,
                "fitnessLevel" to "Intermediate",
                "primaryGoal" to "Muscle Gain",
                "totalPoints" to (300..800).random(),
                "currentStreak" to (2..8).random(),
                "leaderboardOptIn" to true,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        Log.d(tag, "Seeded member: $name (${docRef.id})")
        return docRef.id
    }

    private suspend fun seedPayments(memberIds: List<String>, packageIds: List<String>) {
        val methods = listOf("cash", "bank_transfer", "mobile_banking")
        val today = LocalDate.now()
        val paymentDaysAgo = listOf(2, 7, 12, 18, 25)
        val amounts = listOf(1500.0, 3500.0, 1500.0, 3500.0, 1500.0)

        for (i in 0 until 5) {
            val memberId = memberIds[i % memberIds.size]
            val packageId = packageIds[i % packageIds.size]
            val paymentDate = today.minusDays(paymentDaysAgo[i].toLong()).toString()
            val periodStart = paymentDate
            val periodEnd = today.plusDays(if (i % 2 == 0) 30L else 90L).toString()

            val existing = firestore.collection("payments")
                .whereEqualTo("memberId", memberId)
                .whereEqualTo("paymentDate", paymentDate)
                .get().await()
            if (!existing.isEmpty) continue

            val docRef = firestore.collection("payments").document()
            docRef.set(
                mapOf(
                    "memberId" to memberId,
                    "packageId" to packageId,
                    "amount" to amounts[i],
                    "paymentMethod" to methods[i % methods.size],
                    "paymentDate" to paymentDate,
                    "periodStart" to periodStart,
                    "periodEnd" to periodEnd,
                    "isVoided" to false,
                    "voidReason" to null,
                    "recordedBy" to "admin",
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
        }
        Log.d(tag, "Seeded 5 payment records")
    }

    private suspend fun seedAttendance(memberIds: List<String>, today: LocalDate) {
        val collection = firestore.collection("attendance")
        // 3 weeks of data, 3-4 visits per week per member (Mon/Tue/Thu/Sat pattern)
        val visitDaysOfWeek = listOf(1, 2, 4, 6) // Mon=1, Tue=2, Thu=4, Sat=6

        for (memberIndex in memberIds.indices) {
            for (weeksAgo in 1..3) {
                // Each member visits 3 of the 4 possible days per week
                val visitDays = visitDaysOfWeek.shuffled().take(3)
                for (dayOfWeek in visitDays) {
                    val daysToSubtract = (weeksAgo * 7 - dayOfWeek + today.dayOfWeek.value).toLong()
                    if (daysToSubtract <= 0) continue
                    val visitDate = today.minusDays(daysToSubtract).toString()

                    val existing = collection
                        .whereEqualTo("memberId", memberIds[memberIndex])
                        .whereEqualTo("date", visitDate)
                        .get().await()
                    if (!existing.isEmpty) continue

                    collection.document().set(
                        mapOf(
                            "memberId" to memberIds[memberIndex],
                            "date" to visitDate,
                            "markedBy" to "admin",
                            "createdAt" to System.currentTimeMillis()
                        )
                    ).await()
                }
            }
        }
        Log.d(tag, "Seeded attendance records for ${memberIds.size} members")
    }

    private suspend fun seedAnnouncement() {
        val existing = firestore.collection("announcements")
            .whereEqualTo("title", "Welcome to RepRush!").get().await()
        if (!existing.isEmpty) return

        firestore.collection("announcements").document().set(
            mapOf(
                "title" to "Welcome to RepRush!",
                "body" to "Your AI-powered gym experience starts here. Track your workouts, " +
                        "set personal records, and let your AI trainer guide your journey to peak fitness.",
                "postedBy" to "admin",
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        Log.d(tag, "Seeded announcement")
    }

    // ─── Room Member Data ────────────────────────────────────────────────────

    private suspend fun seedMemberRoomData(uid: String) {
        val exerciseIds = ensureExercisesExist()

        val benchId    = exerciseIds["Barbell Bench Press"]!!
        val squatId    = exerciseIds["Barbell Squat"]!!
        val deadliftId = exerciseIds["Deadlift"]!!
        val ohpId      = exerciseIds["Barbell Overhead Press"]!!
        val rowId      = exerciseIds["Bent Over Barbell Row"]!!
        val curlId     = exerciseIds["Dumbbell Curl"]!!
        val tricepId   = exerciseIds["Tricep Pushdown"]!!

        // Create workout plan
        val planId = createDemoPlan(uid, benchId, squatId, deadliftId, ohpId, rowId, curlId, tricepId)
        val planDays = planDayDao.getDaysForPlan(planId)
        val pushDayId = planDays.find { it.dayLabel.contains("Push") }?.id
        val pullDayId = planDays.find { it.dayLabel.contains("Pull") }?.id
        val legsDayId = planDays.find { it.dayLabel.contains("Leg") }?.id

        // Seed 4 weeks of sessions (16 sessions)
        val today = LocalDate.now()
        val sessionSchedule = buildSessionSchedule(today) // List<Pair<LocalDate, String?>>

        var latestSessionId = ""
        var sessionIndex = 0

        for ((sessionDate, sessionType) in sessionSchedule) {
            val sessionId = UUID.randomUUID().toString()
            if (sessionIndex == sessionSchedule.size - 1) latestSessionId = sessionId

            val startMillis = sessionDate.toEpochDay() * 86400000L + 17 * 3600000L
            val endMillis = startMillis + 75 * 60000L

            val planDayId = when (sessionType) {
                "Push" -> pushDayId
                "Pull" -> pullDayId
                "Legs" -> legsDayId
                else -> null
            }

            workoutSessionDao.insertSession(
                WorkoutSessionEntity(
                    id = sessionId,
                    userId = uid,
                    planDayId = planDayId,
                    startTime = startMillis,
                    endTime = endMillis,
                    notes = null,
                    totalPoints = (80..150).random(),
                    isCompleted = 1
                )
            )

            // Progress factor: weeks 1-4 gradually increase weights
            val progressFactor = 1.0 + (sessionIndex / sessionSchedule.size.toDouble()) * 0.15

            when (sessionType) {
                "Push" -> seedPushSession(uid, sessionId, startMillis, benchId, ohpId, tricepId, progressFactor)
                "Pull" -> seedPullSession(uid, sessionId, startMillis, deadliftId, rowId, curlId, progressFactor)
                "Legs" -> seedLegsSession(uid, sessionId, startMillis, squatId, progressFactor)
                else   -> seedFullBodySession(uid, sessionId, startMillis, benchId, squatId, rowId, progressFactor)
            }
            sessionIndex++
        }

        // Seed PR records (from best sets in the last sessions)
        seedPRRecords(uid, latestSessionId, benchId, squatId, deadliftId, ohpId, rowId)

        // Seed streaks
        val yesterday = today.minusDays(1).toString()
        streakDao.insertStreak(StreakEntity(uid, 14, 14, yesterday))

        // Seed 6 achievements
        val achievementBadges = listOf("first_rep", "on_a_roll", "pr_machine", "plan_master", "heavy_bench", "heavy_squat")
        val baseUnlockTime = System.currentTimeMillis() - 28 * 86400000L
        achievementBadges.forEachIndexed { i, badgeId ->
            val existing = achievementDao.getAchievementByBadgeId(uid, badgeId)
            if (existing == null) {
                achievementDao.insertAchievement(
                    AchievementEntity(
                        id = UUID.randomUUID().toString(),
                        userId = uid,
                        badgeId = badgeId,
                        unlockedAt = baseUnlockTime + i * 3 * 86400000L
                    )
                )
            }
        }

        // Seed body weight logs (21 days)
        val bodyWeights = listOf(
            78.0, 77.8, 77.9, 77.6, 77.7, 77.5, 77.4,
            77.3, 77.2, 77.1, 77.0, 76.9, 77.0, 76.8,
            76.7, 76.8, 76.6, 76.5, 76.6, 76.5, 76.5
        )
        for (i in bodyWeights.indices) {
            val date = today.minusDays((bodyWeights.size - i).toLong()).toString()
            val existing = bodyWeightLogDao.getWeightLogForDate(uid, date)
            if (existing == null) {
                bodyWeightLogDao.insertWeightLog(
                    BodyWeightLogEntity(
                        id = UUID.randomUUID().toString(),
                        userId = uid,
                        weightKg = bodyWeights[i],
                        loggedDate = date,
                        loggedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        Log.d(tag, "Member Room data seeded for uid=$uid")
    }

    private fun buildSessionSchedule(today: LocalDate): List<Pair<LocalDate, String>> {
        val schedule = mutableListOf<Pair<LocalDate, String>>()
        val types = listOf("Push", "Pull", "Legs", "Full")
        for (weekOffset in 4 downTo 1) {
            // Mon/Tue/Thu/Sat = 0/1/3/5 days offset from Monday
            val monday = today.minusWeeks(weekOffset.toLong())
                .let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
            val dayOffsets = listOf(0L, 1L, 3L, 5L)
            dayOffsets.forEachIndexed { i, offset ->
                schedule.add(monday.plusDays(offset) to types[i])
            }
        }
        return schedule
    }

    private suspend fun ensureExercisesExist(): Map<String, String> {
        val exercises = mapOf(
            "Barbell Bench Press"    to ("Chest"       to "Barbell"),
            "Barbell Squat"          to ("Quadriceps"  to "Barbell"),
            "Deadlift"               to ("Lower Back"  to "Barbell"),
            "Barbell Overhead Press" to ("Shoulders"   to "Barbell"),
            "Bent Over Barbell Row"  to ("Lats"        to "Barbell"),
            "Dumbbell Curl"          to ("Biceps"      to "Dumbbell"),
            "Tricep Pushdown"        to ("Triceps"     to "Cable")
        )
        val result = mutableMapOf<String, String>()
        for ((name, musclePair) in exercises) {
            val existing = exerciseDao.getExerciseByName(name)
            if (existing != null) {
                result[name] = existing.id
            } else {
                val id = UUID.randomUUID().toString()
                exerciseDao.insertExercise(
                    ExerciseEntity(
                        id = id,
                        name = name,
                        primaryMuscle = musclePair.first,
                        secondaryMuscles = null,
                        equipment = musclePair.second,
                        category = "Strength",
                        imageUrl = null,
                        thumbnailUrl = null,
                        muscleImageUrl = null,
                        isCustom = 0,
                        isVerified = 1
                    )
                )
                result[name] = id
            }
        }
        return result
    }

    private suspend fun createDemoPlan(
        uid: String,
        benchId: String, squatId: String, deadliftId: String,
        ohpId: String, rowId: String, curlId: String, tricepId: String
    ): String {
        val existing = workoutPlanDao.getActivePlan(uid)
        if (existing != null) return existing.id

        val planId = UUID.randomUUID().toString()
        workoutPlanDao.insertPlan(
            WorkoutPlanEntity(
                id = planId,
                userId = uid,
                planName = "Demo Training Plan",
                goal = "Muscle Gain",
                totalWeeks = 8,
                daysPerWeek = 4,
                schemaVersion = 1,
                isActive = 1,
                createdAt = System.currentTimeMillis()
            )
        )

        val daySpecs = listOf(
            Triple("Push Day", 1, listOf(benchId to "4×8", ohpId to "3×10", tricepId to "3×12")),
            Triple("Pull Day", 2, listOf(deadliftId to "4×5", rowId to "4×8", curlId to "3×12")),
            Triple("Leg Day",  3, listOf(squatId to "4×8")),
            Triple("Full Body", 4, listOf(benchId to "3×8", squatId to "3×8", rowId to "3×10"))
        )

        for ((label, dayNum, exerciseSpecs) in daySpecs) {
            val dayId = UUID.randomUUID().toString()
            planDayDao.insertPlanDay(PlanDayEntity(dayId, planId, dayNum, label))
            exerciseSpecs.forEachIndexed { idx, (exId, reps) ->
                planExerciseDao.insertPlanExercise(
                    PlanExerciseEntity(
                        id = UUID.randomUUID().toString(),
                        dayId = dayId,
                        exerciseId = exId,
                        sets = reps.split("×")[0].toInt(),
                        repsRange = reps.split("×")[1],
                        restSeconds = 90,
                        orderIndex = idx,
                        notes = null
                    )
                )
            }
        }

        return planId
    }

    private suspend fun seedPushSession(
        uid: String, sessionId: String, baseTime: Long,
        benchId: String, ohpId: String, tricepId: String,
        factor: Double
    ) {
        val benchWeight = (70 * factor).roundToHalf()
        val ohpWeight = (45 * factor).roundToHalf()
        val tricepWeight = (30 * factor).roundToHalf()

        insertSets(sessionId, benchId, 4, 8, benchWeight, baseTime)
        insertSets(sessionId, ohpId, 3, 10, ohpWeight, baseTime + 1000)
        insertSets(sessionId, tricepId, 3, 12, tricepWeight, baseTime + 2000)
    }

    private suspend fun seedPullSession(
        uid: String, sessionId: String, baseTime: Long,
        deadliftId: String, rowId: String, curlId: String,
        factor: Double
    ) {
        insertSets(sessionId, deadliftId, 4, 5, (100 * factor).roundToHalf(), baseTime)
        insertSets(sessionId, rowId, 4, 8, (60 * factor).roundToHalf(), baseTime + 1000)
        insertSets(sessionId, curlId, 3, 12, (15 * factor).roundToHalf(), baseTime + 2000)
    }

    private suspend fun seedLegsSession(
        uid: String, sessionId: String, baseTime: Long,
        squatId: String, factor: Double
    ) {
        insertSets(sessionId, squatId, 4, 8, (90 * factor).roundToHalf(), baseTime)
    }

    private suspend fun seedFullBodySession(
        uid: String, sessionId: String, baseTime: Long,
        benchId: String, squatId: String, rowId: String,
        factor: Double
    ) {
        insertSets(sessionId, benchId, 3, 8, (70 * factor).roundToHalf(), baseTime)
        insertSets(sessionId, squatId, 3, 8, (90 * factor).roundToHalf(), baseTime + 1000)
        insertSets(sessionId, rowId, 3, 10, (60 * factor).roundToHalf(), baseTime + 2000)
    }

    private suspend fun insertSets(
        sessionId: String, exerciseId: String,
        setCount: Int, repsPerSet: Int, weight: Double,
        baseTime: Long
    ) {
        for (i in 1..setCount) {
            loggedSetDao.insertSet(
                LoggedSetEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    setNumber = i,
                    weight = weight,
                    reps = repsPerSet,
                    isWarmup = 0,
                    isCompleted = 1,
                    isPersonalRecord = 0,
                    loggedAt = baseTime + i * 60000L
                )
            )
        }
    }

    private suspend fun seedPRRecords(
        uid: String, sessionId: String,
        benchId: String, squatId: String, deadliftId: String,
        ohpId: String, rowId: String
    ) {
        val prs = listOf(
            Triple(benchId,    80.0, 5),
            Triple(squatId,    100.0, 5),
            Triple(deadliftId, 110.0, 3),
            Triple(ohpId,      50.0,  8),
            Triple(rowId,      70.0,  8)
        )
        val now = System.currentTimeMillis()
        for ((exId, weight, reps) in prs) {
            val existing = prRecordDao.getBestOneRepMax(uid, exId)
            val oneRepMax = weight * (1 + reps / 30.0)
            if (existing == null || existing.oneRepMax < oneRepMax) {
                prRecordDao.insertPr(
                    PrRecordEntity(
                        id = UUID.randomUUID().toString(),
                        userId = uid,
                        exerciseId = exId,
                        repCount = reps,
                        weight = weight,
                        oneRepMax = oneRepMax,
                        achievedAt = now,
                        sessionId = sessionId
                    )
                )
            }
        }
        Log.d(tag, "Seeded PR records")
    }

    private fun Double.roundToHalf(): Double = (this * 2).toInt() / 2.0
}
