package com.reprush.app.ui.member.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.local.dao.DailyVolumeWithCount
import com.reprush.app.data.local.dao.LoggedSetDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HeatmapViewModel @Inject constructor(
    private val loggedSetDao: LoggedSetDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _heatmapIntensity = MutableLiveData<Map<LocalDate, Float>>()
    val heatmapIntensity: LiveData<Map<LocalDate, Float>> = _heatmapIntensity

    private val _heatmapDetail = MutableLiveData<Map<LocalDate, Pair<Float, Int>>>()
    val heatmapDetail: LiveData<Map<LocalDate, Pair<Float, Int>>> = _heatmapDetail

    fun load() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val sixMonthsAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(182)
            val raw: List<DailyVolumeWithCount> =
                loggedSetDao.getVolumeWithCountPerDayForHeatmap(uid, sixMonthsAgo)

            val maxVolume = raw.maxOfOrNull { it.totalVolume }?.takeIf { it > 0.0 } ?: 1.0

            val intensityMap = mutableMapOf<LocalDate, Float>()
            val detailMap = mutableMapOf<LocalDate, Pair<Float, Int>>()

            for (entry in raw) {
                try {
                    val date = LocalDate.parse(entry.workoutDate)
                    val intensity = (entry.totalVolume / maxVolume).toFloat().coerceIn(0f, 1f)
                    intensityMap[date] = intensity
                    detailMap[date] = Pair(intensity, entry.exerciseCount)
                } catch (_: Exception) { /* skip malformed dates */ }
            }

            _heatmapIntensity.postValue(intensityMap)
            _heatmapDetail.postValue(detailMap)
        }
    }
}
