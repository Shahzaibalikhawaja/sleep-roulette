package com.sleeproulette.app.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleeproulette.app.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Re-register geofences after reboot — OS clears them. */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        applicationScope.launch {
            try {
                geofenceManager.refreshGeofences()
            } finally {
                pending.finish()
            }
        }
    }
}
