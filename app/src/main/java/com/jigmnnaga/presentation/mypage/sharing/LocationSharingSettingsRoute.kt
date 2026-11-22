package com.jigmnnaga.presentation.mypage.sharing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jigmnnaga.R
import com.jigmnnaga.presentation.appointment.route.hasLocationPermission
import kotlinx.serialization.Serializable

/** 위치 공유 설정 화면 네비게이션 목적지 타입. */
@Serializable
data object LocationSharingSettingsRoute

/**
 * 위치 공유 설정 화면과 [LocationSharingSettingsViewModel]을 연결한다.
 * 앱 설정에서 권한을 바꾸고 돌아오면 화면이 다시 앞에 오는 때 권한 상태를 새로 확인한다.
 * 서버에 저장하지 못하면 안내 문구를 한 번 띄우고, 로그인이 만료됐으면 [onLoginRequired]를 한 번 부른다.
 */
@Composable
fun LocationSharingSettingsRoute(
    viewModel: LocationSharingSettingsViewModel,
    onBackClick: () -> Unit,
    onLoginRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onPermissionChecked(context.hasLocationPermission())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.saveFailed) {
        if (state.saveFailed) {
            viewModel.consumeSaveFailed()
            Toast.makeText(context, R.string.location_sharing_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state.loginRequired) {
        if (state.loginRequired) {
            viewModel.consumeLoginRequired()
            onLoginRequired()
        }
    }

    LocationSharingSettingsScreen(
        state = state,
        onBackClick = onBackClick,
        onSharingEnabledChange = viewModel::onSharingEnabledChange,
        onScopeSelect = viewModel::onScopeSelect,
        onStartTimeSelect = viewModel::onStartTimeSelect,
        onPermissionClick = { context.openAppSettings() },
        modifier = modifier
    )
}

/** 이 앱의 시스템 설정 화면을 연다. 열 수 있는 화면이 없으면 아무 일도 하지 않는다. */
private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
