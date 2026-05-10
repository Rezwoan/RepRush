package com.reprush.app.ui.admin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.reprush.app.R

class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.button_managePackages).setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_packageListFragment)
        }

        view.findViewById<View>(R.id.button_registerMember).setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_manualRegisterFragment)
        }
    }
}