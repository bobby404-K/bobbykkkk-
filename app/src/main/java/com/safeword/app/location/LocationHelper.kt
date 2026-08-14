package com.safeword.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermissions()) {
            return null
        }

        // 1. Try to get last known location first (fast)
        val lastLocation = suspendCancellableCoroutine<Location?> { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }

        // If last location is fresh enough (within 30 seconds), return it.
        if (lastLocation != null && (System.currentTimeMillis() - lastLocation.time) < 30_000) {
            return lastLocation
        }

        // 2. Fetch fresh high-accuracy location fix
        return suspendCancellableCoroutine { continuation ->
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener {
                continuation.resume(null)
            }
            continuation.invokeOnCancellation {
                cts.cancel()
            }
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val fineLocation = android.Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocation = android.Manifest.permission.ACCESS_COARSE_LOCATION
        return context.checkSelfPermission(fineLocation) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(coarseLocation) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
