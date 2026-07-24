package com.sleeproulette.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.repo.LifeEventRepository
import com.sleeproulette.app.domain.repo.SleepSessionRepository
import com.sleeproulette.app.domain.trends.TrendsAnalytics
import com.sleeproulette.app.domain.trends.TrendsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class TrendsUiState(
    val days7: TrendsSnapshot? = null,
    val days30: TrendsSnapshot? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val sleepSessionRepository: SleepSessionRepository,
    private val lifeEventRepository: LifeEventRepository,
) : ViewModel() {

    val uiState: StateFlow<TrendsUiState> = sleepSessionRepository
        .observeSessions()
        .mapLatest { sessions ->
            val now = Instant.now()
            val since7 = now.minus(7, ChronoUnit.DAYS)
            val since30 = now.minus(30, ChronoUnit.DAYS)
            val enterHomes = lifeEventRepository.ofTypeSince(
                LifeEventType.ENTER_HOME,
                since30.minus(TrendsAnalytics.HomeMatchWindow),
            )
            TrendsUiState(
                days7 = TrendsAnalytics.compute(
                    sessions.filter { !it.startAt.isBefore(since7) },
                    enterHomes,
                ),
                days30 = TrendsAnalytics.compute(
                    sessions.filter { !it.startAt.isBefore(since30) },
                    enterHomes,
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrendsUiState())
}
