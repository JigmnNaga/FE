package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.api.NotificationApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.domain.model.NotificationPage
import com.jigmnnaga.domain.model.NotificationResult
import com.jigmnnaga.domain.repository.NotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val api: NotificationApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : NotificationRepository {

    override suspend fun getNotifications(
        cursor: Long?,
        size: Int
    ): NotificationResult<NotificationPage> = request(
        call = { auth -> api.getNotifications(auth, cursor, size) },
        onSuccess = { response ->
            response.body()?.data?.toDomain()
                ?.let { NotificationResult.Success(it) }
                ?: NotificationResult.Failure(retryable = false)
        }
    )

    override suspend fun readNotification(notificationId: Long): NotificationResult<Unit> = request(
        call = { auth -> api.readNotification(auth, notificationId) },
        onSuccess = { NotificationResult.Success(Unit) }
    )

    override suspend fun readAllNotifications(): NotificationResult<Long> = request(
        call = { auth -> api.readAllNotifications(auth) },
        onSuccess = { response ->
            response.body()?.data?.count
                ?.let { NotificationResult.Success(it) }
                ?: NotificationResult.Failure(retryable = false)
        }
    )

    private suspend fun <R, T> request(
        call: suspend (authorization: String) -> Response<R>,
        onSuccess: (Response<R>) -> NotificationResult<T>
    ): NotificationResult<T> {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return NotificationResult.LoginRequired

        return try {
            val response = call("Bearer $token")
            if (response.isSuccessful) {
                onSuccess(response)
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapNotificationError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            NotificationResult.Failure(retryable = true)
        } catch (e: Exception) {
            NotificationResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapNotificationError(httpStatus: Int, errorCode: String?): NotificationResult<Nothing> =
    when {
        errorCode in LOGIN_REQUIRED_CODES -> NotificationResult.LoginRequired
        httpStatus == 401 -> NotificationResult.LoginRequired
        httpStatus >= 500 -> NotificationResult.Failure(retryable = true)
        else -> NotificationResult.Failure(retryable = false)
    }

private val LOGIN_REQUIRED_CODES = setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")
