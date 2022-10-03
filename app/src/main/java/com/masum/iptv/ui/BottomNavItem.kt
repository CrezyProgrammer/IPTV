package com.masum.iptv.ui

import com.masum.iptv.R

sealed class BottomNavItem(var title: String,var icon:Int , var screen_route:String) {
    object Home:BottomNavItem("Home", R.drawable.ic_home_black_24dp,"home")
    object Stream:BottomNavItem("Stream", R.drawable.ic_baseline_ondemand_video_24,"stream")
}