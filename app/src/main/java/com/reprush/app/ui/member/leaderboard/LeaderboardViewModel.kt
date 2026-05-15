package com.reprush.app.ui.member.leaderboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.repository.GameRepository
import com.reprush.app.data.repository.LeaderboardEntry
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RankedEntry(
    val rank: Int,
    val entry: LeaderboardEntry,
    val isCurrentUser: Boolean
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _entries = MutableLiveData<List<RankedEntry>>()
    val entries: LiveData<List<RankedEntry>> = _entries

    private val _currentUserRank = MutableLiveData<Int>()
    val currentUserRank: LiveData<Int> = _currentUserRank

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadLeaderboard() {
        _isLoading.value = true
        viewModelScope.launch {
            when (val result = gameRepository.getLeaderboard()) {
                is Result.Success -> {
                    val uid = auth.currentUser?.uid ?: ""
                    val ranked = result.data.mapIndexed { index, entry ->
                        RankedEntry(
                            rank = index + 1,
                            entry = entry,
                            isCurrentUser = entry.uid == uid
                        )
                    }
                    _entries.value = ranked
                    _currentUserRank.value = ranked.firstOrNull { it.isCurrentUser }?.rank ?: -1
                }
                is Result.Error -> _error.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }
}
