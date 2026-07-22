package com.sleeproulette.app.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.sleeproulette.app.di.ApplicationScope
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.model.PlaceKind
import com.sleeproulette.app.domain.repo.LifeEventRepository
import com.sleeproulette.app.notify.SleepCountdownController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives geofence transitions from Play Services (can wake the app).
 * Keep work short; hand off to repositories / controllers on the app scope.
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var lifeEventRepository: LifeEventRepository
    @Inject lateinit var countdownController: SleepCountdownController
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transition = event.geofenceTransition
        val ids = event.triggeringGeofences?.map { it.requestId }.orEmpty()

        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                ids.forEach { requestId ->
                    val kind = runCatching { PlaceKind.valueOf(requestId) }.getOrNull() ?: return@forEach
                    when (kind) {
                        PlaceKind.HOME -> when (transition) {
                            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                                lifeEventRepository.record(LifeEventType.ENTER_HOME)
                                countdownController.onArrivedHome()
                            }
                            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                                lifeEventRepository.record(LifeEventType.EXIT_HOME)
                                countdownController.onLeftHome()
                            }
                        }
                        PlaceKind.WORK -> when (transition) {
                            Geofence.GEOFENCE_TRANSITION_ENTER ->
                                lifeEventRepository.record(LifeEventType.ENTER_WORK)
                            Geofence.GEOFENCE_TRANSITION_EXIT ->
                                lifeEventRepository.record(LifeEventType.EXIT_WORK)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
