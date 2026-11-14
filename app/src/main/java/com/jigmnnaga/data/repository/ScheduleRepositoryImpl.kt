package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.mapper.toDto
import com.jigmnnaga.data.remote.api.ScheduleApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.domain.model.MySchedule
import com.jigmnnaga.domain.model.ScheduleRejectReason
import com.jigmnnaga.domain.model.ScheduleResult
import com.jigmnnaga.domain.model.ScheduleStatus
import com.jigmnnaga.domain.model.ScheduleSubmission
import com.jigmnnaga.domain.repository.ScheduleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class ScheduleRepositoryImpl @Inject constructor(
    private val api: ScheduleApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : ScheduleRepository {

    override suspend fun getMySchedule(appointmentId: Long): ScheduleResult<MySchedule> = request(
        call = { auth -> api.getMySchedule(auth, appointmentId) },
        onSuccess = { response ->
            response.body()?.data?.toDomain()
                ?.let { ScheduleResult.Success(it) }
                ?: ScheduleResult.Failure(retryable = false)
        }
    )

    override suspend fun getScheduleStatus(appointmentId: Long): ScheduleResult<ScheduleStatus> = request(
        call = { auth -> api.getScheduleStatus(auth, appointmentId) },
        onSuccess = { response ->
            response.body()?.data?.toDomain()
                ?.let { ScheduleResult.Success(it) }
                ?: ScheduleResult.Failure(retryable = false)
        }
    )

    override suspend fun submitSchedule(
        appointmentId: Long,
        submission: ScheduleSubmission
    ): ScheduleResult<Unit> = request(
        call = { auth -> api.submitSchedule(auth, appointmentId, submission.toDto()) },
        onSuccess = { ScheduleResult.Success(Unit) }
    )

    private suspend fun <R, T> request(
        call: suspend (authorization: String) -> Response<R>,
        onSuccess: (Response<R>) -> ScheduleResult<T>
    ): ScheduleResult<T> {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return ScheduleResult.LoginRequired

        return try {
            val response = call("Bearer $token")
            if (response.isSuccessful) {
                onSuccess(response)
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapScheduleError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ScheduleResult.Failure(retryable = true)
        } catch (e: Exception) {
            ScheduleResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapScheduleError(httpStatus: Int, errorCode: String?): ScheduleResult<Nothing> = when {
    errorCode in SCHEDULE_LOGIN_REQUIRED_CODES -> ScheduleResult.LoginRequired
    errorCode == "NOT_APPOINTMENT_PARTICIPANT" -> ScheduleResult.NotParticipant
    errorCode == "APPOINTMENT_CANCELED" -> ScheduleResult.Rejected(ScheduleRejectReason.APPOINTMENT_CANCELED)
    errorCode == "SCHEDULE_RESPONSE_DEADLINE_PASSED" -> ScheduleResult.Rejected(ScheduleRejectReason.DEADLINE_PASSED)
    errorCode == "INVALID_AVAILABLE_DATE" -> ScheduleResult.Rejected(ScheduleRejectReason.INVALID_DATE)
    errorCode == "DUPLICATE_AVAILABLE_DATE" -> ScheduleResult.Rejected(ScheduleRejectReason.DUPLICATE_DATE)
    errorCode == "INVALID_AVAILABLE_TIME" -> ScheduleResult.Rejected(ScheduleRejectReason.INVALID_TIME)
    errorCode == "INVALID_PREFERRED_LOCATION" -> ScheduleResult.Rejected(ScheduleRejectReason.INVALID_LOCATION)
    httpStatus == 401 -> ScheduleResult.LoginRequired
    httpStatus == 403 || httpStatus == 404 -> ScheduleResult.NotParticipant
    httpStatus >= 500 -> ScheduleResult.Failure(retryable = true)
    else -> ScheduleResult.Failure(retryable = false)
}

private val SCHEDULE_LOGIN_REQUIRED_CODES = setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")
