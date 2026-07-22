package com.sleeproulette.app.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleeproulette.app.domain.model.SleepSession
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LogRoute(viewModel: LogViewModel = hiltViewModel()) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    LogScreen(sessions = sessions, onDelete = viewModel::delete)
}

@Composable
fun LogScreen(
    sessions: List<SleepSession>,
    onDelete: (Long) -> Unit,
) {
    val fmt = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm")
        .withZone(ZoneId.systemDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text("Sleep log", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Manual sessions first. Suggested detection comes later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )

        if (sessions.isEmpty()) {
            Text(
                text = "No sessions yet. Start one from Today.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(session = session, fmt = fmt, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: SleepSession,
    fmt: DateTimeFormatter,
    onDelete: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fmt.format(session.startAt),
                style = MaterialTheme.typography.titleLarge,
            )
            val end = session.endAt
            Text(
                text = if (end == null) {
                    "Ongoing · ${session.source.name.lowercase()}"
                } else {
                    val hours = session.durationMinutes()?.div(60.0)
                    "→ ${fmt.format(end)} · ${"%.1f".format(hours ?: 0.0)}h · ${session.source.name.lowercase()}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { onDelete(session.id) }) {
            Text("Delete")
        }
    }
}
