package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DeviceGpsCoordinate(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val bearing: Float,
    val accuracyMeters: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Provides real GPS location telemetry via Google Play Services FusedLocationProviderClient
 * so users can share their live coordinates in real-time.
 */
object LocationServiceHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun observeRealTimeLocation(context: Context): Flow<DeviceGpsCoordinate> = callbackFlow {
        if (!hasLocationPermission(context)) {
            close()
            return@callbackFlow
        }

        val client = LocationServices.getFusedLocationProviderClient(context)

        client.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                trySend(
                    DeviceGpsCoordinate(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        speedKmh = loc.speed * 3.6f,
                        bearing = loc.bearing,
                        accuracyMeters = loc.accuracy
                    )
                )
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(
                    DeviceGpsCoordinate(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        speedKmh = loc.speed * 3.6f,
                        bearing = loc.bearing,
                        accuracyMeters = loc.accuracy
                    )
                )
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: Exception) {
            // Fallback if Play services unavailable in container
        }

        awaitClose {
            client.removeLocationUpdates(callback)
        }
    }
}
