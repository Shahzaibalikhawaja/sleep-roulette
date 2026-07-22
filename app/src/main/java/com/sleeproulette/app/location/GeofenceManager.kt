package com.sleeproulette.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.sleeproulette.app.domain.model.Place
import com.sleeproulette.app.domain.model.PlaceKind
import com.sleeproulette.app.domain.repo.PlaceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers Home/Work geofences with Play Services.
 *
 * ColorOS note: background location + battery exemption are required or
 * fences silently stop firing. Surface that in Setup UI.
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val placeRepository: PlaceRepository,
) {
    private val client = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // GeofencingClient mutates the intent extras — FLAG_MUTABLE is required (API 31+).
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    fun hasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    fun hasBackgroundLocation(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return hasFineLocation()
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun refreshGeofences() {
        if (!hasFineLocation()) return

        val places = buildList {
            placeRepository.getPlace(PlaceKind.HOME)?.let { add(it) }
            placeRepository.getPlace(PlaceKind.WORK)?.let { add(it) }
        }
        if (places.isEmpty()) {
            runCatching { client.removeGeofences(geofencePendingIntent).await() }
            return
        }

        val geofences = places.map { it.toGeofence() }
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        runCatching { client.removeGeofences(geofencePendingIntent).await() }
        client.addGeofences(request, geofencePendingIntent).await()
    }

    private fun Place.toGeofence(): Geofence =
        Geofence.Builder()
            .setRequestId(kind.name)
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT,
            )
            .setLoiteringDelay(30_000)
            .build()
}
