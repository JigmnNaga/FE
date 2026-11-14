package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.mapper.toDto
import com.jigmnnaga.data.remote.api.ConfirmationApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.data.remote.dto.ConfirmDateTimeRequestDto
import com.jigmnnaga.data.remote.dto.ConfirmLocationRequestDto
import com.jigmnnaga.domain.model.ConfirmDetailPlace
import com.jigmnnaga.domain.model.ConfirmationRejectReason
import com.jigmnnaga.domain.model.ConfirmationResult
import com.jigmnnaga.domain.model.TimeCandidates
import com.jigmnnaga.domain.repository.ConfirmationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class ConfirmationRepositoryImpl @Inject constructor(
    private val api: ConfirmationApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : ConfirmationRepository {

    override suspend fun getTimeCandidates(appointmentId: Long): ConfirmationResult<TimeCandidates> = request(
        call = { auth -> api.getHeatmap(auth, appointmentId, HEATMAP_SLOT_MINUTES) },
        onSuccess = { response ->
            response.body()?.data?.toDomain()
                ?.let { ConfirmationResult.Success(it) }
                ?: ConfirmationResult.Failure(retryable = false)
        }
    )

    override suspend fun confirmDateTime(
        appointmentId: Long,
        date: String,
        time: String
    ): ConfirmationResult<Unit> = request(
        call = { auth -> api.confirmDateTime(auth, appointmentId, ConfirmDateTimeRequestDto(date, time)) },
        onSuccess = { ConfirmationResult.Success(Unit) }
    )

    override suspend fun confirmLocation(
        appointmentId: Long,
        locationId: Long,
        detailPlace: ConfirmDetailPlace?
    ): ConfirmationResult<Unit> = request(
        call = { auth ->
            api.confirmLocation(
                auth,
                appointmentId,
                ConfirmLocationRequestDto(locationId, detailPlace?.toDto())
            )
        },
        onSuccess = { ConfirmationResult.Success(Unit) }
    )

    private suspend fun <R, T> request(
        call: suspend (authorization: String) -> Response<R>,
        onSuccess: (Response<R>) -> ConfirmationResult<T>
    ): ConfirmationResult<T> {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return ConfirmationResult.LoginRequired

        return try {
            val response = call("Bearer $token")
            if (response.isSuccessful) {
                onSuccess(response)
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapConfirmationError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ConfirmationResult.Failure(retryable = true)
        } catch (e: Exception) {
            ConfirmationResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val HEATMAP_SLOT_MINUTES = 60
    }
}

internal fun mapConfirmationError(httpStatus: Int, errorCode: String?): ConfirmationResult<Nothing> = when {
    errorCode in CONFIRMATION_LOGIN_REQUIRED_CODES -> ConfirmationResult.LoginRequired
    errorCode == "NOT_APPOINTMENT_HOST" || errorCode == "NOT_APPOINTMENT_PARTICIPANT" -> ConfirmationResult.NotHost
    errorCode == "APPOINTMENT_CANCELED" ->
        ConfirmationResult.Rejected(ConfirmationRejectReason.APPOINTMENT_CANCELED)
    errorCode == "INVALID_CONFIRMED_DATE" -> ConfirmationResult.Rejected(ConfirmationRejectReason.INVALID_DATE)
    errorCode == "INVALID_CONFIRMED_LOCATION" || errorCode == "INVALID_CANDIDATE_LOCATION" ->
        ConfirmationResult.Rejected(ConfirmationRejectReason.INVALID_LOCATION)
    httpStatus == 401 -> ConfirmationResult.LoginRequired
    httpStatus == 403 || httpStatus == 404 -> ConfirmationResult.NotHost
    httpStatus >= 500 -> ConfirmationResult.Failure(retryable = true)
    else -> ConfirmationResult.Failure(retryable = false)
}

private val CONFIRMATION_LOGIN_REQUIRED_CODES = setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")
