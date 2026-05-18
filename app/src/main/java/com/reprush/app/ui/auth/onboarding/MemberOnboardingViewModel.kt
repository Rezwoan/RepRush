package com.reprush.app.ui.auth.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.data.local.datastore.AppPreferences
import com.reprush.app.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemberOnboardingViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val memberRepository: MemberRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _saved = MutableLiveData(false)
    val saved: LiveData<Boolean> = _saved

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun saveOnboarding(
        heightCm: Float,
        weightKg: Float,
        experience: String,
        lastExercised: String,
        squatKg: Float?,
        benchKg: Float?,
        deadliftKg: Float?
    ) {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true
        viewModelScope.launch {
            appPreferences.saveMemberOnboarding(
                heightCm, weightKg, experience, lastExercised, squatKg, benchKg, deadliftKg
            )
            memberRepository.saveOnboardingProfile(uid, heightCm, weightKg, experience, lastExercised)
            _isLoading.postValue(false)
            _saved.postValue(true)
        }
    }
}
