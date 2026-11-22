package com.jigmnnaga.presentation.mypage.sharing

import com.jigmnnaga.domain.model.LocationSharingScope
import com.jigmnnaga.domain.model.LocationSharingStartTime

/**
 * 위치 공유 설정 화면(MYP-004) 상태. Figma 기본값은 공유 사용 켜짐, 약속에 참여한 친구만, 약속 30분 전이고
 * 위치 권한은 허용된 모습이다.
 * [saveFailed]는 서버에 저장하지 못해 이전 값으로 되돌렸을 때, [loginRequired]는 로그인이 만료됐을 때
 * 화면이 한 번 알리고 [LocationSharingSettingsViewModel]에 처리했다고 돌려주는 신호다.
 */
data class LocationSharingSettingsUiState(
    val sharingEnabled: Boolean = true,
    val scope: LocationSharingScope = LocationSharingScope.FRIENDS_ONLY,
    val startTime: LocationSharingStartTime = LocationSharingStartTime.MINUTES_30,
    val locationPermissionGranted: Boolean = true,
    val saveFailed: Boolean = false,
    val loginRequired: Boolean = false
)
