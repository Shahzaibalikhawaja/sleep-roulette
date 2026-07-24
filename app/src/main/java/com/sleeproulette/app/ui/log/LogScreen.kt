package com.sleeproulette.app.ui.log

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleeproulette.app.domain.model.SleepSession
import com.sleeproulette.app.domain.time.AppTimeFormat
import com.sleeproulette.app.ui.components.OverflowMenu
import com.sleeproulette.app.ui.components.QuietEmpty
import com.sleeproulette.app.ui.components.ScreenHeader
import com.sleeproulette.app.ui.components.SoftCard
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LogRoute(viewModel: LogViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LogScreen(
        state = state,
        onOpenEdit = viewModel::openEdit,
        onDismissEdit = viewModel::dismissEdit,
        onUpdateEditing = viewModel::updateEditing,
        onSaveEdit = viewModel::saveEdit,
        onRequestDelete = viewModel::requestDelete,
        onDismissDelete = viewModel::dismissDelete,
        onConfirmDelete = viewModel::confirmDelete,
    )
}

@Composable
fun LogScreen(
    state: LogUiState,
    onOpenEdit: (SleepSession) -> Unit,
    onDismissEdit: () -> Unit,
    onUpdateEditing: (Instant, Instant?) -> Unit,
    onSaveEdit: () -> Unit,
    onRequestDelete: (Long) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM")
        .withZone(ZoneId.systemDefault())
    val timeFmt = AppTimeFormat.timeFormatter(state.use24HourClock)
    val dateTimeFmt = AppTimeFormat.dateTimeFormatter(state.use24HourClock)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
    ) {
        ScreenHeader(title = "Log")

        if (state.sessions.isEmpty()) {
            QuietEmpty(message = "No sleep logged yet")
        } else {
            LazyColumn(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.sessions, key = { it.id }) { session ->
                    SoftCard(onClick = { onOpenEdit(session) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dateFmt.format(session.startAt),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                val end = session.endAt
                                Text(
                                    text = if (end == null) {
                                        "Sleeping · ${timeFmt.format(session.startAt)}–"
                                    } else {
                                        "${timeFmt.format(session.startAt)}–${timeFmt.format(end)} · " +
                                            formatDurationHoursMinutes(Duration.between(session.startAt, end))
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OverflowMenu(
                                items = listOf(
                                    "Delete" to { onRequestDelete(session.id) },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    state.editing?.let { edit ->
        EditSessionDialog(
            session = edit,
            use24HourClock = state.use24HourClock,
            dateTimeFmt = dateTimeFmt,
            error = state.editError,
            onDismiss = onDismissEdit,
            onUpdate = onUpdateEditing,
            onSave = onSaveEdit,
            onRequestDelete = { onRequestDelete(edit.id) },
        )
    }

    state.confirmDeleteId?.let {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete sleep?") },
            text = { Text("This night will be removed from your log.") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EditSessionDialog(
    session: SleepSession,
    use24HourClock: Boolean,
    dateTimeFmt: DateTimeFormatter,
    error: String?,
    onDismiss: () -> Unit,
    onUpdate: (Instant, Instant?) -> Unit,
    onSave: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()

    fun pickDateTime(current: Instant, onPicked: (Instant) -> Unit) {
        val zdt = current.atZone(zone)
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val date = LocalDate.of(year, month + 1, day)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val time = LocalTime.of(hour, minute)
                        onPicked(LocalDateTime.of(date, time).atZone(zone).toInstant())
                    },
                    zdt.hour,
                    zdt.minute,
                    use24HourClock,
                ).show()
            },
            zdt.year,
            zdt.monthValue - 1,
            zdt.dayOfMonth,
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit sleep") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        pickDateTime(session.startAt) { start ->
                            onUpdate(start, session.endAt)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Start  ${dateTimeFmt.format(session.startAt)}")
                }
                OutlinedButton(
                    onClick = {
                        val seed = session.endAt ?: Instant.now()
                        pickDateTime(seed) { end ->
                            onUpdate(session.startAt, end)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (session.endAt == null) {
                            "End  (ongoing)"
                        } else {
                            "End  ${dateTimeFmt.format(session.endAt)}"
                        },
                    )
                }
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = {
                    onDismiss()
                    onRequestDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = error == null,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun formatDurationHoursMinutes(duration: Duration): String {
    val totalMinutes = duration.toMinutes().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}
