package com.reprush.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class AuthState {
    object Loading : AuthState()
    object Admin : AuthState()
    object MemberActive : AuthState()
    object MemberPending : AuthState()
    object MemberBlocked : AuthState()
    object MemberRejected : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    fun handleGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                val uid = auth.currentUser?.uid ?: throw Exception("No user found")
                checkUserRole(uid)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign-in failed")
            }
        }
    }

    fun checkCurrentUser() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            // Not signed in, stay on sign-in screen
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            checkUserRole(uid)
        }
    }

    private suspend fun checkUserRole(uid: String) {
        val doc = firestore.collection("users").document(uid).get().await()

        if (!doc.exists()) {
            // New user — create pending member document
            val newUser = hashMapOf(
                "displayName" to (FirebaseAuth.getInstance().currentUser?.displayName ?: ""),
                "email" to (FirebaseAuth.getInstance().currentUser?.email ?: ""),
                "photoUrl" to (FirebaseAuth.getInstance().currentUser?.photoUrl?.toString() ?: ""),
                "role" to "member",
                "membershipStatus" to "pending",
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).set(newUser).await()
            _authState.value = AuthState.MemberPending
            return
        }

        val role = doc.getString("role")
        val status = doc.getString("membershipStatus")

        when {
            role == "admin" -> _authState.value = AuthState.Admin
            status == "pending" -> _authState.value = AuthState.MemberPending
            status == "rejected" -> _authState.value = AuthState.MemberRejected
            status == "suspended" || status == "expired" -> _authState.value = AuthState.MemberBlocked
            else -> _authState.value = AuthState.MemberActive
        }
    }
}
