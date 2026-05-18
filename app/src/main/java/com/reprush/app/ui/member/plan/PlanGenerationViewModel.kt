package com.reprush.app.ui.member.plan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reprush.app.data.local.datastore.AppPreferences
import com.reprush.app.data.repository.ExerciseRepository
import com.reprush.app.data.repository.GeminiRepository
import com.reprush.app.data.repository.ImportedPlan
import com.reprush.app.data.repository.PlanImportRepository
import com.reprush.app.data.repository.PlanImportValidator
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GenerationState { IDLE, GENERATING, DONE, ERROR }

@HiltViewModel
class PlanGenerationViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val exerciseRepository: ExerciseRepository,
    private val planImportRepository: PlanImportRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    var goal: String = ""
    var daysPerWeek: Int = 3
    var splitType: String = ""
    var sessionDuration: Int = 60
    var equipment: String = "Full Gym"
    var fitnessLevel: String = ""
    var weeks: Int = 8
    var injuries: String = ""

    private val _generationState = MutableLiveData(GenerationState.IDLE)
    val generationState: LiveData<GenerationState> = _generationState

    private val _importedPlan = MutableLiveData<ImportedPlan?>()
    val importedPlan: LiveData<ImportedPlan?> = _importedPlan

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun generatePlan() {
        _generationState.value = GenerationState.GENERATING
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val exercises = exerciseRepository.getExercisesForEquipment(equipment)
            val exerciseNames = exercises.map { it.name }
            val profile = appPreferences.getMemberProfile()

            when (val geminiResult = geminiRepository.generatePlan(
                goal, daysPerWeek, splitType, sessionDuration,
                equipment, fitnessLevel, weeks, injuries, exerciseNames, profile
            )) {
                is Result.Error -> {
                    _errorMessage.postValue(geminiResult.message)
                    _generationState.postValue(GenerationState.ERROR)
                }
                is Result.Success -> {
                    val validator = PlanImportValidator()
                    when (val validationResult = validator.validate(geminiResult.data)) {
                        is Result.Error -> {
                            _errorMessage.postValue(validationResult.message)
                            _generationState.postValue(GenerationState.ERROR)
                        }
                        is Result.Success -> {
                            when (val importResult = planImportRepository.importPlan(validationResult.data)) {
                                is Result.Error -> {
                                    _errorMessage.postValue(importResult.message)
                                    _generationState.postValue(GenerationState.ERROR)
                                }
                                is Result.Success -> {
                                    _importedPlan.postValue(importResult.data)
                                    _generationState.postValue(GenerationState.DONE)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun resetState() {
        _generationState.value = GenerationState.IDLE
        _errorMessage.value = null
    }
}
