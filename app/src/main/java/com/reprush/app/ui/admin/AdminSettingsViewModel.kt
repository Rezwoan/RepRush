package com.reprush.app.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.repository.GymSettings
import com.reprush.app.data.repository.GymSettingsRepository
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminSettingsViewModel @Inject constructor(
    private val settingsRepository: GymSettingsRepository
) : ViewModel() {

    private val _settings = MutableLiveData<GymSettings>()
    val settings: LiveData<GymSettings> = _settings

    private val _saveResult = MutableLiveData<Result<Unit>?>(null)
    val saveResult: LiveData<Result<Unit>?> = _saveResult

    private val _suspensionResult = MutableLiveData<Result<Int>?>(null)
    val suspensionResult: LiveData<Result<Int>?> = _suspensionResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadSettings() {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = settingsRepository.getSettings()) {
                is Result.Success -> _settings.postValue(result.data)
                is Result.Error -> _settings.postValue(GymSettings())
            }
            _isLoading.postValue(false)
        }
    }

    fun saveSettings(autoSuspensionEnabled: Boolean, gracePeriodDays: Int) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val settings = GymSettings(autoSuspensionEnabled, gracePeriodDays)
            val result = settingsRepository.updateSettings(settings)
            _saveResult.postValue(result)
            if (result is Result.Success) {
                _settings.postValue(settings)
            }
            _isLoading.postValue(false)
        }
    }

    fun runAutoSuspensionNow() {
        val graceDays = _settings.value?.gracePeriodDays ?: 3
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = settingsRepository.runAutoSuspension(graceDays)
            _suspensionResult.postValue(result)
            _isLoading.postValue(false)
        }
    }

    fun clearSaveResult() { _saveResult.value = null }
    fun clearSuspensionResult() { _suspensionResult.value = null }
}
