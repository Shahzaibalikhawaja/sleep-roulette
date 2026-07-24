package com.sleeproulette.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.model.Place
import com.sleeproulette.app.domain.model.PlaceKind
import com.sleeproulette.app.domain.model.SleepSession
import com.sleeproulette.app.domain.model.SleepSource
import com.sleeproulette.app.domain.model.UserSettings
import com.sleeproulette.app.domain.policy.BedtimePolicy
import com.sleeproulette.app.domain.repo.LifeEventRepository
import com.sleeproulette.app.domain.repo.PlaceRepository
import com.sleeproulette.app.domain.repo.SettingsRepository
import com.sleeproulette.app.domain.repo.SleepSessionRepository
import com.sleeproulette.app.domain.tonight.TonightPhase
import com.sleeproulette.app.domain.tonight.TonightStateModel
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
    val phase: TonightPhase = TonightPhase.Away,
    val window: BedtimePolicy.SleepWindow? = null,
    val remaining: Duration = Duration.ZERO,
    val ongoingSleep: SleepSession? = null,
    val lastCompleted: SleepSession? = null,
    val now: Instant = Instant.now(),
)

private data class TonightCore(
    val settings: UserSettings,
    val places: List<Place>,
    val ongoing: SleepSession?,
    val lastCompleted: SleepSession?,
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

    private val homeTransitions = lifeEventRepository
        .observeRecent(limit = 80)
        .map { events ->
            val enter = events.firstOrNull { it.type == LifeEventType.ENTER_HOME }?.occurredAt
            val exit = events.firstOrNull { it.type == LifeEventType.EXIT_HOME }?.occurredAt
            enter to exit
        }

    private val lastCompleted = sleepSessionRepository
        .observeSessions()
        .map { sessions -> sessions.firstOrNull { it.endAt != null } }

    private val core = combine(
        settingsRepository.settings,
        placeRepository.observePlaces(),
        sleepSessionRepository.observeOngoing(),
        lastCompleted,
    ) { settings, places, ongoing, completed ->
        TonightCore(settings, places, ongoing, completed)
    }

    val uiState: StateFlow<TodayUiState> = combine(
        core,
        homeTransitions,
        tick,
    ) { coreState, transitions, now ->
        val (enter, exit) = transitions
        val home = coreState.places.firstOrNull { it.kind == PlaceKind.HOME }
        val window = BedtimePolicy.resolveSleepWindow(
            settings = coreState.settings,
            homeLatitude = home?.latitude ?: 0.0,
            homeLongitude = home?.longitude ?: 0.0,
            arrivedHomeAt = enter,
            now = now,
        )
        val phase = TonightStateModel.derive(
            ongoing = coreState.ongoing,
            lastCompleted = coreState.lastCompleted,
            latestEnterHome = enter,
            latestExitHome = exit,
            now = now,
        )
        TodayUiState(
            settings = coreState.settings,
            hasHome = home != null,
            phase = phase,
            window = window,
            remaining = window.remaining(now),
            ongoingSleep = coreState.ongoing,
            lastCompleted = coreState.lastCompleted,
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
                delay(60_000)
            }
        }
    }

    fun startSleep() {
        viewModelScope.launch {
            val goalInstant = uiState.value.window?.goalBedtime?.toInstant()
            sleepSessionRepository.start(
                source = SleepSource.MANUAL,
                goalAtStart = goalInstant,
            )
            countdownController.stop()
        }
    }

    fun endSleep() {
        viewModelScope.launch {
            sleepSessionRepository.endOngoing()
        }
    }

    fun startWindDown() {
        viewModelScope.launch {
            lifeEventRepository.record(LifeEventType.ENTER_HOME)
            countdownController.startManually()
        }
    }
}
