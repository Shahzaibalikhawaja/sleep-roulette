package com.sleeproulette.app.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleeproulette.app.domain.model.SleepSession
import com.sleeproulette.app.domain.repo.SleepSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val sleepSessionRepository: SleepSessionRepository,
) : ViewModel() {

    val sessions: StateFlow<List<SleepSession>> = sleepSessionRepository
        .observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { sleepSessionRepository.delete(id) }
    }
}
