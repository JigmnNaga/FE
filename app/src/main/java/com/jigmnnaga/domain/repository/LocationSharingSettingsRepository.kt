package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.LocationSharingScope
import com.jigmnnaga.domain.model.LocationSharingSettings
import com.jigmnnaga.domain.model.LocationSharingStartTime
import kotlinx.coroutines.flow.Flow

/** 위치 공유 설정을 읽고 바꾼다. 저장된 값이 없으면 [LocationSharingSettings]의 기본값을 돌려 준다. */
interface LocationSharingSettingsRepository {
    val settings: Flow<LocationSharingSettings>

    suspend fun setSharingEnabled(enabled: Boolean)

    suspend fun setScope(scope: LocationSharingScope)

    suspend fun setStartTime(startTime: LocationSharingStartTime)
}
