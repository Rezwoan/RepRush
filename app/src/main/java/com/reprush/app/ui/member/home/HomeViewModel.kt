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
import com.reprush.app.data.repository.SessionRepository
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
    private val sessionRepository: SessionRepository,
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

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userPhotoUrl = MutableLiveData<String?>()
    val userPhotoUrl: LiveData<String?> = _userPhotoUrl

    fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            sessionRepository.syncSessionsFromFirestore(uid)

            _streak.postValue(streakDao.getStreakForUser(uid))

            // Resolve exercise names for recent PRs (Bug 5)
            val rawPRs = prRecordDao.getRecentPRs(uid, 3)
            val prDisplays = rawPRs.mapNotNull { pr ->
                val ex = exerciseDao.getExerciseById(pr.exerciseId)
                if (ex != null) PrDisplay(ex.name, pr.weight, pr.repCount) else null
            }
            _recentPRs.postValue(prDisplays)

            // Read Room user first, then Firestore for anything missing
            val roomUser = userDao.getUserById(uid)
            val fsData = (gameRepository.getUserData(uid) as? Result.Success)?.data

            _userName.postValue(roomUser?.displayName ?: fsData?.displayName ?: "")
            _userPhotoUrl.postValue(roomUser?.photoUrl ?: auth.currentUser?.photoUrl?.toString())

            // Monthly points from Firestore (Bug 3)
            _monthlyPoints.postValue(fsData?.monthlyPoints ?: 0)

            // Membership: prefer Room, fall back to Firestore (Bug 4 / Bug 6)
            val endDateStr = roomUser?.membershipEndDate?.takeIf { it.isNotBlank() }
                ?: fsData?.membershipEndDate
            val pkgId = roomUser?.packageId?.takeIf { it.isNotBlank() }
                ?: fsData?.packageId

            if (!endDateStr.isNullOrBlank()) {
                val packageName = pkgId?.let { pid ->
                    membershipPackageDao.getPackageById(pid)?.name
                } ?: "Membership"
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val endDate = try { sdf.parse(endDateStr) } catch (_: Exception) { null }
                val daysLeft = if (endDate != null)
                    TimeUnit.MILLISECONDS.toDays(endDate.time - System.currentTimeMillis())
                else -1L
                _membershipDisplay.postValue(MembershipDisplay(packageName, endDateStr, daysLeft))
            } else {
                _membershipDisplay.postValue(null)
            }

            // User rank from leaderboard (Bug 4 — rank badge)
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
