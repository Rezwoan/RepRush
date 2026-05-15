package com.reprush.app.data.gamification

import com.reprush.app.data.local.dao.StreakDao
import com.reprush.app.data.local.entity.StreakEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakCalculator @Inject constructor(
    private val streakDao: StreakDao
) {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun updateStreak(
        userId: String,
        sessionEndTime: Long,
        isPlanSession: Boolean
    ): StreakUpdate {
        val existing = streakDao.getStreakForUser(userId)

        if (!isPlanSession) {
            return StreakUpdate(
                currentStreak = existing?.currentStreak ?: 0,
                longestStreak = existing?.longestStreak ?: 0,
                isNewLongest = false
            )
        }

        val sessionDate = LocalDate.ofInstant(
            java.time.Instant.ofEpochMilli(sessionEndTime),
            ZoneId.systemDefault()
        )
        val todayStr = sessionDate.format(dateFormat)

        val newStreak: Int
        if (existing == null) {
            newStreak = 1
        } else {
            val lastDate = existing.lastWorkoutDate?.let {
                try { LocalDate.parse(it, dateFormat) } catch (_: Exception) { null }
            }
            newStreak = when {
                lastDate == null -> 1
                lastDate == sessionDate -> existing.currentStreak
                lastDate == sessionDate.minusDays(1) -> existing.currentStreak + 1
                else -> 1
            }
        }

        val longestStreak = maxOf(existing?.longestStreak ?: 0, newStreak)
        val isNewLongest = newStreak > (existing?.longestStreak ?: 0)

        streakDao.insertStreak(
            StreakEntity(
                userId = userId,
                currentStreak = newStreak,
                longestStreak = longestStreak,
                lastWorkoutDate = todayStr
            )
        )

        return StreakUpdate(
            currentStreak = newStreak,
            longestStreak = longestStreak,
            isNewLongest = isNewLongest
        )
    }
}
