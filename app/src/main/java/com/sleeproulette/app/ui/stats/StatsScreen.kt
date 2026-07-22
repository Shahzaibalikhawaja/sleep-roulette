package com.sleeproulette.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleeproulette.app.domain.model.ConsistencyStats

@Composable
fun StatsRoute(viewModel: StatsViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    StatsScreen(stats = stats)
}

@Composable
fun StatsScreen(stats: ConsistencyStats?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Consistency", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Last 30 nights",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (stats == null || stats.nightCount == 0) {
            Text(
                text = "Log a few nights to see averages and streaks.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            StatLine("Nights logged", stats.nightCount.toString())
            StatLine(
                "Avg duration",
                stats.averageDurationHours?.let { "%.1fh".format(it) } ?: "—",
            )
            StatLine("Current streak", "${stats.currentStreak} night(s)")
            StatLine(
                "Home → bed median",
                stats.medianHomeToBedMinutes?.let { "${it}m" } ?: "soon",
            )
            StatLine(
                "Goal hit rate",
                stats.goalHitRate?.let { "${(it * 100).toInt()}%" } ?: "soon",
            )
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
