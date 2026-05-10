package com.reprush.app.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.reprush.app.R
import com.reprush.app.databinding.ActivityAdminBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.navHostAdmin.updatePadding(top = bars.top)
            binding.bottomNavAdmin.updatePadding(bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_admin) as NavHostFragment
        binding.bottomNavAdmin.setupWithNavController(navHost.navController)
    }
}
