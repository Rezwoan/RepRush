package com.reprush.app.ui.member

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
                val bundle = Bundle().apply {
                    putBoolean("rejected", rejected)
                }
                navController.navigate(R.id.statusBlockedFragment, bundle)
            }
            else -> binding.bottomNavMember.setupWithNavController(navController)
        }
    }
}
