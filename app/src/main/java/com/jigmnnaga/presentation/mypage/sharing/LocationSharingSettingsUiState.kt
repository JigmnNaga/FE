package com.jigmnnaga.presentation.mypage.sharing

import com.jigmnnaga.domain.model.LocationSharingScope
import com.jigmnnaga.domain.model.LocationSharingStartTime

/**
 * 위치 공유 설정 화면(MYP-004) 상태. Figma 기본값은 공유 사용 켜짐, 약속에 참여한 친구만, 약속 30분 전이고
 * 위치 권한은 허용된 모습이다.
 */
data class LocationSharingSettingsUiState(
    val sharingEnabled: Boolean = true,
    val scope: LocationSharingScope = LocationSharingScope.FRIENDS_ONLY,
    val startTime: LocationSharingStartTime = LocationSharingStartTime.MINUTES_30,
    val locationPermissionGranted: Boolean = true
)
