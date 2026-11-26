package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.mapper.toDto
import com.jigmnnaga.data.remote.api.NotificationSettingsApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.NotificationSettingsDto
import com.jigmnnaga.domain.model.NotificationSettings
import com.jigmnnaga.domain.model.NotificationSettingsSyncResult
import com.jigmnnaga.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

/**
 * 서버(GET·PUT /api/v1/members/me/notification-settings)와 기기 저장값을 맞춘다.
 * 받기와 바꾸기를 한 번에 하나씩만 실행해서, 늦게 도착한 서버 응답이 방금 바꾼 값을 덮어쓰지 않게 한다.
 */
class NotificationSettingsRepositoryImpl(
    private val api: NotificationSettingsApi,
    private val tokenManager: TokenManager,
    private val json: Json,
    private val localStore: NotificationSettingsLocalStore,
) : NotificationSettingsRepository {

    private val mutex = Mutex()

    override val settings: Flow<NotificationSettings> = localStore.settings

    override suspend fun refresh(): NotificationSettingsSyncResult = mutex.withLock {
        request(call = { auth -> api.getSettings(auth) }, onSuccess = { localStore.save(it) })
    }

    override suspend fun update(
        transform: (NotificationSettings) -> NotificationSettings
    ): NotificationSettingsSyncResult = mutex.withLock {
        // 화면을 떠나도 바꾼 값이 기기와 서버에서 어긋나지 않도록 끝까지 처리한다.
        withContext(NonCancellable) {
            val previous = localStore.settings.first()
            val next = transform(previous)
            if (next == previous) return@withContext NotificationSettingsSyncResult.Success

            localStore.save(next)
            val result = request(
                call = { auth -> api.saveSettings(auth, next.toDto()) },
                onSuccess = { localStore.save(it) },
            )
            if (result != NotificationSettingsSyncResult.Success) localStore.save(previous)
            result
        }
    }

    private suspend fun request(
        call: suspend (authorization: String) -> Response<ApiResponse<NotificationSettingsDto>>,
        onSuccess: suspend (NotificationSettings) -> Unit,
    ): NotificationSettingsSyncResult {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return NotificationSettingsSyncResult.LoginRequired

        return try {
            val response = call("Bearer $token")
            if (response.isSuccessful) {
                val settings = response.body()?.data?.toDomain()
                    ?: return NotificationSettingsSyncResult.Failure(retryable = false)
                onSuccess(settings)
                NotificationSettingsSyncResult.Success
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapNotificationSettingsError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            NotificationSettingsSyncResult.Failure(retryable = true)
        } catch (e: Exception) {
            NotificationSettingsSyncResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapNotificationSettingsError(
    httpStatus: Int,
    errorCode: String? = null,
): NotificationSettingsSyncResult = when {
    errorCode in NOTIFICATION_LOGIN_REQUIRED_CODES -> NotificationSettingsSyncResult.LoginRequired
    httpStatus == 401 -> NotificationSettingsSyncResult.LoginRequired
    httpStatus >= 500 -> NotificationSettingsSyncResult.Failure(retryable = true)
    else -> NotificationSettingsSyncResult.Failure(retryable = false)
}

private val NOTIFICATION_LOGIN_REQUIRED_CODES =
    setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")
