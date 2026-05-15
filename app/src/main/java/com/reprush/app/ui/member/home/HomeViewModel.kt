package com.reprush.app.ui.member.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.local.dao.PrRecordDao
import com.reprush.app.data.local.dao.StreakDao
import com.reprush.app.data.local.dao.UserDao
import com.reprush.app.data.local.entity.PrRecordEntity
import com.reprush.app.data.local.entity.StreakEntity
import com.reprush.app.data.local.entity.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val streakDao: StreakDao,
    private val prRecordDao: PrRecordDao,
    private val userDao: UserDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _streak = MutableLiveData<StreakEntity?>()
    val streak: LiveData<StreakEntity?> = _streak

    private val _recentPRs = MutableLiveData<List<PrRecordEntity>>()
    val recentPRs: LiveData<List<PrRecordEntity>> = _recentPRs

    private val _membershipInfo = MutableLiveData<UserEntity?>()
    val membershipInfo: LiveData<UserEntity?> = _membershipInfo

    fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _streak.postValue(streakDao.getStreakForUser(uid))
            _recentPRs.postValue(prRecordDao.getRecentPRs(uid, 3))
            _membershipInfo.postValue(userDao.getUserById(uid))
        }
    }
}
