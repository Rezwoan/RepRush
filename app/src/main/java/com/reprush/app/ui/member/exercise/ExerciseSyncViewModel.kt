package com.reprush.app.ui.member.exercise

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SyncState { IDLE, SYNCING, DONE, ERROR }

@HiltViewModel
class ExerciseSyncViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _syncState = MutableLiveData(SyncState.IDLE)
    val syncState: LiveData<SyncState> = _syncState

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun startSync() {
        if (_syncState.value == SyncState.SYNCING) return
        _syncState.value = SyncState.SYNCING
        viewModelScope.launch(Dispatchers.IO) {
            val result = exerciseRepository.syncExercises()
            when (result) {
                is com.reprush.app.data.repository.Result.Success -> {
                    _syncState.postValue(SyncState.DONE)
                }
                is com.reprush.app.data.repository.Result.Error -> {
                    _errorMessage.postValue(result.message)
                    _syncState.postValue(SyncState.ERROR)
                }
            }
        }
    }
}
