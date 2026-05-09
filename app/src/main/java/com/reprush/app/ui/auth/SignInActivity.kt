package com.reprush.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import com.reprush.app.R
import com.reprush.app.databinding.ActivitySignInBinding
import com.reprush.app.ui.admin.AdminActivity
import com.reprush.app.ui.member.MemberActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val viewModel: AuthViewModel by viewModels()

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(Exception::class.java)
            account.idToken?.let { viewModel.handleGoogleSignIn(it) }
        } catch (e: Exception) {
            showError("Sign-in cancelled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        observeAuthState()

        // Check if already signed in
        viewModel.checkCurrentUser()

        binding.buttonGoogleSignIn.setOnClickListener {
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
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
