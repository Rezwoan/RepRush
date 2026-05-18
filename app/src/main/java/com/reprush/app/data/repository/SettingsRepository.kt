package com.reprush.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore: DataStore<Preferences>
    by preferencesDataStore(name = "settings_prefs")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val REST_TIMER_DURATION = intPreferencesKey("rest_timer_duration")
    private val AUTO_TIMER_ENABLED = booleanPreferencesKey("auto_timer_enabled")
    private val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    private val LEADERBOARD_OPT_IN = booleanPreferencesKey("leaderboard_opt_in")

    val restTimerDurationFlow: Flow<Int> = context.settingsStore.data.map { prefs ->
        prefs[REST_TIMER_DURATION] ?: 90
    }

    val autoTimerEnabledFlow: Flow<Boolean> = context.settingsStore.data.map { prefs ->
        prefs[AUTO_TIMER_ENABLED] ?: true
    }

    val weightUnitFlow: Flow<String> = context.settingsStore.data.map { prefs ->
        prefs[WEIGHT_UNIT] ?: "kg"
    }

    val leaderboardOptInFlow: Flow<Boolean> = context.settingsStore.data.map { prefs ->
        prefs[LEADERBOARD_OPT_IN] ?: true
    }

    suspend fun setRestTimerDuration(seconds: Int) {
        context.settingsStore.edit { prefs ->
            prefs[REST_TIMER_DURATION] = seconds
        }
    }

    suspend fun setAutoTimerEnabled(enabled: Boolean) {
        context.settingsStore.edit { prefs ->
            prefs[AUTO_TIMER_ENABLED] = enabled
        }
    }

    suspend fun setWeightUnit(unit: String) {
        context.settingsStore.edit { prefs ->
            prefs[WEIGHT_UNIT] = unit
        }
    }

    suspend fun setLeaderboardOptIn(optIn: Boolean) {
        context.settingsStore.edit { prefs ->
            prefs[LEADERBOARD_OPT_IN] = optIn
        }
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid)
                .update("leaderboardOptIn", optIn)
                .await()
        } catch (e: Exception) {
            Log.e("SettingsRepository", "Failed to update leaderboardOptIn in Firestore", e)
        }
    }

    suspend fun clearAll() {
        context.settingsStore.edit { it.clear() }
    }
}
