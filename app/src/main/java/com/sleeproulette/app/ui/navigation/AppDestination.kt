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
    data object Tonight : AppDestination {
        override val labelRes: Int = R.string.nav_tonight
    }

    @Serializable
    data object Log : AppDestination {
        override val labelRes: Int = R.string.nav_log
    }

    @Serializable
    data object Trends : AppDestination {
        override val labelRes: Int = R.string.nav_trends
    }

    @Serializable
    data object Settings : AppDestination {
        override val labelRes: Int = R.string.nav_settings
    }

    companion object {
        val bottomBarItems: List<AppDestination> = listOf(Tonight, Log, Trends, Settings)
    }
}
