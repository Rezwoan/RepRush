package com.reprush.app.ui.member.exercise

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.local.entity.ExerciseEntity
import com.reprush.app.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _exercises = MutableLiveData<List<ExerciseEntity>>(emptyList())
    val exercises: LiveData<List<ExerciseEntity>> = _exercises

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentQuery = ""
    private var currentMuscle = ""
    private var currentEquipment = ""

    fun loadExercises(query: String = currentQuery, muscle: String = currentMuscle, equipment: String = currentEquipment) {
        currentQuery = query
        currentMuscle = muscle
        currentEquipment = equipment
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = exerciseRepository.getExercisesFiltered(query, muscle, equipment)
            _exercises.postValue(result)
            _isLoading.postValue(false)
        }
    }
}
