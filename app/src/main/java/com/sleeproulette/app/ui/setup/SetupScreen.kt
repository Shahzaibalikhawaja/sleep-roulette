package com.sleeproulette.app.ui.setup

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleeproulette.app.domain.model.BedtimeGoalMode
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SetupRoute(viewModel: SetupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Modern replacement for DisposableEffect + LifecycleEventObserver
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissionFlags()
    }

    val fineLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshPermissionFlags()
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.refreshPermissionFlags()
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* refresh on resume */ }

    SetupScreen(
        state = state,
        onRequestFineLocation = {
            fineLauncher.launch(viewModel.locationPermissionNeeded())
        },
        onRequestBackgroundLocation = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundLauncher.launch(viewModel.backgroundLocationPermission())
            }
        },
        onRequestNotifications = {
            viewModel.notificationPermission()?.let { notifLauncher.launch(it) }
        },
        onOpenUsageAccess = {
            context.startActivity(viewModel.usageAccessIntent())
        },
        onOpenBatteryOpt = {
            runCatching { context.startActivity(viewModel.batteryOptimizationIntent()) }
            viewModel.markBatteryAck()
        },
        onOpenAppDetails = {
            context.startActivity(viewModel.appDetailsIntent())
        },
        onUseCurrentAsHome = viewModel::useCurrentLocationAsHome,
        onClearHome = viewModel::clearHome,
        onGoalMode = viewModel::setGoalMode,
        onFixedMinutes = viewModel::setFixedGoalMinutes,
        onBeforeSunrise = viewModel::setMinutesBeforeSunrise,
    )
}

@Composable
fun SetupScreen(
    state: SetupUiState,
    onRequestFineLocation: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenBatteryOpt: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onUseCurrentAsHome: () -> Unit,
    onClearHome: () -> Unit,
    onGoalMode: (BedtimeGoalMode) -> Unit,
    onFixedMinutes: (Int) -> Unit,
    onBeforeSunrise: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Setup", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Personal MVP — grant what ColorOS needs so geofences and nudges survive.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("Permissions")
        PermissionRow("Fine location", state.hasFineLocation, onRequestFineLocation)
        PermissionRow("Background location", state.hasBackgroundLocation, onRequestBackgroundLocation)
        PermissionRow("Usage access", state.hasUsageAccess, onOpenUsageAccess)
        PermissionRow(
            "Ignore battery optimizations",
            state.ignoringBatteryOptimizations,
            onOpenBatteryOpt,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OutlinedButton(onClick = onRequestNotifications, modifier = Modifier.fillMaxWidth()) {
                Text("Request notification permission")
            }
        }
        OutlinedButton(onClick = onOpenAppDetails, modifier = Modifier.fillMaxWidth()) {
            Text("Open app settings (Autostart on ColorOS)")
        }

        SectionTitle("Home geofence")
        Text(
            text = state.home?.let {
                "Home @ %.5f, %.5f (±%.0fm)".format(Locale.US, it.latitude, it.longitude, it.radiusMeters)
            } ?: "Not set",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onUseCurrentAsHome,
            enabled = state.hasFineLocation,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (state.home == null) {
                    "Use current location as Home"
                } else {
                    "Overwrite Home with current location"
                },
            )
        }
        if (state.home != null) {
            OutlinedButton(
                onClick = onClearHome,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear Home")
            }
        }

        SectionTitle("Bedtime goal")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.settings.goalMode == BedtimeGoalMode.FIXED_CLOCK,
                onClick = { onGoalMode(BedtimeGoalMode.FIXED_CLOCK) },
                label = { Text("Fixed clock") },
            )
            FilterChip(
                selected = state.settings.goalMode == BedtimeGoalMode.MINUTES_BEFORE_SUNRISE,
                onClick = { onGoalMode(BedtimeGoalMode.MINUTES_BEFORE_SUNRISE) },
                label = { Text("Before sunrise") },
            )
        }

        when (state.settings.goalMode) {
            BedtimeGoalMode.FIXED_CLOCK -> {
                val minutes = state.settings.fixedGoalMinutesFromMidnight
                Text("Goal time: %02d:%02d".format(minutes / 60, minutes % 60))
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { onFixedMinutes(it.roundToInt()) },
                    valueRange = 0f..(24 * 60 - 1).toFloat(),
                )
            }
            BedtimeGoalMode.MINUTES_BEFORE_SUNRISE -> {
                val m = state.settings.minutesBeforeSunrise
                Text("$m minutes before sunrise")
                Slider(
                    value = m.toFloat(),
                    onValueChange = { onBeforeSunrise(it.roundToInt()) },
                    valueRange = 15f..(6 * 60).toFloat(),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (granted) "Granted" else "Needed",
                color = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (!granted) {
            OutlinedButton(onClick = onAction) { Text("Fix") }
        }
    }
}
