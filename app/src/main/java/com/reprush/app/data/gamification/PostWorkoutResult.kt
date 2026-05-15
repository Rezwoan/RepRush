package com.reprush.app.data.gamification

data class PostWorkoutResult(
    val sessionId: String,
    val pointsBreakdown: PointsCalculator.PointsBreakdown,
    val newPRs: List<NewPR>,
    val newAchievements: List<String>,
    val streakUpdate: StreakUpdate
)

data class NewPR(
    val setId: String,
    val exerciseName: String,
    val exerciseId: String,
    val reps: Int,
    val weight: Double
)

data class StreakUpdate(
    val currentStreak: Int,
    val longestStreak: Int,
    val isNewLongest: Boolean
)
