package com.sleeproulette.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TodayRoute(
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        state = state,
        onStartSleep = viewModel::startSleep,
        onEndSleep = viewModel::endSleep,
        onSimulateHome = viewModel::simulateArrivedHome,
    )
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    onStartSleep: () -> Unit,
    onEndSleep: () -> Unit,
    onSimulateHome: () -> Unit,
) {
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val startedFmt = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
    val sleeping = state.ongoingSleep != null
    val sleepElapsed = state.ongoingSleep?.let { session ->
        Duration.between(session.startAt, state.now).coerceAtLeast(Duration.ZERO)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Sleep Roulette",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = when {
                    sleeping -> "Sleep in progress — timer runs from when you tapped Start"
                    state.hasHome -> "Your sleep window for tonight"
                    else -> "Set Home in Setup to unlock countdown + geofence"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(20.dp),
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (sleeping && sleepElapsed != null) {
                        Text(
                            text = formatClock(sleepElapsed),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "asleep (since ${startedFmt.format(state.ongoingSleep!!.startAt)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = formatCountdown(state.remaining),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = if (state.remaining.isNegative || state.remaining.isZero) {
                                "past goal bedtime"
                            } else {
                                "until goal bedtime"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    val window = state.window
                    if (window != null) {
                        Text(
                            text = "Goal ${window.goalBedtime.format(timeFmt)}" +
                                (window.sunrise?.let { " · Sunrise ${it.format(timeFmt)}" } ?: ""),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            if (sleeping) {
                Button(
                    onClick = onEndSleep,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("I'm awake — end sleep")
                }
            } else {
                Button(
                    onClick = onStartSleep,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Start sleep now")
                }
            }

            OutlinedButton(
                onClick = onSimulateHome,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.hasHome,
            ) {
                Text("I'm home (start countdown)")
            }

            TextButton(onClick = { /* nav hint */ }) {
                Text(
                    text = "Tip: disable battery optimization for Sleep Roulette on ColorOS",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Elapsed / countdown as HH:MM:SS from a non-negative duration. */
private fun formatClock(duration: Duration): String {
    val total = duration.seconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

/**
 * Countdown to goal. When past goal, show how long past (not a fake sleep duration).
 */
private fun formatCountdown(duration: Duration): String {
    if (duration.isNegative || duration.isZero) {
        return formatClock(duration.abs())
    }
    return formatClock(duration)
}
