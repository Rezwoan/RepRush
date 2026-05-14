package com.reprush.app.ui.admin.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.repository.AttendanceRepository
import com.reprush.app.data.repository.Member
import com.reprush.app.data.repository.MemberRepository
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val memberRepository: MemberRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _activeMembers = MutableLiveData<List<Member>>()
    val activeMembers: LiveData<List<Member>> = _activeMembers

    private val _todayCheckedIn = MutableLiveData<Set<String>>()
    val todayCheckedIn: LiveData<Set<String>> = _todayCheckedIn

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operationResult = MutableLiveData<Result<Unit>?>(null)
    val operationResult: LiveData<Result<Unit>?> = _operationResult

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            when (val result = memberRepository.getMembers("active")) {
                is Result.Success -> _activeMembers.postValue(result.data)
                is Result.Error -> _activeMembers.postValue(emptyList())
            }
            when (val result = attendanceRepository.getTodayAttendance()) {
                is Result.Success -> {
                    _todayCheckedIn.postValue(result.data.map { it.memberId }.toSet())
                }
                is Result.Error -> _todayCheckedIn.postValue(emptySet())
            }
            _isLoading.postValue(false)
        }
    }

    fun markAttendance(memberId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminUid = auth.currentUser?.uid ?: ""
            val result = attendanceRepository.markAttendance(memberId, adminUid)
            _operationResult.postValue(result)
            if (result is Result.Success) {
                val current = _todayCheckedIn.value.orEmpty().toMutableSet()
                current.add(memberId)
                _todayCheckedIn.postValue(current)
            }
        }
    }

    fun clearOperationResult() {
        _operationResult.postValue(null)
    }
}
