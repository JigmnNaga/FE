package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.api.InviteApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.domain.model.AppointmentInvite
import com.jigmnnaga.domain.model.InviteOperationResult
import com.jigmnnaga.domain.model.InviteUnavailableReason
import com.jigmnnaga.domain.repository.AppointmentInviteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class AppointmentInviteRepositoryImpl @Inject constructor(
    private val api: InviteApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : AppointmentInviteRepository {

    override suspend fun getInvite(inviteCode: String): InviteOperationResult<AppointmentInvite> =
        request(
            call = { auth -> api.getInvite(auth, inviteCode) },
            onSuccess = { response ->
                response.body()?.data?.toDomain()
                    ?.let { InviteOperationResult.Success(it) }
                    ?: InviteOperationResult.Failure(retryable = false)
            }
        )

    override suspend fun acceptInvite(inviteCode: String): InviteOperationResult<Long> =
        request(
            call = { auth -> api.acceptInvite(auth, inviteCode) },
            onSuccess = { response ->
                response.body()?.data?.appointmentId
                    ?.let { InviteOperationResult.Success(it) }
                    ?: InviteOperationResult.Failure(retryable = false)
            }
        )

    override suspend fun declineInvite(inviteCode: String): InviteOperationResult<Unit> =
        request(
            call = { auth -> api.declineInvite(auth, inviteCode) },
            onSuccess = { InviteOperationResult.Success(Unit) }
        )

    private suspend fun <R, T> request(
        call: suspend (authorization: String) -> Response<R>,
        onSuccess: (Response<R>) -> InviteOperationResult<T>
    ): InviteOperationResult<T> {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return InviteOperationResult.LoginRequired

        return try {
            val response = call("Bearer $token")
            if (response.isSuccessful) {
                onSuccess(response)
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapInviteError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            InviteOperationResult.Failure(retryable = true)
        } catch (e: Exception) {
            InviteOperationResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapInviteError(httpStatus: Int, errorCode: String?): InviteOperationResult<Nothing> =
    when (errorCode) {
        "UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN" ->
            InviteOperationResult.LoginRequired
        "INVITE_NOT_FOUND" ->
            InviteOperationResult.Unavailable(InviteUnavailableReason.NOT_FOUND)
        "APPOINTMENT_CANCELED" ->
            InviteOperationResult.Unavailable(InviteUnavailableReason.CANCELED)
        "INVITE_EXPIRED" ->
            InviteOperationResult.Expired
        "KICKED_MEMBER_CANNOT_REJOIN" ->
            InviteOperationResult.Unavailable(InviteUnavailableReason.KICKED)
        "HOST_CANNOT_RESPOND" ->
            InviteOperationResult.Unavailable(InviteUnavailableReason.HOST_CANNOT_RESPOND)
        "ALREADY_RESPONDED" ->
            InviteOperationResult.AlreadyResponded
        else -> when {
            httpStatus == 401 -> InviteOperationResult.LoginRequired
            httpStatus == 404 -> InviteOperationResult.Unavailable(InviteUnavailableReason.NOT_FOUND)
            httpStatus == 409 -> InviteOperationResult.AlreadyResponded
            httpStatus >= 500 -> InviteOperationResult.Failure(retryable = true)
            else -> InviteOperationResult.Failure(retryable = false)
        }
    }
