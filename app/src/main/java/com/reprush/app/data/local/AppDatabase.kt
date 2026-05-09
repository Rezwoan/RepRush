package com.reprush.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.reprush.app.data.local.dao.*
import com.reprush.app.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        MembershipPackageEntity::class,
        PaymentRecordEntity::class,
        AttendanceLogEntity::class,
        AnnouncementEntity::class,
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        PlanDayEntity::class,
        PlanExerciseEntity::class,
        WorkoutSessionEntity::class,
        LoggedSetEntity::class,
        PrRecordEntity::class,
        BodyWeightLogEntity::class,
        StreakEntity::class,
        AchievementEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun membershipPackageDao(): MembershipPackageDao
    abstract fun paymentRecordDao(): PaymentRecordDao
    abstract fun attendanceLogDao(): AttendanceLogDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun planDayDao(): PlanDayDao
    abstract fun planExerciseDao(): PlanExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun loggedSetDao(): LoggedSetDao
    abstract fun prRecordDao(): PrRecordDao
    abstract fun bodyWeightLogDao(): BodyWeightLogDao
    abstract fun streakDao(): StreakDao
    abstract fun achievementDao(): AchievementDao
    abstract fun chatMessageDao(): ChatMessageDao
}