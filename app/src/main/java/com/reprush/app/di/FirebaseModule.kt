package com.reprush.app.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.reprush.app.data.local.AppDatabase
import com.reprush.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "reprush_database"
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()
    @Provides fun provideWorkoutPlanDao(db: AppDatabase): WorkoutPlanDao = db.workoutPlanDao()
    @Provides fun providePlanDayDao(db: AppDatabase): PlanDayDao = db.planDayDao()
    @Provides fun providePlanExerciseDao(db: AppDatabase): PlanExerciseDao = db.planExerciseDao()
    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideWorkoutSessionDao(db: AppDatabase): WorkoutSessionDao = db.workoutSessionDao()
    @Provides fun provideLoggedSetDao(db: AppDatabase): LoggedSetDao = db.loggedSetDao()
    @Provides fun providePrRecordDao(db: AppDatabase): PrRecordDao = db.prRecordDao()
    @Provides fun provideStreakDao(db: AppDatabase): StreakDao = db.streakDao()
    @Provides fun provideAchievementDao(db: AppDatabase): AchievementDao = db.achievementDao()
}
