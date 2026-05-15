package com.reprush.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val IS_LIBRARY_SYNCED = booleanPreferencesKey("is_library_synced")
    private val SYNC_PROGRESS_COUNT = intPreferencesKey("sync_progress_count")

    val isLibrarySynced: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_LIBRARY_SYNCED] ?: false
    }

    val syncProgressCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SYNC_PROGRESS_COUNT] ?: 0
    }

    suspend fun setLibrarySynced(synced: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_LIBRARY_SYNCED] = synced
        }
    }

    suspend fun setSyncProgressCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[SYNC_PROGRESS_COUNT] = count
        }
    }

    suspend fun resetSyncState() {
        context.dataStore.edit { prefs ->
            prefs[IS_LIBRARY_SYNCED] = false
            prefs[SYNC_PROGRESS_COUNT] = 0
        }
    }
}
