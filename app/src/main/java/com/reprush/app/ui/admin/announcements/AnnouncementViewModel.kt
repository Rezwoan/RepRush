package com.reprush.app.ui.admin.announcements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.repository.Announcement
import com.reprush.app.data.repository.AnnouncementRepository
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnnouncementViewModel @Inject constructor(
    private val announcementRepository: AnnouncementRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _postResult = MutableLiveData<Result<Int>?>(null)
    val postResult: LiveData<Result<Int>?> = _postResult

    private val _announcements = MutableLiveData<List<Announcement>>()
    val announcements: LiveData<List<Announcement>> = _announcements

    private val _deleteResult = MutableLiveData<Result<Unit>?>(null)
    val deleteResult: LiveData<Result<Unit>?> = _deleteResult

    fun postAnnouncement(title: String, body: String) {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val postResult = announcementRepository.postAnnouncement(title, body, uid)
            when (postResult) {
                is Result.Success -> {
                    val fanOutResult = announcementRepository.fanOutNotification(
                        title, body, postResult.data
                    )
                    _postResult.postValue(fanOutResult)
                }
                is Result.Error -> {
                    _postResult.postValue(Result.Error(postResult.message))
                }
            }
            _isLoading.postValue(false)
        }
    }

    fun loadAnnouncements() {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = announcementRepository.getAnnouncements()) {
                is Result.Success -> _announcements.postValue(result.data)
                is Result.Error -> _announcements.postValue(emptyList())
            }
            _isLoading.postValue(false)
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = announcementRepository.deleteAnnouncement(announcementId)
            _deleteResult.postValue(result)
        }
    }

    fun clearPostResult() { _postResult.value = null }
    fun clearDeleteResult() { _deleteResult.value = null }
}
