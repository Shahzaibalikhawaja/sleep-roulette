package com.sleeproulette.app.ui.setup

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.sleeproulette.app.domain.model.BedtimeGoalMode
import com.sleeproulette.app.domain.model.Place
import com.sleeproulette.app.domain.model.PlaceKind
import com.sleeproulette.app.domain.model.UserSettings
import com.sleeproulette.app.domain.repo.PlaceRepository
import com.sleeproulette.app.domain.repo.SettingsRepository
import com.sleeproulette.app.location.GeofenceManager
import com.sleeproulette.app.usage.UsageStatsSampler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class SetupUiState(
    val settings: UserSettings = UserSettings(),
    val home: Place? = null,
    val work: Place? = null,
    val hasFineLocation: Boolean = false,
    val hasBackgroundLocation: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val ignoringBatteryOptimizations: Boolean = false,
    val permissionsExpanded: Boolean = false,
    val feedback: String? = null,
    val feedbackIsError: Boolean = false,
) {
    val neededPermissionCount: Int
        get() = listOf(
            hasFineLocation,
            hasBackgroundLocation,
            hasUsageAccess,
            hasNotificationPermission,
            ignoringBatteryOptimizations,
        ).count { !it }

    val permissionsReady: Boolean get() = neededPermissionCount == 0
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val placeRepository: PlaceRepository,
    private val geofenceManager: GeofenceManager,
    private val usageStatsSampler: UsageStatsSampler,
) : ViewModel() {

    private val feedback = MutableStateFlow<Pair<String, Boolean>?>(null)
    private val permissionsExpanded = MutableStateFlow(false)

    val uiState: StateFlow<SetupUiState> = combine(
        settingsRepository.settings,
        placeRepository.observePlaces(),
        feedback,
        permissionsExpanded,
    ) { settings, places, fb, expanded ->
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val base = SetupUiState(
            settings = settings,
            home = places.firstOrNull { it.kind == PlaceKind.HOME },
            work = places.firstOrNull { it.kind == PlaceKind.WORK },
            hasFineLocation = geofenceManager.hasFineLocation(),
            hasBackgroundLocation = geofenceManager.hasBackgroundLocation(),
            hasUsageAccess = usageStatsSampler.hasUsageAccess(),
            hasNotificationPermission =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED,
            ignoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(context.packageName),
            permissionsExpanded = expanded,
            feedback = fb?.first,
            feedbackIsError = fb?.second == true,
        )
        base.copy(
            permissionsExpanded = expanded || !base.permissionsReady,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetupUiState())

    fun refreshPermissionFlags() {
        viewModelScope.launch {
            settingsRepository.update { it }
        }
    }

    fun togglePermissionsExpanded() {
        permissionsExpanded.update { !it }
    }

    fun clearFeedback() {
        feedback.value = null
    }

    @SuppressLint("MissingPermission")
    fun useCurrentLocationAsHome() {
        viewModelScope.launch {
            if (!geofenceManager.hasFineLocation()) {
                feedback.value = "Location permission needed" to true
                return@launch
            }
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val location = runCatching {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            }.getOrNull() ?: client.lastLocation.await()

            if (location == null) {
                feedback.value = "Couldn't read location" to true
                return@launch
            }

            runCatching {
                placeRepository.upsert(
                    Place(
                        kind = PlaceKind.HOME,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        radiusMeters = Place.DEFAULT_RADIUS_METERS,
                        label = "Home",
                    ),
                )
                geofenceManager.refreshGeofences()
                settingsRepository.update { it.copy(onboardingComplete = true) }
            }.onSuccess {
                feedback.value = "Home updated" to false
            }.onFailure {
                feedback.value = "Couldn't update Home" to true
            }
        }
    }

    fun clearHome() {
        viewModelScope.launch {
            runCatching {
                placeRepository.delete(PlaceKind.HOME)
                geofenceManager.refreshGeofences()
            }.onSuccess {
                feedback.value = "Home cleared" to false
            }.onFailure {
                feedback.value = "Couldn't clear Home" to true
            }
        }
    }

    fun setGoalMode(mode: BedtimeGoalMode) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(goalMode = mode) }
        }
    }

    fun setFixedGoalMinutes(minutesFromMidnight: Int) {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(fixedGoalMinutesFromMidnight = minutesFromMidnight.coerceIn(0, 24 * 60 - 1))
            }
        }
    }

    fun setMinutesBeforeSunrise(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(minutesBeforeSunrise = minutes.coerceIn(15, 6 * 60))
            }
        }
    }

    fun setUse24HourClock(use24HourClock: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(use24HourClock = use24HourClock) }
        }
    }

    fun usageAccessIntent(): Intent = usageStatsSampler.usageAccessSettingsIntent()

    fun batteryOptimizationIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun appDetailsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun locationPermissionNeeded(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun backgroundLocationPermission(): String = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    fun notificationPermission(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    fun markBatteryAck() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(batteryOptimizationAcknowledged = true) }
        }
    }
}
