package com.jigmnnaga.presentation.mypage.sharing

/** 누구에게 내 위치를 보여 줄지. */
enum class LocationSharingScope {
    /** 같은 약속에 참여한 친구에게만 보인다. */
    FRIENDS_ONLY,

    /** 위치를 공유하지 않는다. */
    NONE
}

/** 약속 몇 분 전부터 위치 공유를 자동으로 시작할지. */
enum class LocationSharingStartTime(val minutesBefore: Int) {
    MINUTES_10(10),
    MINUTES_30(30),
    HOUR_1(60)
}

/**
 * 위치 공유 설정 화면(MYP-004) 상태. Figma 기본값은 공유 사용 켜짐, 약속에 참여한 친구만, 약속 30분 전이다.
 */
data class LocationSharingSettingsUiState(
    val sharingEnabled: Boolean = true,
    val scope: LocationSharingScope = LocationSharingScope.FRIENDS_ONLY,
    val startTime: LocationSharingStartTime = LocationSharingStartTime.MINUTES_30
)
