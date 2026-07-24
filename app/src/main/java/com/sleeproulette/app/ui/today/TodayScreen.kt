package com.sleeproulette.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleeproulette.app.domain.tonight.TonightPhase
import com.sleeproulette.app.domain.time.AppTimeFormat
import com.sleeproulette.app.ui.components.OverflowMenu
import com.sleeproulette.app.ui.components.ScreenHeader
import com.sleeproulette.app.ui.components.SoftCard
import java.time.Duration

@Composable
fun TodayRoute(
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        state = state,
        onStartSleep = viewModel::startSleep,
        onEndSleep = viewModel::endSleep,
        onStartWindDown = viewModel::startWindDown,
    )
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    onStartSleep: () -> Unit,
    onEndSleep: () -> Unit,
    onStartWindDown: () -> Unit,
) {
    val use24 = state.settings.use24HourClock
    val startedFmt = AppTimeFormat.timeFormatter(use24)
    val dateTimeFmt = AppTimeFormat.weekdayTimeFormatter(use24)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(
            title = "Tonight",
            subtitle = phaseSubtitle(state.phase),
            actions = {
                if (state.phase != TonightPhase.Sleeping && state.hasHome) {
                    OverflowMenu(
                        items = listOf(
                            "Start wind-down" to onStartWindDown,
                        ),
                    )
                }
            },
        )

        SoftCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (state.phase) {
                    TonightPhase.Sleeping -> {
                        val elapsed = state.ongoingSleep?.let {
                            Duration.between(it.startAt, state.now).coerceAtLeast(Duration.ZERO)
                        } ?: Duration.ZERO
                        Text(
                            text = formatHoursMinutes(elapsed),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "since ${startedFmt.format(state.ongoingSleep!!.startAt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TonightPhase.WindDown -> {
                        Text(
                            text = formatHoursMinutes(state.remaining.abs()),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (state.remaining.isNegative || state.remaining.isZero) {
                                "past goal"
                            } else {
                                "until goal"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TonightPhase.Complete -> {
                        val last = state.lastCompleted
                        if (last?.endAt != null) {
                            val duration = Duration.between(last.startAt, last.endAt)
                            Text(
                                text = formatHoursMinutes(duration),
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${dateTimeFmt.format(last.startAt)} → ${startedFmt.format(last.endAt)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    TonightPhase.Away -> {
                        val window = state.window
                        Text(
                            text = window?.goalBedtime?.let {
                                AppTimeFormat.formatTemporal(it, use24)
                            } ?: "—",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "next goal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                val window = state.window
                if (window != null && state.phase != TonightPhase.Sleeping) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = buildString {
                            if (state.phase == TonightPhase.Complete || state.phase == TonightPhase.Away) {
                                append("Tonight ${AppTimeFormat.formatTemporal(window.goalBedtime, use24)}")
                            } else {
                                append("Goal ${AppTimeFormat.formatTemporal(window.goalBedtime, use24)}")
                            }
                            window.sunrise?.let {
                                append(" · Sunrise ${AppTimeFormat.formatTemporal(it, use24)}")
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        when (state.phase) {
            TonightPhase.Sleeping -> {
                Button(
                    onClick = onEndSleep,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Wake up")
                }
            }
            TonightPhase.Away,
            TonightPhase.WindDown,
            TonightPhase.Complete,
            -> {
                Button(
                    onClick = onStartSleep,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Start sleep")
                }
            }
        }
    }
}

private fun phaseSubtitle(phase: TonightPhase): String = when (phase) {
    TonightPhase.Away -> "Away"
    TonightPhase.WindDown -> "Wind-down"
    TonightPhase.Sleeping -> "Sleeping"
    TonightPhase.Complete -> "Rest logged"
}

/** Calm H/M display — no ticking seconds. */
private fun formatHoursMinutes(duration: Duration): String {
    val totalMinutes = duration.toMinutes().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}
