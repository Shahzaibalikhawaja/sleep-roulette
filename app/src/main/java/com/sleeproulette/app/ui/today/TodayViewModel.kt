package com.sleeproulette.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.model.PlaceKind
import com.sleeproulette.app.domain.model.SleepSession
import com.sleeproulette.app.domain.model.SleepSource
import com.sleeproulette.app.domain.model.UserSettings
import com.sleeproulette.app.domain.policy.BedtimePolicy
import com.sleeproulette.app.domain.repo.LifeEventRepository
import com.sleeproulette.app.domain.repo.PlaceRepository
import com.sleeproulette.app.domain.repo.SettingsRepository
import com.sleeproulette.app.domain.repo.SleepSessionRepository
import com.sleeproulette.app.notify.SleepCountdownController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class TodayUiState(
    val settings: UserSettings = UserSettings(),
    val hasHome: Boolean = false,
    val window: BedtimePolicy.SleepWindow? = null,
    val remaining: Duration = Duration.ZERO,
    val ongoingSleep: SleepSession? = null,
    val now: Instant = Instant.now(),
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    placeRepository: PlaceRepository,
    private val lifeEventRepository: LifeEventRepository,
    private val sleepSessionRepository: SleepSessionRepository,
    private val countdownController: SleepCountdownController,
) : ViewModel() {

    private val tick = MutableStateFlow(Instant.now())

    private val latestHomeArrival = lifeEventRepository
        .observeRecent(limit = 50)
        .map { events -> events.firstOrNull { it.type == LifeEventType.ENTER_HOME }?.occurredAt }

    val uiState: StateFlow<TodayUiState> = combine(
        settingsRepository.settings,
        placeRepository.observePlaces(),
        sleepSessionRepository.observeOngoing(),
        latestHomeArrival,
        tick,
    ) { settings, places, ongoing, arrived, now ->
        val home = places.firstOrNull { it.kind == PlaceKind.HOME }
        val window = home?.let {
            BedtimePolicy.resolveSleepWindow(
                settings = settings,
                homeLatitude = it.latitude,
                homeLongitude = it.longitude,
                arrivedHomeAt = arrived,
                now = now,
            )
        }
        TodayUiState(
            settings = settings,
            hasHome = home != null,
            window = window,
            remaining = window?.remaining(now) ?: Duration.ZERO,
            ongoingSleep = ongoing,
            now = now,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(),
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                tick.value = Instant.now()
                delay(1_000)
            }
        }
    }

    fun startSleep() {
        viewModelScope.launch {
            sleepSessionRepository.start(SleepSource.MANUAL)
        }
    }

    fun endSleep() {
        viewModelScope.launch {
            sleepSessionRepository.endOngoing()
        }
    }

    fun simulateArrivedHome() {
        viewModelScope.launch {
            lifeEventRepository.record(LifeEventType.ENTER_HOME)
            countdownController.startManually()
        }
    }
}
