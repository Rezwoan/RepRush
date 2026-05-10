package com.reprush.app.ui.admin.members

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.repository.Member
import com.reprush.app.data.repository.MemberRepository
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemberDirectoryViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _allMembers = MutableLiveData<List<Member>>()

    private val _filteredMembers = MutableLiveData<List<Member>>()
    val filteredMembers: LiveData<List<Member>> = _filteredMembers

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var currentStatusFilter: String = "active"
    private var currentSearchQuery: String = ""

    fun loadMembers(statusFilter: String = "active") {
        currentStatusFilter = statusFilter
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            _error.postValue(null)
            when (val result = memberRepository.getMembers(statusFilter)) {
                is Result.Success -> {
                    _allMembers.postValue(result.data)
                    applySearch(result.data, currentSearchQuery)
                }
                is Result.Error -> {
                    _error.postValue(result.message)
                    _filteredMembers.postValue(emptyList())
                }
            }
            _isLoading.postValue(false)
        }
    }

    fun onStatusFilterChanged(status: String) {
        currentStatusFilter = status
        loadMembers(status)
    }

    fun onSearchQueryChanged(query: String) {
        currentSearchQuery = query
        val current = _allMembers.value ?: emptyList()
        applySearch(current, query)
    }

    private fun applySearch(members: List<Member>, query: String) {
        val result = if (query.isBlank()) {
            members
        } else {
            members.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
            }
        }
        _filteredMembers.postValue(result)
    }
}