package com.sleeproulette.app.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleeproulette.app.domain.model.SleepSession
import com.sleeproulette.app.domain.repo.SleepSessionRepository
import com.sleeproulette.app.domain.trends.TrendsAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class LogUiState(
    val sessions: List<SleepSession> = emptyList(),
    val editing: SleepSession? = null,
    val editError: String? = null,
    val confirmDeleteId: Long? = null,
)

@HiltViewModel
class LogViewModel @Inject constructor(
    private val sleepSessionRepository: SleepSessionRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            sleepSessionRepository.observeSessions().collect { sessions ->
                _ui.update { state ->
                    state.copy(
                        sessions = sessions,
                        editing = state.editing?.let { edit ->
                            sessions.firstOrNull { it.id == edit.id }?.copy(
                                startAt = edit.startAt,
                                endAt = edit.endAt,
                            ) ?: edit
                        },
                    )
                }
            }
        }
    }

    fun openEdit(session: SleepSession) {
        _ui.update { it.copy(editing = session, editError = null) }
    }

    fun dismissEdit() {
        _ui.update { it.copy(editing = null, editError = null) }
    }

    fun updateEditing(startAt: Instant, endAt: Instant?) {
        val current = _ui.value.editing ?: return
        val error = TrendsAnalytics.validateSessionBounds(startAt, endAt)
        _ui.update {
            it.copy(
                editing = current.copy(startAt = startAt, endAt = endAt),
                editError = error,
            )
        }
    }

    fun saveEdit() {
        viewModelScope.launch {
            val edit = _ui.value.editing ?: return@launch
            val error = TrendsAnalytics.validateSessionBounds(edit.startAt, edit.endAt)
            if (error != null) {
                _ui.update { it.copy(editError = error) }
                return@launch
            }
            sleepSessionRepository.upsert(edit)
            _ui.update { it.copy(editing = null, editError = null) }
        }
    }

    fun requestDelete(id: Long) {
        _ui.update { it.copy(confirmDeleteId = id) }
    }

    fun dismissDelete() {
        _ui.update { it.copy(confirmDeleteId = null) }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            val id = _ui.value.confirmDeleteId ?: return@launch
            sleepSessionRepository.delete(id)
            _ui.update {
                it.copy(
                    confirmDeleteId = null,
                    editing = it.editing?.takeIf { edit -> edit.id != id },
                )
            }
        }
    }
}
