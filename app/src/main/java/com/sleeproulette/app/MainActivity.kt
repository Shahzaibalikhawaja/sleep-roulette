package com.sleeproulette.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.sleeproulette.app.ui.log.LogRoute
import com.sleeproulette.app.ui.navigation.AppDestination
import com.sleeproulette.app.ui.setup.SetupRoute
import com.sleeproulette.app.ui.stats.StatsRoute
import com.sleeproulette.app.ui.theme.SleepRouletteColors
import com.sleeproulette.app.ui.theme.SleepRouletteTheme
import com.sleeproulette.app.ui.today.TodayRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SleepRouletteTheme {
                SleepRouletteAppShell()
            }
        }
    }
}

@Composable
private fun SleepRouletteAppShell() {
    val navController = rememberNavController()
    val destinations = AppDestination.bottomBarItems
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = scheme.surface,
                contentColor = scheme.onSurface,
            ) {
                destinations.forEach { dest ->
                    val selected = when (dest) {
                        AppDestination.Tonight ->
                            currentDestination?.hasRoute<AppDestination.Tonight>() == true
                        AppDestination.Log ->
                            currentDestination?.hasRoute<AppDestination.Log>() == true
                        AppDestination.Trends ->
                            currentDestination?.hasRoute<AppDestination.Trends>() == true
                        AppDestination.Settings ->
                            currentDestination?.hasRoute<AppDestination.Settings>() == true
                    }
                    val accent = when (dest) {
                        AppDestination.Tonight -> SleepRouletteColors.LavenderDeep
                        AppDestination.Log -> SleepRouletteColors.PowderBlueDeep
                        AppDestination.Trends -> SleepRouletteColors.Mint
                        AppDestination.Settings -> SleepRouletteColors.Peach
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(
                                route = dest,
                                navOptions = navOptions {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                },
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accent,
                            selectedTextColor = accent,
                            indicatorColor = accent.copy(alpha = 0.18f),
                            unselectedIconColor = scheme.onSurfaceVariant,
                            unselectedTextColor = scheme.onSurfaceVariant,
                        ),
                        icon = {
                            Icon(
                                imageVector = when (dest) {
                                    AppDestination.Tonight -> Icons.Outlined.Nightlight
                                    AppDestination.Log -> Icons.Outlined.Bedtime
                                    AppDestination.Trends -> Icons.Outlined.AutoGraph
                                    AppDestination.Settings -> Icons.Outlined.Tune
                                },
                                contentDescription = stringResource(dest.labelRes),
                            )
                        },
                        label = { Text(stringResource(dest.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Tonight,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<AppDestination.Tonight> { TodayRoute() }
            composable<AppDestination.Log> { LogRoute() }
            composable<AppDestination.Trends> { StatsRoute() }
            composable<AppDestination.Settings> { SetupRoute() }
        }
    }
}
