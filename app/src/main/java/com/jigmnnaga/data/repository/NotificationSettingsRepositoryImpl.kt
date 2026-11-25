package com.jigmnnaga.data.repository

import com.jigmnnaga.domain.model.NotificationSettings
import com.jigmnnaga.domain.model.NotificationSettingsSaveResult
import com.jigmnnaga.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * 알림 설정을 기기 DataStore에 저장한다.
 * BE 전역 알림 설정 API가 없어 현재는 로컬 저장만 한다.
 * API 추가 시 [update]에 서버 저장 로직을 추가하고 refresh()를 구현한다.
 */
class NotificationSettingsRepositoryImpl(
    private val localStore: NotificationSettingsLocalStore
) : NotificationSettingsRepository {

    override val settings: Flow<NotificationSettings> = localStore.settings

    override suspend fun update(
        transform: (NotificationSettings) -> NotificationSettings
    ): NotificationSettingsSaveResult {
        return try {
            val previous = localStore.settings.first()
            val next = transform(previous)
            if (next == previous) return NotificationSettingsSaveResult.Success
            localStore.save(next)
            NotificationSettingsSaveResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            NotificationSettingsSaveResult.Failure(retryable = true)
        } catch (e: Exception) {
            NotificationSettingsSaveResult.Failure(retryable = false)
        }
    }
}
