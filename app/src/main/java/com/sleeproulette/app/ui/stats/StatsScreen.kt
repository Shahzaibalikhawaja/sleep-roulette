package com.sleeproulette.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleeproulette.app.domain.trends.GoalHitPoint
import com.sleeproulette.app.domain.trends.HomeToBedPoint
import com.sleeproulette.app.domain.trends.TrendsSnapshot
import com.sleeproulette.app.ui.components.MetricBlock
import com.sleeproulette.app.ui.components.QuietEmpty
import com.sleeproulette.app.ui.components.ScreenHeader
import com.sleeproulette.app.ui.components.SoftCard
import com.sleeproulette.app.ui.theme.SleepRouletteColors
import kotlin.math.roundToLong

@Composable
fun StatsRoute(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(state = state)
}

@Composable
fun StatsScreen(state: TrendsUiState) {
    val primary = state.days30
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(
            title = "Trends",
            subtitle = "Goal timing, not vanity hours",
        )

        if (primary == null || primary.nightCount == 0) {
            QuietEmpty(message = "Log a few nights to see trends")
            return
        }

        SoftCard {
            Text("Goal hit rate", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricBlock(
                    label = "7 nights",
                    value = formatRate(state.days7?.goalHitRate),
                )
                MetricBlock(
                    label = "30 nights",
                    value = formatRate(primary.goalHitRate),
                )
                MetricBlock(
                    label = "Streak",
                    value = when (primary.goalHitStreak) {
                        0 -> "—"
                        1 -> "1 night"
                        else -> "${primary.goalHitStreak} nights"
                    },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = goalHitSummary(primary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (primary.goalHitSeries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                GoalHitChart(
                    points = primary.goalHitSeries,
                    description = goalHitSummary(primary),
                )
            }
        }

        SoftCard {
            Text("Home → bed", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            MetricBlock(
                label = "Median (30 nights)",
                value = primary.medianHomeToBedMinutes?.let { "${it}m" } ?: "—",
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = homeToBedSummary(primary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (primary.homeToBedSeries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HomeToBedChart(
                    points = primary.homeToBedSeries,
                    description = homeToBedSummary(primary),
                )
            }
        }

        SoftCard {
            Text("Context", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricBlock(label = "Nights", value = primary.nightCount.toString())
                MetricBlock(
                    label = "Avg sleep",
                    value = primary.averageDurationHours?.let(::formatHours) ?: "—",
                )
            }
        }
    }
}

@Composable
private fun GoalHitChart(
    points: List<GoalHitPoint>,
    description: String,
) {
    val barColor = SleepRouletteColors.Mint
    val missColor = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .semantics { contentDescription = description },
    ) {
        if (points.isEmpty()) return@Canvas
        val gap = 6.dp.toPx()
        val barWidth = ((size.width - gap * (points.size - 1)) / points.size)
            .coerceAtLeast(4.dp.toPx())
        val maxH = size.height
        points.forEachIndexed { index, point ->
            val h = if (point.hit) maxH * 0.85f else maxH * 0.35f
            val left = index * (barWidth + gap)
            drawRect(
                color = if (point.hit) barColor else missColor,
                topLeft = Offset(left, maxH - h),
                size = Size(barWidth, h),
            )
        }
    }
}

@Composable
private fun HomeToBedChart(
    points: List<HomeToBedPoint>,
    description: String,
) {
    val lineColor = SleepRouletteColors.PowderBlueDeep
    val maxMinutes = (points.maxOfOrNull { it.minutes } ?: 1L).coerceAtLeast(1).toFloat()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .semantics { contentDescription = description },
    ) {
        if (points.isEmpty()) return@Canvas
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = if (points.size == 1) {
                size.width / 2f
            } else {
                index * (size.width / (points.size - 1))
            }
            val y = size.height - (point.minutes / maxMinutes) * size.height * 0.9f - 4.dp.toPx()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun formatRate(rate: Double?): String =
    rate?.let { "${(it * 100).toInt()}%" } ?: "—"

private fun goalHitSummary(snapshot: TrendsSnapshot): String {
    val rate = snapshot.goalHitRate
    val n = snapshot.goalHitSeries.size
    return when {
        n == 0 -> "Older nights without a goal snapshot are excluded."
        rate == null -> "No goal snapshots yet."
        else -> {
            val hits = snapshot.goalHitSeries.count { it.hit }
            "Hit goal on $hits of $n nights with a snapshot."
        }
    }
}

private fun homeToBedSummary(snapshot: TrendsSnapshot): String {
    val median = snapshot.medianHomeToBedMinutes
    val n = snapshot.homeToBedSeries.size
    return when {
        n == 0 -> "Needs Home arrivals matched to sleep starts."
        median == null -> "Not enough Home → bed pairs yet."
        else -> "Median $median minutes from Home to sleep start ($n nights)."
    }
}

private fun formatHours(decimalHours: Double): String {
    val totalMinutes = (decimalHours * 60).roundToLong()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}
