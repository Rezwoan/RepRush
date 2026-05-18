package com.reprush.app.ui.member.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.reprush.app.data.repository.NotificationItem
import com.reprush.app.data.repository.NotificationRepository
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _notifications = MutableLiveData<List<NotificationItem>>()
    val notifications: LiveData<List<NotificationItem>> = _notifications

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private var unreadCountListener: ListenerRegistration? = null

    init {
        startUnreadCountListener()
    }

    private fun startUnreadCountListener() {
        val uid = auth.currentUser?.uid ?: return
        unreadCountListener?.remove()
        unreadCountListener = notificationRepository.listenUnreadCount(uid) { count ->
            _unreadCount.postValue(count)
        }
    }

    fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = notificationRepository.getNotifications(uid)) {
                is Result.Success -> {
                    _notifications.postValue(result.data)
                    _unreadCount.postValue(result.data.count { !it.isRead })
                }
                is Result.Error -> {
                    _notifications.postValue(emptyList())
                    _unreadCount.postValue(0)
                }
            }
            _isLoading.postValue(false)
        }
    }

    fun markAsRead(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.markAsRead(uid, notificationId)
            loadNotifications()
        }
    }

    fun markAllAsRead() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.markAllAsRead(uid)
            loadNotifications()
        }
    }

    override fun onCleared() {
        super.onCleared()
        unreadCountListener?.remove()
    }
}
