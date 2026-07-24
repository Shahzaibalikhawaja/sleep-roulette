package com.sleeproulette.app.ui.setup

import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleeproulette.app.domain.model.BedtimeGoalMode
import com.sleeproulette.app.domain.time.AppTimeFormat
import com.sleeproulette.app.ui.components.ScreenHeader
import com.sleeproulette.app.ui.components.SoftCard
import kotlin.math.roundToInt

@Composable
fun SetupRoute(viewModel: SetupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
        onClearFeedback = viewModel::clearFeedback,
        onTogglePermissions = viewModel::togglePermissionsExpanded,
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
        onPickGoalTime = {
            val minutes = state.settings.fixedGoalMinutesFromMidnight
            TimePickerDialog(
                context,
                { _, hour, minute -> viewModel.setFixedGoalMinutes(hour * 60 + minute) },
                minutes / 60,
                minutes % 60,
                state.settings.use24HourClock,
            ).show()
        },
        onBeforeSunrise = viewModel::setMinutesBeforeSunrise,
        onUse24HourClock = viewModel::setUse24HourClock,
    )
}

@Composable
fun SetupScreen(
    state: SetupUiState,
    onClearFeedback: () -> Unit,
    onTogglePermissions: () -> Unit,
    onRequestFineLocation: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenBatteryOpt: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onUseCurrentAsHome: () -> Unit,
    onClearHome: () -> Unit,
    onGoalMode: (BedtimeGoalMode) -> Unit,
    onPickGoalTime: () -> Unit,
    onBeforeSunrise: (Int) -> Unit,
    onUse24HourClock: (Boolean) -> Unit,
) {
    val use24 = state.settings.use24HourClock

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title = "Settings")

        state.feedback?.let { message ->
            SoftCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = message,
                        color = if (state.feedbackIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearFeedback) { Text("OK") }
                }
            }
        }

        SoftCard {
            Text("Schedule", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.settings.goalMode == BedtimeGoalMode.FIXED_CLOCK,
                    onClick = { onGoalMode(BedtimeGoalMode.FIXED_CLOCK) },
                    label = { Text("Fixed time") },
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
                    OutlinedButton(
                        onClick = onPickGoalTime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Text(
                            text = AppTimeFormat.formatMinutesFromMidnight(minutes, use24),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
                BedtimeGoalMode.MINUTES_BEFORE_SUNRISE -> {
                    val m = state.settings.minutesBeforeSunrise
                    Text(
                        text = "$m min before sunrise",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Slider(
                        value = m.toFloat(),
                        onValueChange = {
                            onBeforeSunrise((it / 15f).roundToInt() * 15)
                        },
                        valueRange = 15f..(6 * 60).toFloat(),
                        steps = 22,
                    )
                }
            }
        }

        SoftCard {
            Text("Display", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Clock format",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = use24,
                    onClick = { onUse24HourClock(true) },
                    label = { Text("24-hour") },
                )
                FilterChip(
                    selected = !use24,
                    onClick = { onUse24HourClock(false) },
                    label = { Text("12-hour") },
                )
            }
        }

        SoftCard {
            Text("Places", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (state.home == null) "Home not set" else "Home set",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            Button(
                onClick = onUseCurrentAsHome,
                enabled = state.hasFineLocation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.home == null) "Set Home" else "Update Home")
            }
            if (state.home != null) {
                OutlinedButton(
                    onClick = onClearHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text("Clear Home")
                }
            }
        }

        SoftCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTogglePermissions),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Permissions", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (state.permissionsReady) {
                            "Ready"
                        } else {
                            "${state.neededPermissionCount} needed"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.permissionsReady) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
                Text(
                    text = if (state.permissionsExpanded) "Hide" else "Show",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = state.permissionsExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PermissionRow("Location", state.hasFineLocation, onRequestFineLocation)
                    PermissionRow("Background location", state.hasBackgroundLocation, onRequestBackgroundLocation)
                    PermissionRow("Usage access", state.hasUsageAccess, onOpenUsageAccess)
                    PermissionRow("Notifications", state.hasNotificationPermission, onRequestNotifications)
                    PermissionRow(
                        "Unrestricted battery",
                        state.ignoringBatteryOptimizations,
                        onOpenBatteryOpt,
                    )
                }
            }
        }

        SoftCard {
            Text("System", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "ColorOS may still need Autostart or battery exceptions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            OutlinedButton(onClick = onOpenAppDetails, modifier = Modifier.fillMaxWidth()) {
                Text("App settings")
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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
            OutlinedButton(onClick = onAction) { Text("Allow") }
        }
    }
}
