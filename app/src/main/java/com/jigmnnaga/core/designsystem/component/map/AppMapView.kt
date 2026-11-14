package com.jigmnnaga.core.designsystem.component.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jigmnnaga.BuildConfig
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

/** 지도에 표시할 마커 한 개. 좌표는 위도·경도(WGS84)입니다. */
data class AppMapMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double
)

/** 지도를 쓸 수 없는 이유. 호출 측에서 대체 UI를 고를 때 사용합니다. */
enum class AppMapStatus {
    LOADING,
    READY,
    UNAVAILABLE
}

/** 카카오 앱 키가 없거나, Compose 미리보기처럼 지도를 띄울 수 없는 환경인지 판단합니다. */
fun isMapSupported(appKey: String = BuildConfig.KAKAO_NATIVE_APP_KEY): Boolean = appKey.isNotBlank()

/** 마커가 없으면 기본 위치(서울 시청 부근)를, 있으면 첫 마커를 지도 중심으로 삼습니다. */
fun List<AppMapMarker>.initialCenter(): LatLng {
    val first = firstOrNull()
    return if (first != null) {
        LatLng.from(first.latitude, first.longitude)
    } else {
        LatLng.from(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    }
}

/**
 * 카카오맵을 Compose 안에 띄우는 공통 지도 컴포저블입니다.
 *
 * - 지도 시작·일시정지·재개·종료는 화면 생명주기에 맞춰 이 함수가 처리합니다.
 * - [markers]가 바뀌면 기존 마커를 지우고 새로 그립니다.
 * - 인증 실패 등으로 지도를 쓸 수 없게 되면 [onStatusChange]에 [AppMapStatus.UNAVAILABLE]을 알립니다.
 */
@Composable
fun AppMapView(
    markers: List<AppMapMarker>,
    modifier: Modifier = Modifier,
    zoomLevel: Int = DEFAULT_ZOOM_LEVEL,
    onStatusChange: (AppMapStatus) -> Unit = {}
) {
    if (LocalInspectionMode.current || !isMapSupported()) {
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnStatusChange by rememberUpdatedState(onStatusChange)
    val currentMarkers by rememberUpdatedState(markers)
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    val mapView = remember { MapView(context) }
    val markerStyle = remember(context) {
        LabelStyles.from(
            LabelStyle.from(createMarkerBitmap(context)).setAnchorPoint(0.5f, 0.5f)
        )
    }

    DisposableEffect(mapView, lifecycleOwner) {
        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() = Unit

                override fun onMapError(error: Exception) {
                    kakaoMap = null
                    currentOnStatusChange(AppMapStatus.UNAVAILABLE)
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    currentOnStatusChange(AppMapStatus.READY)
                }

                override fun getPosition(): LatLng = currentMarkers.initialCenter()

                override fun getZoomLevel(): Int = zoomLevel
            }
        )
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            kakaoMap = null
            mapView.finish()
        }
    }

    // 지도 준비가 끝났거나 마커 목록이 바뀔 때마다 마커를 다시 그린다.
    DisposableEffect(kakaoMap, markers) {
        val map = kakaoMap
        if (map != null) {
            val layer = map.labelManager?.layer
            layer?.removeAll()
            markers.forEach { marker ->
                layer?.addLabel(
                    LabelOptions.from(marker.id, LatLng.from(marker.latitude, marker.longitude))
                        .setStyles(markerStyle)
                )
            }
            markers.firstOrNull()?.let { first ->
                map.moveCamera(
                    CameraUpdateFactory.newCenterPosition(
                        LatLng.from(first.latitude, first.longitude),
                        zoomLevel
                    )
                )
            }
        }
        onDispose { }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun createMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (MARKER_SIZE_DP * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = MARKER_BORDER_COLOR
    canvas.drawCircle(center, center, center, paint)
    paint.color = MARKER_FILL_COLOR
    canvas.drawCircle(center, center, center - MARKER_BORDER_DP * density, paint)
    return bitmap
}

private const val DEFAULT_LATITUDE = 37.5665
private const val DEFAULT_LONGITUDE = 126.9780
private const val DEFAULT_ZOOM_LEVEL = 15
private const val MARKER_SIZE_DP = 20
private const val MARKER_BORDER_DP = 3
private const val MARKER_FILL_COLOR = 0xFF566149.toInt()
private const val MARKER_BORDER_COLOR = 0xFFF7F9ED.toInt()
