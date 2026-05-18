package com.reprush.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

data class MemberProfile(
    val heightCm: Float?,
    val weightKg: Float?,
    val experience: String?,
    val lastExercised: String?,
    val squatKg: Float?,
    val benchKg: Float?,
    val deadliftKg: Float?
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val IS_LIBRARY_SYNCED = booleanPreferencesKey("is_library_synced")
    private val IS_MEMBER_ONBOARDING_COMPLETE = booleanPreferencesKey("is_member_onboarding_complete")
    private val ONBOARDING_HEIGHT_CM = floatPreferencesKey("onboarding_height_cm")
    private val ONBOARDING_WEIGHT_KG = floatPreferencesKey("onboarding_weight_kg")
    private val ONBOARDING_EXPERIENCE = stringPreferencesKey("onboarding_experience")
    private val ONBOARDING_LAST_EXERCISED = stringPreferencesKey("onboarding_last_exercised")
    private val ONBOARDING_SQUAT_1RM = floatPreferencesKey("onboarding_squat_1rm")
    private val ONBOARDING_BENCH_1RM = floatPreferencesKey("onboarding_bench_1rm")
    private val ONBOARDING_DEADLIFT_1RM = floatPreferencesKey("onboarding_deadlift_1rm")

    val isLibrarySynced: Flow<Boolean> = context.dataStore.data.map { it[IS_LIBRARY_SYNCED] ?: false }

    suspend fun setLibrarySynced(synced: Boolean) {
        context.dataStore.edit { it[IS_LIBRARY_SYNCED] = synced }
    }

    suspend fun resetSyncState() {
        context.dataStore.edit { it[IS_LIBRARY_SYNCED] = false }
    }

    val isMemberOnboardingComplete: Flow<Boolean> = context.dataStore.data.map {
        it[IS_MEMBER_ONBOARDING_COMPLETE] ?: false
    }

    suspend fun saveMemberOnboarding(
        heightCm: Float,
        weightKg: Float,
        experience: String,
        lastExercised: String,
        squatKg: Float?,
        benchKg: Float?,
        deadliftKg: Float?
    ) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_HEIGHT_CM] = heightCm
            prefs[ONBOARDING_WEIGHT_KG] = weightKg
            prefs[ONBOARDING_EXPERIENCE] = experience
            prefs[ONBOARDING_LAST_EXERCISED] = lastExercised
            if (squatKg != null) prefs[ONBOARDING_SQUAT_1RM] = squatKg
            if (benchKg != null) prefs[ONBOARDING_BENCH_1RM] = benchKg
            if (deadliftKg != null) prefs[ONBOARDING_DEADLIFT_1RM] = deadliftKg
            prefs[IS_MEMBER_ONBOARDING_COMPLETE] = true
        }
    }

    suspend fun getMemberProfile(): MemberProfile {
        val prefs = context.dataStore.data.first()
        return MemberProfile(
            heightCm = prefs[ONBOARDING_HEIGHT_CM],
            weightKg = prefs[ONBOARDING_WEIGHT_KG],
            experience = prefs[ONBOARDING_EXPERIENCE]?.takeIf { it.isNotBlank() },
            lastExercised = prefs[ONBOARDING_LAST_EXERCISED]?.takeIf { it.isNotBlank() },
            squatKg = prefs[ONBOARDING_SQUAT_1RM],
            benchKg = prefs[ONBOARDING_BENCH_1RM],
            deadliftKg = prefs[ONBOARDING_DEADLIFT_1RM]
        )
    }
}
