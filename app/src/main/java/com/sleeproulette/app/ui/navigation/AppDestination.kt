package com.sleeproulette.app.ui.navigation

import androidx.annotation.StringRes
import com.sleeproulette.app.R
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation destinations (Navigation 2.8+).
 * Prefer @Serializable routes over stringly-typed paths.
 */
@Serializable
sealed interface AppDestination {
    @get:StringRes
    val labelRes: Int

    @Serializable
    data object Today : AppDestination {
        override val labelRes: Int = R.string.nav_today
    }

    @Serializable
    data object Log : AppDestination {
        override val labelRes: Int = R.string.nav_log
    }

    @Serializable
    data object Stats : AppDestination {
        override val labelRes: Int = R.string.nav_stats
    }

    @Serializable
    data object Setup : AppDestination {
        override val labelRes: Int = R.string.nav_setup
    }

    companion object {
        val bottomBarItems: List<AppDestination> = listOf(Today, Log, Stats, Setup)
    }
}
