package com.reprush.app.ui.member.progress

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ProgressPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> ProgressOverviewFragment()
        1 -> BodyFragment()
        2 -> StrengthFragment()
        else -> ProgressOverviewFragment()
    }
}
