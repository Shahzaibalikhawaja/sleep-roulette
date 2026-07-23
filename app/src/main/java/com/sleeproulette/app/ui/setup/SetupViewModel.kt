package com.sleeproulette.app.ui.setup

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import android.content.Context
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val ignoringBatteryOptimizations: Boolean = false,
    val statusMessage: String? = null,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val placeRepository: PlaceRepository,
    private val geofenceManager: GeofenceManager,
    private val usageStatsSampler: UsageStatsSampler,
) : ViewModel() {

    val uiState: StateFlow<SetupUiState> = combine(
        settingsRepository.settings,
        placeRepository.observePlaces(),
    ) { settings, places ->
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        SetupUiState(
            settings = settings,
            home = places.firstOrNull { it.kind == PlaceKind.HOME },
            work = places.firstOrNull { it.kind == PlaceKind.WORK },
            hasFineLocation = geofenceManager.hasFineLocation(),
            hasBackgroundLocation = geofenceManager.hasBackgroundLocation(),
            hasUsageAccess = usageStatsSampler.hasUsageAccess(),
            ignoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(context.packageName),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetupUiState())

    fun refreshPermissionFlags() {
        // Re-collect by touching settings (forces UI consumers to recompose via new emit
        // when user returns from Settings). Simplest: re-upsert settings unchanged.
        viewModelScope.launch {
            settingsRepository.update { it }
        }
    }

    @SuppressLint("MissingPermission")
    fun useCurrentLocationAsHome() {
        viewModelScope.launch {
            if (!geofenceManager.hasFineLocation()) return@launch
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val location = runCatching {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            }.getOrNull() ?: client.lastLocation.await()

            if (location == null) return@launch

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
        }
    }

    fun clearHome() {
        viewModelScope.launch {
            placeRepository.delete(PlaceKind.HOME)
            geofenceManager.refreshGeofences()
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
