package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.LocationSharingSettingsDto
import com.jigmnnaga.domain.model.LocationSharingScope
import com.jigmnnaga.domain.model.LocationSharingSettings
import com.jigmnnaga.domain.model.LocationSharingStartTime

private const val SCOPE_PARTICIPANTS = "PARTICIPANTS"
private const val SCOPE_NONE = "NONE"

/** 서버 값을 앱 설정으로 바꾼다. 앱이 모르는 범위나 시작 시간이 오면 null이다. */
fun LocationSharingSettingsDto.toDomain(): LocationSharingSettings? {
    val scope = when (scope) {
        SCOPE_PARTICIPANTS -> LocationSharingScope.FRIENDS_ONLY
        SCOPE_NONE -> LocationSharingScope.NONE
        else -> return null
    }
    val startTime = LocationSharingStartTime.entries.firstOrNull { it.minutesBefore == startMinutesBefore }
        ?: return null
    return LocationSharingSettings(sharingEnabled = enabled, scope = scope, startTime = startTime)
}

fun LocationSharingSettings.toDto(): LocationSharingSettingsDto = LocationSharingSettingsDto(
    enabled = sharingEnabled,
    scope = when (scope) {
        LocationSharingScope.FRIENDS_ONLY -> SCOPE_PARTICIPANTS
        LocationSharingScope.NONE -> SCOPE_NONE
    },
    startMinutesBefore = startTime.minutesBefore
)
