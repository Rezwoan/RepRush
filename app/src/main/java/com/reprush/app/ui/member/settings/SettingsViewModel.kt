package com.reprush.app.ui.member.settings

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.local.AppDatabase
import com.reprush.app.data.local.datastore.AppPreferences
import com.reprush.app.data.repository.SettingsRepository
import com.reprush.app.ui.auth.SignInActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appPreferences: AppPreferences,
    private val appDatabase: AppDatabase,
    private val auth: FirebaseAuth
) : ViewModel() {

    val restTimerDuration: LiveData<Int> = settingsRepository.restTimerDurationFlow.asLiveData()
    val autoTimerEnabled: LiveData<Boolean> = settingsRepository.autoTimerEnabledFlow.asLiveData()
    val weightUnit: LiveData<String> = settingsRepository.weightUnitFlow.asLiveData()
    val leaderboardOptIn: LiveData<Boolean> = settingsRepository.leaderboardOptInFlow.asLiveData()

    private val _signOutComplete = MutableLiveData(false)
    val signOutComplete: LiveData<Boolean> = _signOutComplete

    fun setRestTimerDuration(seconds: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setRestTimerDuration(seconds)
        }
    }

    fun setAutoTimerEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setAutoTimerEnabled(enabled)
        }
    }

    fun setWeightUnit(unit: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setWeightUnit(unit)
        }
    }

    fun setLeaderboardOptIn(optIn: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setLeaderboardOptIn(optIn)
        }
    }

    fun signOut(activity: Activity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appDatabase.clearAllTables()
                settingsRepository.clearAll()
                appPreferences.resetSyncState()
            } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                auth.signOut()
                val intent = Intent(activity, SignInActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                activity.startActivity(intent)
            }
        }
    }
}
