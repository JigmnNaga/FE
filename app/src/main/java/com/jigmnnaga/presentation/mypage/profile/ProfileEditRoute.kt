package com.jigmnnaga.presentation.mypage.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import java.io.File

/** 프로필 편집 화면 네비게이션 목적지 타입. */
@Serializable
data object ProfileEditRoute

/**
 * 프로필 편집 화면 Route Composable.
 *
 * ViewModel은 MypageNavigation에서 주입한다.
 * 갤러리/카메라 launcher를 소유하고 이미지 바이트를 ViewModel에 전달한다.
 */
@Composable
fun ProfileEditRoute(
    viewModel: ProfileEditViewModel,
    onBackClick: () -> Unit,
    onLoginRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.loginRequired) {
        if (state.loginRequired) {
            viewModel.consumeLoginRequired()
            onLoginRequired()
        }
    }

    // 카메라 촬영용 임시 URI (TakePicture 결과 수신 전까지 보관)
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        viewModel.onImageSelected(contentType, bytes.size.toLong(), bytes)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) return@rememberLauncherForActivityResult
        val uri = cameraImageUri ?: return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        viewModel.onImageSelected("image/jpeg", bytes.size.toLong(), bytes)
    }

    ProfileEditScreen(
        state = state,
        onBackClick = onBackClick,
        onNicknameChange = viewModel::onNicknameChange,
        onSaveClick = viewModel::onSaveClick,
        onSelectFromLibrary = { galleryLauncher.launch("image/*") },
        onTakePhoto = {
            val photoFile = File(
                context.cacheDir,
                "camera_images/${System.currentTimeMillis()}.jpg"
            ).also { it.parentFile?.mkdirs() }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        },
        onDeletePhoto = viewModel::onDeletePhoto,
        modifier = modifier
    )
}
