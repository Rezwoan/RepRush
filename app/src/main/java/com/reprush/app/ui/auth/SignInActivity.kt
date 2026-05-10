package com.reprush.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.R
import com.reprush.app.databinding.ActivitySignInBinding
import com.reprush.app.ui.admin.AdminActivity
import com.reprush.app.ui.member.MemberActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var credentialManager: CredentialManager
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sidePadding = resources.getDimensionPixelSize(R.dimen.spacing_md)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                top = bars.top + sidePadding,
                bottom = bars.bottom + sidePadding
            )
            WindowInsetsCompat.CONSUMED
        }

        credentialManager = CredentialManager.create(this)

        observeAuthState()
        viewModel.checkCurrentUser()

        binding.buttonGoogleSignIn.setOnClickListener {
            launchGoogleSignIn()
        }
    }

    private fun launchGoogleSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@SignInActivity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.handleGoogleSignIn(googleIdTokenCredential.idToken)
                } else {
                    showError("Unexpected credential type")
                }
            } catch (_: GetCredentialCancellationException) {
                showError("Sign-in cancelled")
            } catch (e: GetCredentialException) {
                showError("Sign-in failed: ${e.message}")
            }
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> showLoading(true)
                is AuthState.Admin -> navigateTo(AdminActivity::class.java)
                is AuthState.MemberActive -> navigateTo(MemberActivity::class.java)
                is AuthState.MemberPending -> navigateTo(MemberActivity::class.java, pending = true)
                is AuthState.MemberBlocked -> navigateTo(MemberActivity::class.java, blocked = true)
                is AuthState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>, pending: Boolean = false, blocked: Boolean = false) {
        val intent = Intent(this, activityClass).apply {
            putExtra("pending", pending)
            putExtra("blocked", blocked)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBarLoading.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonGoogleSignIn.isEnabled = !show
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
