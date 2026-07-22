package com.sleeproulette.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleeproulette.app.domain.model.ConsistencyStats
import com.sleeproulette.app.domain.repo.SleepSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    sleepSessionRepository: SleepSessionRepository,
) : ViewModel() {

    val stats: StateFlow<ConsistencyStats?> = sleepSessionRepository
        .observeSessions()
        .mapLatest {
            sleepSessionRepository.stats(Instant.now().minus(30, ChronoUnit.DAYS))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
