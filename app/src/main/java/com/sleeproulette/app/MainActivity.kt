package com.sleeproulette.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sleeproulette.app.ui.log.LogRoute
import com.sleeproulette.app.ui.navigation.AppDestination
import com.sleeproulette.app.ui.setup.SetupRoute
import com.sleeproulette.app.ui.stats.StatsRoute
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
    val destinations = listOf(
        AppDestination.Today,
        AppDestination.Log,
        AppDestination.Stats,
        AppDestination.Setup,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                destinations.forEach { dest ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (dest) {
                                    AppDestination.Today -> Icons.Outlined.Nightlight
                                    AppDestination.Log -> Icons.Outlined.Bedtime
                                    AppDestination.Stats -> Icons.Outlined.BarChart
                                    AppDestination.Setup -> Icons.Outlined.Settings
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
            startDestination = AppDestination.Today.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Today.route) { TodayRoute() }
            composable(AppDestination.Log.route) { LogRoute() }
            composable(AppDestination.Stats.route) { StatsRoute() }
            composable(AppDestination.Setup.route) { SetupRoute() }
        }
    }
}
