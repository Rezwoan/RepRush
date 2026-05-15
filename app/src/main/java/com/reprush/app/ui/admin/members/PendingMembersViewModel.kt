package com.reprush.app.ui.admin.members

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.repository.MemberRepository
import com.reprush.app.data.repository.PendingMember
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingMembersViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _pendingMembers = MutableLiveData<List<PendingMember>>()
    val pendingMembers: LiveData<List<PendingMember>> = _pendingMembers

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Null means "no pending result" — consumers set it back to null after handling
    private val _operationResult = MutableLiveData<Result<Unit>?>(null)
    val operationResult: LiveData<Result<Unit>?> = _operationResult

    fun clearOperationResult() {
        _operationResult.postValue(null)
    }

    fun loadPendingMembers() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            _error.postValue(null)
            when (val result = memberRepository.getPendingMembers()) {
                is Result.Success -> _pendingMembers.postValue(result.data)
                is Result.Error -> {
                    _error.postValue(result.message)
                    _pendingMembers.postValue(emptyList())
                }
            }
            _isLoading.postValue(false)
        }
    }

    fun approveMember(uid: String, packageId: String, startDate: String, packageDurationDays: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = memberRepository.approveMember(uid, packageId, startDate, packageDurationDays)
            _operationResult.postValue(result)
            if (result is Result.Success) {
                loadPendingMembers()
            } else {
                _isLoading.postValue(false)
            }
        }
    }

    fun rejectMember(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = memberRepository.rejectMember(uid)
            _operationResult.postValue(result)
            if (result is Result.Success) {
                loadPendingMembers()
            } else {
                _isLoading.postValue(false)
            }
        }
    }

    fun registerMemberManually(displayName: String, email: String, phone: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = memberRepository.registerMemberManually(displayName, email, phone)
            _operationResult.postValue(result)
            _isLoading.postValue(false)
        }
    }

    fun registerMemberAsActive(
        displayName: String,
        email: String,
        phone: String,
        packageId: String,
        startDate: String,
        endDate: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = memberRepository.registerMemberAsActive(
                displayName, email, phone, packageId, startDate, endDate
            )
            _operationResult.postValue(result)
            _isLoading.postValue(false)
        }
    }
}