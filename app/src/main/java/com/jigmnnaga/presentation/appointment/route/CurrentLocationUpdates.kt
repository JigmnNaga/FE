package com.jigmnnaga.presentation.appointment.route

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** 이동하는 동안 내 위치를 계속 받는다. 위치를 받을 수 없으면 아무 값도 내지 않고 끝난다. */
fun interface CurrentLocationUpdates {
    fun updates(): Flow<RouteCoordinates>
}

/**
 * 안드로이드 위치 서비스로 위치 변화를 받는다. 모으는 동안만 갱신을 요청하고, 멈추면 요청을 거둔다.
 * 권한이 없거나 켜진 위치 제공자가 없으면 아무 값도 내지 않고 끝난다. 시작할 때 마지막으로 알려진 위치가 있으면 먼저 낸다.
 */
class AndroidCurrentLocationUpdates(context: Context) : CurrentLocationUpdates {
    private val appContext = context.applicationContext

    @SuppressLint("MissingPermission")
    override fun updates(): Flow<RouteCoordinates> = callbackFlow {
        val manager = appContext.getSystemService(LocationManager::class.java)
        val providers = if (manager == null || !appContext.hasLocationPermission()) {
            emptyList()
        } else {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        }
        if (manager == null || providers.isEmpty()) {
            close()
            awaitClose { }
            return@callbackFlow
        }

        // API 30 아래에서는 onStatusChanged도 플랫폼이 부르므로 람다 대신 모두 구현한다.
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(RouteCoordinates(location.latitude, location.longitude))
            }

            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit

            @Deprecated("API 29까지 플랫폼이 부른다.")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { trySend(RouteCoordinates(it.latitude, it.longitude)) }
        providers.forEach { provider ->
            runCatching {
                manager.requestLocationUpdates(provider, UPDATE_INTERVAL_MILLIS, 0f, listener, Looper.getMainLooper())
            }
        }
        awaitClose { manager.removeUpdates(listener) }
    }

    private companion object {
        const val UPDATE_INTERVAL_MILLIS = 5_000L
    }
}
