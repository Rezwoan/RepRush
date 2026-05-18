package com.reprush.app.ui.member.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.local.dao.AchievementDao
import com.reprush.app.data.local.dao.LoggedSetDao
import com.reprush.app.data.local.dao.MembershipPackageDao
import com.reprush.app.data.local.dao.PrRecordDao
import com.reprush.app.data.local.dao.StreakDao
import com.reprush.app.data.local.dao.UserDao
import com.reprush.app.data.local.dao.WorkoutPlanDao
import com.reprush.app.data.local.dao.WorkoutSessionDao
import com.reprush.app.data.local.entity.UserEntity
import com.reprush.app.data.local.entity.WorkoutPlanEntity
import com.reprush.app.data.repository.GameRepository
import com.reprush.app.data.repository.Result
import com.reprush.app.ui.member.home.MembershipDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ProfileStats(
    val totalWorkouts: Int,
    val totalPRs: Int,
    val totalVolumeKg: Double,
    val longestStreak: Int,
    val currentStreak: Int,
    val totalPoints: Int
)

data class BadgeDefinition(
    val id: String,
    val name: String,
    val description: String,
    val unlockCondition: String,
    val icon: String
)

data class AchievementDisplayItem(
    val badge: BadgeDefinition,
    val isUnlocked: Boolean,
    val unlockedAt: Long?
)

object BadgeDefinitions {
    val ALL = listOf(
        BadgeDefinition("first_rep",      "First Rep",       "Complete your first workout",       "Complete 1 workout",                      "🏋️"),
        BadgeDefinition("on_a_roll",      "On a Roll",       "Get a 3-day workout streak",        "Achieve a 3-day streak",                  "🔥"),
        BadgeDefinition("unstoppable",    "Unstoppable",     "Get a 7-day workout streak",        "Achieve a 7-day streak",                  "⚡"),
        BadgeDefinition("century",        "Century",         "Complete 100 workouts",              "Complete 100 workouts",                   "💯"),
        BadgeDefinition("pr_machine",     "PR Machine",      "Log 10 personal records",           "Log 10 personal records",                 "🏆"),
        BadgeDefinition("plan_master",    "Plan Master",     "Complete a full training plan",      "Complete all days of a plan",             "📋"),
        BadgeDefinition("heavy_bench",    "Heavy Bench",     "Bench press 1RM ≥ 100 kg",          "Bench Press 1RM ≥ 100 kg",               "💪"),
        BadgeDefinition("heavy_squat",    "Heavy Squat",     "Squat 1RM ≥ 140 kg",               "Squat 1RM ≥ 140 kg",                      "🏗️"),
        BadgeDefinition("heavy_deadlift", "Heavy Deadlift",  "Deadlift 1RM ≥ 180 kg",            "Deadlift 1RM ≥ 180 kg",                   "⛏️"),
        BadgeDefinition("comeback",       "Comeback",        "Return after a 14-day break",       "Return after a 14-day break",             "↩️"),
        BadgeDefinition("full_house",     "Full House",      "Train every muscle group in a week","Train every muscle group in one week",    "🌟")
    )
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val workoutSessionDao: WorkoutSessionDao,
    private val prRecordDao: PrRecordDao,
    private val loggedSetDao: LoggedSetDao,
    private val streakDao: StreakDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val achievementDao: AchievementDao,
    private val userDao: UserDao,
    private val membershipPackageDao: MembershipPackageDao,
    private val gameRepository: GameRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _profileStats = MutableLiveData<ProfileStats>()
    val profileStats: LiveData<ProfileStats> = _profileStats

    private val _achievements = MutableLiveData<List<AchievementDisplayItem>>()
    val achievements: LiveData<List<AchievementDisplayItem>> = _achievements

    private val _activePlan = MutableLiveData<WorkoutPlanEntity?>()
    val activePlan: LiveData<WorkoutPlanEntity?> = _activePlan

    private val _user = MutableLiveData<UserEntity?>()
    val user: LiveData<UserEntity?> = _user

    private val _membershipDisplay = MutableLiveData<MembershipDisplay?>()
    val membershipDisplay: LiveData<MembershipDisplay?> = _membershipDisplay

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val totalWorkouts = workoutSessionDao.getCompletedSessionCount(uid)
            val totalPRs = prRecordDao.getTotalPrCount(uid)
            val totalVolume = loggedSetDao.getTotalVolume(uid)
            val streak = streakDao.getStreakForUser(uid)
            val longestStreak = streak?.longestStreak ?: 0

            val fsData = (gameRepository.getProfileFirestoreData(uid) as? Result.Success)?.data
            val currentStreak = fsData?.currentStreak ?: streak?.currentStreak ?: 0
            val totalPoints = fsData?.totalPoints ?: 0

            _profileStats.postValue(ProfileStats(
                totalWorkouts = totalWorkouts,
                totalPRs = totalPRs,
                totalVolumeKg = totalVolume,
                longestStreak = longestStreak,
                currentStreak = currentStreak,
                totalPoints = totalPoints
            ))

            val unlockedEntities = achievementDao.getAchievementsForUser(uid)
            val unlockedMap = unlockedEntities.associateBy { it.badgeId }
            val displayItems = BadgeDefinitions.ALL.map { badge ->
                val unlocked = unlockedMap[badge.id]
                AchievementDisplayItem(
                    badge = badge,
                    isUnlocked = unlocked != null,
                    unlockedAt = unlocked?.unlockedAt
                )
            }
            _achievements.postValue(displayItems)

            val plan = workoutPlanDao.getActivePlan(uid)
            _activePlan.postValue(plan)

            val roomUser = userDao.getUserById(uid)
            _user.postValue(roomUser)

            val gameResult = gameRepository.getUserData(uid)
            val gameUserData = (gameResult as? Result.Success)?.data
            val endDateStr = roomUser?.membershipEndDate?.takeIf { it.isNotBlank() }
                ?: gameUserData?.membershipEndDate
            val pkgId = roomUser?.packageId?.takeIf { it.isNotBlank() }
                ?: gameUserData?.packageId

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
            _isLoading.postValue(false)
        }
    }
}
