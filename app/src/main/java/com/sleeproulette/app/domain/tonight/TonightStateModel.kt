package com.sleeproulette.app.domain.tonight

import com.sleeproulette.app.domain.model.SleepSession
import java.time.Duration
import java.time.Instant

/**
 * Explicit Tonight screen phases — one primary action each.
 *
 * Away → (ENTER_HOME) → WindDown → (Start sleep) → Sleeping → (Wake up) → Complete
 * WindDown → (EXIT_HOME) → Away
 */
enum class TonightPhase {
    Away,
    WindDown,
    Sleeping,
    Complete,
}

object TonightStateModel {

    /** Max age of a completed sleep that still counts as "today's Complete" daytime view. */
    val CompleteWindow: Duration = Duration.ofHours(18)

    fun derive(
        ongoing: SleepSession?,
        lastCompleted: SleepSession?,
        latestEnterHome: Instant?,
        latestExitHome: Instant?,
        now: Instant = Instant.now(),
    ): TonightPhase {
        if (ongoing != null) return TonightPhase.Sleeping

        val lastSleepEnd = lastCompleted?.endAt
        val enterAfterLastSleep = latestEnterHome != null &&
            (lastSleepEnd == null || latestEnterHome.isAfter(lastSleepEnd))
        val atHome = latestEnterHome != null &&
            (latestExitHome == null || latestEnterHome.isAfter(latestExitHome))

        if (atHome && enterAfterLastSleep) return TonightPhase.WindDown

        if (lastCompleted?.endAt != null) {
            val age = Duration.between(lastCompleted.endAt, now)
            if (!age.isNegative && age <= CompleteWindow) {
                return TonightPhase.Complete
            }
        }

        return TonightPhase.Away
    }
}
