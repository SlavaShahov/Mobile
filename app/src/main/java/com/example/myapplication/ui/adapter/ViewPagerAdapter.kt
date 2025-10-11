package com.example.myapplication.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapplication.ui.fragment.RecordsFragment
import com.example.myapplication.ui.fragment.RulesFragment
import com.example.myapplication.ui.fragment.AuthorsFragment
import com.example.myapplication.ui.fragment.SettingsFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RecordsFragment()
            1 -> RulesFragment()
            2 -> AuthorsFragment()
            3 -> SettingsFragment()
            else -> RecordsFragment()
        }
    }
}