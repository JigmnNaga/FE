package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.api.ChatApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.data.remote.dto.ChatReadRequestDto
import com.jigmnnaga.domain.model.ChatPage
import com.jigmnnaga.domain.model.ChatResult
import com.jigmnnaga.domain.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : ChatRepository {

    override suspend fun getMessages(appointmentId: Long, cursor: Long?, size: Int): ChatResult<ChatPage> = request(
        call = { auth -> api.getMessages(auth, appointmentId, cursor, size) },
        onSuccess = { response ->
            response.body()?.data?.toDomain()
                ?.let { ChatResult.Success(it) }
                ?: ChatResult.Failure(retryable = false)
        }
    )

    override suspend fun markAsRead(appointmentId: Long, lastReadMessageId: Long): ChatResult<Unit> = request(
        call = { auth -> api.markAsRead(auth, appointmentId, ChatReadRequestDto(lastReadMessageId)) },
        onSuccess = { ChatResult.Success(Unit) }
    )

    private suspend fun <R, T> request(
        call: suspend (authorization: String) -> Response<R>,
        onSuccess: (Response<R>) -> ChatResult<T>
    ): ChatResult<T> {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return ChatResult.LoginRequired

        return try {
            val response = call("Bearer $token")
            if (response.isSuccessful) {
                onSuccess(response)
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapChatError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ChatResult.Failure(retryable = true)
        } catch (e: Exception) {
            ChatResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapChatError(httpStatus: Int, errorCode: String?): ChatResult<Nothing> = when {
    httpStatus == 401 || errorCode in CHAT_LOGIN_REQUIRED_CODES -> ChatResult.LoginRequired
    errorCode == "NOT_APPOINTMENT_PARTICIPANT" || errorCode == "APPOINTMENT_NOT_FOUND" ||
        httpStatus == 403 || httpStatus == 404 -> ChatResult.NotParticipant
    httpStatus >= 500 -> ChatResult.Failure(retryable = true)
    else -> ChatResult.Failure(retryable = false)
}

private val CHAT_LOGIN_REQUIRED_CODES = setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")
