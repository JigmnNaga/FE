package com.jigmnnaga.presentation.mypage.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

/** 프로필 편집 화면 네비게이션 목적지 타입. */
@Serializable
data object ProfileEditRoute

/**
 * 프로필 편집 화면 Route Composable.
 *
 * ViewModel 연동 전까지는 Mock 데이터를 사용한다.
 * 사진 변경 바텀시트는 ProfileEditScreen 내부에서 로컬 상태로 관리한다.
 */
@Composable
fun ProfileEditRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nickname by remember { mutableStateOf("김지금") }
    ProfileEditScreen(
        state = ProfileEditUiState(
            nickname = nickname,
            profileInitial = nickname.firstOrNull()?.toString(),
            joinDate = "2026. 04. 12",
            totalAppointments = 23,
            isSaveEnabled = nickname.isNotBlank()
        ),
        onBackClick = onBackClick,
        onNicknameChange = { nickname = it },
        onSaveClick = { /* TODO: 저장 처리 */ },
        modifier = modifier
    )
}
