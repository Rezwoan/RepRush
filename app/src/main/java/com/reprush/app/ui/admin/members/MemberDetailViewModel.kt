package com.reprush.app.ui.admin.members

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.repository.MemberDetail
import com.reprush.app.data.repository.MemberRepository
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _member = MutableLiveData<MemberDetail>()
    val member: LiveData<MemberDetail> = _member

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operationResult = MutableLiveData<Result<Unit>>()
    val operationResult: LiveData<Result<Unit>> = _operationResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadMember(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            when (val result = memberRepository.getMemberById(uid)) {
                is Result.Success -> _member.postValue(result.data)
                is Result.Error -> _error.postValue(result.message)
            }
            _isLoading.postValue(false)
        }
    }

    fun suspendMember(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = memberRepository.suspendMember(uid)
            _operationResult.postValue(result)
            if (result is Result.Success) loadMember(uid)
            else _isLoading.postValue(false)
        }
    }

    fun reactivateMember(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = memberRepository.reactivateMember(uid)
            _operationResult.postValue(result)
            if (result is Result.Success) loadMember(uid)
            else _isLoading.postValue(false)
        }
    }

    fun removeMember(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = memberRepository.removeMember(uid)
            _operationResult.postValue(result)
            _isLoading.postValue(false)
        }
    }
}