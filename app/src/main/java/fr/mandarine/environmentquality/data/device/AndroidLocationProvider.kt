package fr.mandarine.environmentquality.data.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.repository.LocationProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Coarse position from the platform LocationManager: a commune does not need
 * GPS precision, and the app stays free of Play services.
 */
class AndroidLocationProvider(private val context: Context) : LocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun current(): GeoPoint? {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        val manager = ContextCompat.getSystemService(context, LocationManager::class.java) ?: return null
        val providers = manager.getProviders(true)
        val lastKnown = providers.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
        if (lastKnown != null) return lastKnown.toPoint()
        val provider = providers.firstOrNull { it == LocationManager.NETWORK_PROVIDER } ?: providers.firstOrNull() ?: return null
        return withTimeoutOrNull(TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                    continuation.resume(location?.toPoint())
                }
            }
        }
    }

    private fun Location.toPoint() = GeoPoint(latitude, longitude)

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
