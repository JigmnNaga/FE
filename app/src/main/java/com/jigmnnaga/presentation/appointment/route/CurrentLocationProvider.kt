package com.jigmnnaga.presentation.appointment.route

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** 현재 위치 좌표. */
data class RouteCoordinates(val latitude: Double, val longitude: Double)

/** 현재 위치를 한 번 가져온다. 가져오지 못하면 null을 돌려 준다. */
fun interface CurrentLocationProvider {
    suspend fun current(): RouteCoordinates?
}

/** 위치 권한을 이미 받았는지. */
fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** 안드로이드 위치 서비스로 현재 위치를 가져온다. 권한이 없거나 위치를 켜지 않았으면 null이다. */
class AndroidCurrentLocationProvider(context: Context) : CurrentLocationProvider {
    private val appContext = context.applicationContext

    @SuppressLint("MissingPermission")
    override suspend fun current(): RouteCoordinates? = withContext(Dispatchers.Main) {
        if (!appContext.hasLocationPermission()) return@withContext null
        val manager = appContext.getSystemService(LocationManager::class.java) ?: return@withContext null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return@withContext null

        val lastKnown = providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val fresh = withTimeoutOrNull(FreshLocationTimeoutMillis) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val signal = CancellationSignal()
                    continuation.invokeOnCancellation { signal.cancel() }
                    manager.getCurrentLocation(providers.first(), signal, ContextCompat.getMainExecutor(appContext)) {
                        continuation.resume(it)
                    }
                }
            }
            (fresh ?: lastKnown)?.toCoordinates()
        } else {
            lastKnown?.toCoordinates()
        }
    }

    private fun Location.toCoordinates() = RouteCoordinates(latitude, longitude)

    private companion object {
        const val FreshLocationTimeoutMillis = 5_000L
    }
}
