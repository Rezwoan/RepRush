package com.reprush.app.ui.member.plan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.local.entity.WorkoutPlanEntity
import com.reprush.app.data.repository.PlanImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanListViewModel @Inject constructor(
    private val planImportRepository: PlanImportRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _plans = MutableLiveData<List<WorkoutPlanEntity>>(emptyList())
    val plans: LiveData<List<WorkoutPlanEntity>> = _plans

    fun loadPlans() {
        viewModelScope.launch(Dispatchers.IO) {
            _plans.postValue(planImportRepository.getPlansForUser())
        }
    }

    fun setActive(plan: WorkoutPlanEntity) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            planImportRepository.setActivePlan(plan.id, userId)
            _plans.postValue(planImportRepository.getPlansForUser())
        }
    }

    fun deletePlan(plan: WorkoutPlanEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            planImportRepository.deletePlan(plan)
            _plans.postValue(planImportRepository.getPlansForUser())
        }
    }
}
