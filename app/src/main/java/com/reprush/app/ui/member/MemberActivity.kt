package com.reprush.app.ui.member

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.reprush.app.R
import com.reprush.app.databinding.ActivityMemberBinding
import com.reprush.app.ui.member.notifications.NotificationsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MemberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemberBinding
    private val notifViewModel: NotificationsViewModel by viewModels()

    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var auth: FirebaseAuth

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: badge and FCM work regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.navHostMember.updatePadding(top = bars.top)
            binding.bottomNavMember.updatePadding(bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val pending = intent.getBooleanExtra("pending", false)
        val blocked = intent.getBooleanExtra("blocked", false)
        val rejected = intent.getBooleanExtra("rejected", false)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_member) as NavHostFragment
        val navController = navHost.navController

        when {
            pending -> {
                binding.bottomNavMember.visibility = View.GONE
                navController.navigate(R.id.pendingFragment)
            }
            blocked || rejected -> {
                binding.bottomNavMember.visibility = View.GONE
                val bundle = Bundle().apply { putBoolean("rejected", rejected) }
                navController.navigate(R.id.statusBlockedFragment, bundle)
            }
            else -> {
                binding.bottomNavMember.setupWithNavController(navController)
                setupNotificationBadge()
                requestNotificationPermission()
                setupFcm()
            }
        }
    }

    private fun setupNotificationBadge() {
        val badge = binding.bottomNavMember.getOrCreateBadge(R.id.notificationsFragment)
        badge.isVisible = false

        notifViewModel.unreadCount.observe(this) { count ->
            if (count > 0) {
                badge.number = count
                badge.isVisible = true
            } else {
                badge.isVisible = false
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupFcm() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().subscribeToTopic("gym_announcements")
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            firestore.collection("users").document(uid)
                .update("fcmToken", token)
        }
    }
}
