package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.mapper.toDto
import com.jigmnnaga.data.remote.api.AppointmentManageApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.data.remote.dto.AppointmentTitleUpdateRequestDto
import com.jigmnnaga.data.remote.dto.CandidateDatesUpdateRequestDto
import com.jigmnnaga.data.remote.dto.HostDelegateRequestDto
import com.jigmnnaga.data.remote.dto.ResponseDeadlineUpdateRequestDto
import com.jigmnnaga.domain.model.AppointmentManageRejectReason
import com.jigmnnaga.domain.model.AppointmentManageResult
import com.jigmnnaga.domain.model.CandidateLocationChange
import com.jigmnnaga.domain.model.NewAppointmentDateType
import com.jigmnnaga.domain.model.ReminderSendSummary
import com.jigmnnaga.domain.repository.AppointmentManageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class AppointmentManageRepositoryImpl @Inject constructor(
    private val api: AppointmentManageApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : AppointmentManageRepository {

    override suspend fun updateTitle(appointmentId: Long, title: String) = request(
        call = { auth -> api.updateTitle(auth, appointmentId, AppointmentTitleUpdateRequestDto(title)) },
        onSuccess = { AppointmentManageResult.Success(Unit) }
    )

    override suspend fun updateCandidateDates(
        appointmentId: Long,
        dateType: NewAppointmentDateType,
        dates: List<String>
    ) = request(
        call = { auth ->
            api.updateCandidateDates(auth, appointmentId, CandidateDatesUpdateRequestDto(dateType.name, dates))
        },
        onSuccess = { AppointmentManageResult.Success(Unit) }
    )

    override suspend fun updateCandidateLocations(
        appointmentId: Long,
        change: CandidateLocationChange
    ) = request(
        call = { auth -> api.updateCandidateLocations(auth, appointmentId, change.toDto()) },
        onSuccess = { AppointmentManageResult.Success(Unit) }
    )

    override suspend fun updateResponseDeadline(appointmentId: Long, deadline: String) = request(
        call = { auth ->
            api.updateResponseDeadline(auth, appointmentId, ResponseDeadlineUpdateRequestDto(deadline))
        },
        onSuccess = { AppointmentManageResult.Success(Unit) }
    )

    override suspend fun cancel(appointmentId: Long) = request(
        call = { auth -> api.cancel(auth, appointmentId) },
        onSuccess = { AppointmentManageResult.Success(Unit) }
    )

    override suspend fun delete(appointmentId: Long) = request(
        call = { auth -> api.delete(auth, appointmentId) },
        onSuccess = { AppointmentManageResult.Success(Unit) }
    )

    override suspend fun delegateHost(appointmentId: Long, newHostMemberId: Long) = request(
        call = { auth -> api.delegateHost(auth, appointmentId, HostDelegateRequestDto(newHostMemberId)) },
        onSuccess = { AppointmentManageResult.Success(Unit) }
    )

    override suspend fun kickParticipant(appointmentId: Long, targetMemberId: Long) = request(
        call = { auth -> api.kickParticipant(auth, appointmentId, targetMemberId) },
        onSuccess = { AppointmentManageResult.Success(Unit) }
    )

    override suspend fun sendReminders(appointmentId: Long): AppointmentManageResult<ReminderSendSummary> = request(
        call = { auth -> api.sendReminders(auth, appointmentId) },
        onSuccess = { response ->
            response.body()?.data?.toDomain()
                ?.let { AppointmentManageResult.Success(it) }
                ?: AppointmentManageResult.Failure(retryable = false)
        }
    )

    private suspend fun <R, T> request(
        call: suspend (authorization: String) -> Response<R>,
        onSuccess: (Response<R>) -> AppointmentManageResult<T>
    ): AppointmentManageResult<T> {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return AppointmentManageResult.LoginRequired

        return try {
            val response = call("Bearer $token")
            if (response.isSuccessful) {
                onSuccess(response)
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapAppointmentManageError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppointmentManageResult.Failure(retryable = true)
        } catch (e: Exception) {
            AppointmentManageResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapAppointmentManageError(httpStatus: Int, errorCode: String?): AppointmentManageResult<Nothing> = when {
    errorCode in MANAGE_LOGIN_REQUIRED_CODES -> AppointmentManageResult.LoginRequired
    errorCode == "NOT_APPOINTMENT_HOST" || errorCode == "NOT_APPOINTMENT_PARTICIPANT" ->
        AppointmentManageResult.NotHost
    errorCode == "APPOINTMENT_CANCELED" ->
        AppointmentManageResult.Rejected(AppointmentManageRejectReason.APPOINTMENT_CANCELED)
    errorCode == "DUPLICATE_CANDIDATE_DATE" ||
        errorCode == "PAST_CANDIDATE_DATE" ||
        errorCode == "INVALID_CANDIDATE_DATES" ->
        AppointmentManageResult.Rejected(AppointmentManageRejectReason.INVALID_DATES)
    errorCode == "CANDIDATE_DATE_BEFORE_RESPONSE_DEADLINE" ->
        AppointmentManageResult.Rejected(AppointmentManageRejectReason.DATES_BEFORE_DEADLINE)
    errorCode == "INVALID_RESPONSE_DEADLINE" ->
        AppointmentManageResult.Rejected(AppointmentManageRejectReason.INVALID_DEADLINE)
    errorCode == "INVALID_CANDIDATE_LOCATION_COUNT" || errorCode == "INVALID_CANDIDATE_LOCATION" ->
        AppointmentManageResult.Rejected(AppointmentManageRejectReason.INVALID_LOCATIONS)
    errorCode == "MEMBER_NOT_FOUND" ->
        AppointmentManageResult.Rejected(AppointmentManageRejectReason.INVALID_MEMBER)
    errorCode == "INVALID_INPUT_VALUE" || httpStatus == 400 ->
        AppointmentManageResult.Rejected(AppointmentManageRejectReason.INVALID_INPUT)
    httpStatus == 401 -> AppointmentManageResult.LoginRequired
    httpStatus == 403 || httpStatus == 404 -> AppointmentManageResult.NotHost
    httpStatus >= 500 -> AppointmentManageResult.Failure(retryable = true)
    else -> AppointmentManageResult.Failure(retryable = false)
}

private val MANAGE_LOGIN_REQUIRED_CODES = setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")
