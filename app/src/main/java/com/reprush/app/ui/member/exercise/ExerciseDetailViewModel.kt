package com.reprush.app.ui.member.exercise

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.local.entity.ExerciseEntity
import com.reprush.app.data.repository.ExerciseRepository
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _exercise = MutableLiveData<ExerciseEntity?>()
    val exercise: LiveData<ExerciseEntity?> = _exercise

    fun loadExercise(exerciseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _exercise.postValue(exerciseRepository.getExerciseById(exerciseId))
        }
    }

    private val _saveResult = MutableLiveData<Result<Unit>?>()
    val saveResult: LiveData<Result<Unit>?> = _saveResult

    fun saveCustomExercise(name: String, muscle: String, equipment: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = exerciseRepository.insertCustomExercise(name, muscle, equipment, category)
            _saveResult.postValue(result)
        }
    }
}
