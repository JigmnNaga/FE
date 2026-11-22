package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.LocationSharingSettings
import com.jigmnnaga.domain.model.LocationSharingSyncResult
import kotlinx.coroutines.flow.Flow

/**
 * 위치 공유 설정을 읽고 바꾼다. 서버가 설정을 관리하고, 기기에는 마지막으로 맞춘 값을 저장해 둔다.
 * 저장된 값이 없으면 [LocationSharingSettings]의 기본값을 돌려 준다.
 */
interface LocationSharingSettingsRepository {
    /** 기기에 저장된 설정. 서버에서 받거나 사용자가 바꾸면 바로 바뀐다. */
    val settings: Flow<LocationSharingSettings>

    /** 서버에 저장된 설정을 받아 기기 값에 반영한다. 실패하면 기기 값을 그대로 둔다. */
    suspend fun refresh(): LocationSharingSyncResult

    /**
     * 현재 설정을 [transform]으로 바꿔 기기에 먼저 반영하고 서버에 저장한다.
     * 서버 저장에 실패하면 바꾸기 전 값으로 되돌린다.
     */
    suspend fun update(transform: (LocationSharingSettings) -> LocationSharingSettings): LocationSharingSyncResult
}
