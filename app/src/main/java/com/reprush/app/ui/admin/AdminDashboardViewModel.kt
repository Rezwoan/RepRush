package com.reprush.app.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.repository.AttendanceRepository
import com.reprush.app.data.repository.GymSettingsRepository
import com.reprush.app.data.repository.MemberRepository
import com.reprush.app.data.repository.PaymentRepository
import com.reprush.app.data.repository.Result
import com.reprush.app.service.ExpiryNotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository,
    private val gymSettingsRepository: GymSettingsRepository,
    private val expiryNotificationService: ExpiryNotificationService
) : ViewModel() {

    private val _totalMembers = MutableLiveData(0)
    val totalMembers: LiveData<Int> = _totalMembers

    private val _activeMembers = MutableLiveData(0)
    val activeMembers: LiveData<Int> = _activeMembers

    private val _pendingMembers = MutableLiveData(0)
    val pendingMembers: LiveData<Int> = _pendingMembers

    private val _todayCheckIns = MutableLiveData(0)
    val todayCheckIns: LiveData<Int> = _todayCheckIns

    private val _monthlyRevenue = MutableLiveData(0.0)
    val monthlyRevenue: LiveData<Double> = _monthlyRevenue

    private val _yearlyRevenue = MutableLiveData(0.0)
    val yearlyRevenue: LiveData<Double> = _yearlyRevenue

    fun loadStats() {
        runBackgroundJobs()
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = memberRepository.getMembers()) {
                is Result.Success -> {
                    _totalMembers.postValue(result.data.size)
                    _activeMembers.postValue(result.data.count { it.membershipStatus == "active" })
                    _pendingMembers.postValue(result.data.count { it.membershipStatus == "pending" })
                }
                is Result.Error -> {}
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            when (val result = attendanceRepository.getCheckedInTodayCount()) {
                is Result.Success -> _todayCheckIns.postValue(result.data)
                is Result.Error -> {}
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val currentMonth = YearMonth.now()
            when (val result = paymentRepository.getMonthlyRevenue(currentMonth)) {
                is Result.Success -> _monthlyRevenue.postValue(result.data)
                is Result.Error -> {}
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val currentYear = YearMonth.now().year
            when (val result = paymentRepository.getYearlyRevenue(currentYear)) {
                is Result.Success -> _yearlyRevenue.postValue(result.data)
                is Result.Error -> {}
            }
        }
    }

    private fun runBackgroundJobs() {
        viewModelScope.launch(Dispatchers.IO) {
            try { expiryNotificationService.checkAndNotify() } catch (_: Exception) {}
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (val settingsResult = gymSettingsRepository.getSettings()) {
                    is Result.Success -> {
                        if (settingsResult.data.autoSuspensionEnabled) {
                            gymSettingsRepository.runAutoSuspension(settingsResult.data.gracePeriodDays)
                        }
                    }
                    is Result.Error -> {}
                }
            } catch (_: Exception) {}
        }
    }
}
