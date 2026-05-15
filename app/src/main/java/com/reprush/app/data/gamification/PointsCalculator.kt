package com.reprush.app.data.gamification

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PointsCalculator @Inject constructor() {

    companion object {
        const val ATTENDANCE_POINTS = 50
        const val POINTS_PER_EXERCISE = 10
        const val POINTS_PER_WORKING_SET = 2
        const val POINTS_PER_PR = 100
        const val DAILY_CAP = 500
        const val STREAK_7_BONUS = 200
        const val STREAK_30_BONUS = 1000
        const val MIN_SESSION_MINUTES = 10L
    }

    data class PointsBreakdown(
        val attendancePoints: Int,
        val exercisePoints: Int,
        val setPoints: Int,
        val prPoints: Int,
        val capApplied: Boolean,
        val streakBonus: Int,
        val totalPoints: Int
    )

    fun calculate(
        durationMinutes: Long,
        exerciseCount: Int,
        workingSetCount: Int,
        prCount: Int,
        currentStreak: Int
    ): PointsBreakdown {
        val attendance = if (durationMinutes >= MIN_SESSION_MINUTES) ATTENDANCE_POINTS else 0
        val exercises = exerciseCount * POINTS_PER_EXERCISE
        val sets = workingSetCount * POINTS_PER_WORKING_SET
        val prs = prCount * POINTS_PER_PR

        val rawPoints = attendance + exercises + sets + prs
        val capApplied = rawPoints > DAILY_CAP
        val capped = if (capApplied) DAILY_CAP else rawPoints

        val streakBonus = when {
            currentStreak >= 30 -> STREAK_30_BONUS
            currentStreak >= 7 -> STREAK_7_BONUS
            else -> 0
        }

        return PointsBreakdown(
            attendancePoints = attendance,
            exercisePoints = exercises,
            setPoints = sets,
            prPoints = prs,
            capApplied = capApplied,
            streakBonus = streakBonus,
            totalPoints = capped + streakBonus
        )
    }
}
