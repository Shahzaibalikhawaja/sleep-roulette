package com.sleeproulette.app.ui.navigation

import androidx.annotation.StringRes
import com.sleeproulette.app.R

sealed class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
) {
    data object Today : AppDestination("today", R.string.nav_today)
    data object Log : AppDestination("log", R.string.nav_log)
    data object Stats : AppDestination("stats", R.string.nav_stats)
    data object Setup : AppDestination("setup", R.string.nav_setup)
}
