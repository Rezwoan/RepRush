package com.reprush.app.ui.member.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.local.dao.BodyWeightLogDao
import com.reprush.app.data.local.entity.BodyWeightLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class WeightEntry(val date: LocalDate, val weightKg: Double)
data class BodyStats(
    val startingWeight: Double?,
    val currentWeight: Double?,
    val monthlyChange: Double?,
    val allTimeChange: Double?,
    val lowestWeight: Double?
)

@HiltViewModel
class BodyViewModel @Inject constructor(
    private val bodyWeightLogDao: BodyWeightLogDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _weightEntries = MutableLiveData<List<WeightEntry>>()
    val weightEntries: LiveData<List<WeightEntry>> = _weightEntries

    private val _rollingAverage = MutableLiveData<List<WeightEntry>>()
    val rollingAverage: LiveData<List<WeightEntry>> = _rollingAverage

    private val _bodyStats = MutableLiveData<BodyStats>()
    val bodyStats: LiveData<BodyStats> = _bodyStats

    private val _logSuccess = MutableLiveData<Boolean>()
    val logSuccess: LiveData<Boolean> = _logSuccess

    fun load() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val logs = bodyWeightLogDao.getWeightLogsForUser(uid)
            val entries = logs.mapNotNull { log ->
                try {
                    WeightEntry(LocalDate.parse(log.loggedDate), log.weightKg)
                } catch (_: Exception) { null }
            }.sortedBy { it.date }

            _weightEntries.postValue(entries)
            _rollingAverage.postValue(computeRollingAverage(entries, 7))
            _bodyStats.postValue(computeStats(entries))
        }
    }

    fun logWeight(weightKg: Double) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now().toString()
            val existing = bodyWeightLogDao.getWeightLogForDate(uid, today)
            if (existing != null) {
                bodyWeightLogDao.updateWeightLog(
                    existing.copy(weightKg = weightKg, loggedAt = System.currentTimeMillis())
                )
            } else {
                bodyWeightLogDao.insertWeightLog(
                    BodyWeightLogEntity(
                        id = UUID.randomUUID().toString(),
                        userId = uid,
                        weightKg = weightKg,
                        loggedDate = today,
                        loggedAt = System.currentTimeMillis()
                    )
                )
            }
            _logSuccess.postValue(true)
            load()
        }
    }

    private fun computeRollingAverage(entries: List<WeightEntry>, windowDays: Int): List<WeightEntry> {
        return entries.mapIndexed { i, entry ->
            val start = maxOf(0, i - windowDays + 1)
            val window = entries.subList(start, i + 1)
            val avg = window.sumOf { it.weightKg } / window.size
            WeightEntry(entry.date, avg)
        }
    }

    private fun computeStats(entries: List<WeightEntry>): BodyStats {
        if (entries.isEmpty()) return BodyStats(null, null, null, null, null)
        val current = entries.last().weightKg
        val starting = entries.first().weightKg
        val lowest = entries.minOf { it.weightKg }
        val thirtyDaysAgo = LocalDate.now().minusDays(30)
        val monthlyBase = entries.filter { it.date <= thirtyDaysAgo }.lastOrNull()?.weightKg
        return BodyStats(
            startingWeight = starting,
            currentWeight = current,
            monthlyChange = monthlyBase?.let { current - it },
            allTimeChange = current - starting,
            lowestWeight = lowest
        )
    }
}
