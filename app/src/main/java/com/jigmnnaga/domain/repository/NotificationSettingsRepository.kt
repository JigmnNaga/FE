package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.NotificationSettings
import com.jigmnnaga.domain.model.NotificationSettingsSaveResult
import kotlinx.coroutines.flow.Flow

/**
 * 알림 설정을 읽고 바꾼다.
 * 현재는 기기 DataStore에만 저장하며, 저장된 값이 없으면 [NotificationSettings]의 기본값을 돌려 준다.
 * BE 전역 알림 설정 API 추가 시 서버 동기화 메서드(refresh)를 추가한다.
 */
interface NotificationSettingsRepository {
    /** 기기에 저장된 설정. 사용자가 바꾸면 바로 바뀐다. */
    val settings: Flow<NotificationSettings>

    /**
     * 현재 설정을 [transform]으로 바꿔 기기에 저장한다.
     * 저장에 실패하면 [NotificationSettingsSaveResult.Failure]를 돌려 준다.
     */
    suspend fun update(transform: (NotificationSettings) -> NotificationSettings): NotificationSettingsSaveResult
}
