package com.reprush.app.ui.member.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.local.dao.ExerciseDao
import com.reprush.app.data.local.dao.MembershipPackageDao
import com.reprush.app.data.local.dao.PrRecordDao
import com.reprush.app.data.local.dao.StreakDao
import com.reprush.app.data.local.dao.UserDao
import com.reprush.app.data.local.entity.StreakEntity
import com.reprush.app.data.repository.GameRepository
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class PrDisplay(
    val exerciseName: String,
    val weight: Double,
    val repCount: Int
)

data class MembershipDisplay(
    val packageName: String,
    val endDateStr: String,
    val daysLeft: Long
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val streakDao: StreakDao,
    private val prRecordDao: PrRecordDao,
    private val exerciseDao: ExerciseDao,
    private val userDao: UserDao,
    private val membershipPackageDao: MembershipPackageDao,
    private val gameRepository: GameRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _streak = MutableLiveData<StreakEntity?>()
    val streak: LiveData<StreakEntity?> = _streak

    private val _recentPRs = MutableLiveData<List<PrDisplay>>()
    val recentPRs: LiveData<List<PrDisplay>> = _recentPRs

    private val _membershipDisplay = MutableLiveData<MembershipDisplay?>()
    val membershipDisplay: LiveData<MembershipDisplay?> = _membershipDisplay

    private val _monthlyPoints = MutableLiveData<Int>()
    val monthlyPoints: LiveData<Int> = _monthlyPoints

    private val _userRank = MutableLiveData<Int>()
    val userRank: LiveData<Int> = _userRank

    fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _streak.postValue(streakDao.getStreakForUser(uid))

            // Bug 5: resolve exercise name before posting to LiveData
            val rawPRs = prRecordDao.getRecentPRs(uid, 3)
            val prDisplays = rawPRs.mapNotNull { pr ->
                val ex = exerciseDao.getExerciseById(pr.exerciseId)
                if (ex != null) PrDisplay(ex.name, pr.weight, pr.repCount) else null
            }
            _recentPRs.postValue(prDisplays)

            // Bug 6: resolve package name from MembershipPackageDao
            val user = userDao.getUserById(uid)
            if (user?.membershipEndDate != null) {
                val packageName = user.packageId?.let { pid ->
                    membershipPackageDao.getPackageById(pid)?.name
                } ?: "Membership"
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val endDate = try { sdf.parse(user.membershipEndDate) } catch (_: Exception) { null }
                val daysLeft = if (endDate != null)
                    TimeUnit.MILLISECONDS.toDays(endDate.time - System.currentTimeMillis())
                else -1L
                _membershipDisplay.postValue(MembershipDisplay(packageName, user.membershipEndDate, daysLeft))
            } else {
                _membershipDisplay.postValue(null)
            }

            // Bug 3: monthly points from Firestore users/{uid}
            when (val result = gameRepository.getMonthlyPoints(uid)) {
                is Result.Success -> _monthlyPoints.postValue(result.data)
                is Result.Error -> _monthlyPoints.postValue(0)
            }

            // Bug 4: user rank from Firestore leaderboard
            when (val result = gameRepository.getLeaderboard()) {
                is Result.Success -> {
                    val rank = result.data.indexOfFirst { it.uid == uid }
                    _userRank.postValue(if (rank >= 0) rank + 1 else -1)
                }
                is Result.Error -> _userRank.postValue(-1)
            }
        }
    }
}
