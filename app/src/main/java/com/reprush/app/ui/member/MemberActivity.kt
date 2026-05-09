package com.reprush.app.ui.member

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.reprush.app.R
import com.reprush.app.databinding.ActivityMemberBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MemberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemberBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pending = intent.getBooleanExtra("pending", false)
        val blocked = intent.getBooleanExtra("blocked", false)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_member) as NavHostFragment
        val navController = navHost.navController

        when {
            pending -> navController.navigate(R.id.pendingFragment)
            blocked -> navController.navigate(R.id.statusBlockedFragment)
            else -> binding.bottomNavMember.setupWithNavController(navController)
        }
    }
}
