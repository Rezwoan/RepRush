package com.reprush.app.ui.member.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.reprush.app.data.local.dao.UserDao
import com.reprush.app.data.local.entity.UserEntity
import com.reprush.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _user = MutableLiveData<UserEntity?>()
    val user: LiveData<UserEntity?> = _user

    private val _saveResult = MutableLiveData<Result<Unit>?>()
    val saveResult: LiveData<Result<Unit>?> = _saveResult

    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _user.postValue(userDao.getUserById(uid))
        }
    }

    fun saveProfile(
        fitnessLevel: String,
        primaryGoal: String,
        availableEquipment: String,
        injuries: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = userDao.getUserById(uid) ?: run {
                    _saveResult.postValue(Result.Error("User not found"))
                    return@launch
                }
                val updated = existing.copy(
                    fitnessLevel = fitnessLevel.ifBlank { null },
                    primaryGoal = primaryGoal.ifBlank { null },
                    availableEquipment = availableEquipment.ifBlank { null },
                    injuries = injuries.ifBlank { null }
                )
                userDao.updateUser(updated)

                withContext(Dispatchers.IO) {
                    try {
                        firestore.collection("users").document(uid).update(
                            mapOf(
                                "fitnessLevel" to fitnessLevel.ifBlank { null },
                                "primaryGoal" to primaryGoal.ifBlank { null },
                                "availableEquipment" to availableEquipment.ifBlank { null },
                                "injuries" to injuries.ifBlank { null }
                            )
                        ).await()
                    } catch (e: Exception) {
                        Log.e("ProfileEditVM", "Firestore update failed", e)
                    }
                }

                _saveResult.postValue(Result.Success(Unit))
            } catch (e: Exception) {
                _saveResult.postValue(Result.Error(e.message ?: "Save failed"))
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }
}
