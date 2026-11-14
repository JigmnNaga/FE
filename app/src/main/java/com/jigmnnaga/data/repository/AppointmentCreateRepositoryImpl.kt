package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.mapper.toDto
import com.jigmnnaga.data.remote.api.AppointmentCreateApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.domain.model.AppointmentCreateRejectReason
import com.jigmnnaga.domain.model.AppointmentCreateResult
import com.jigmnnaga.domain.model.CreatedAppointment
import com.jigmnnaga.domain.model.NewAppointment
import com.jigmnnaga.domain.repository.AppointmentCreateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

class AppointmentCreateRepositoryImpl @Inject constructor(
    private val api: AppointmentCreateApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : AppointmentCreateRepository {

    override suspend fun create(appointment: NewAppointment): AppointmentCreateResult<CreatedAppointment> {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return AppointmentCreateResult.LoginRequired

        return try {
            val response = api.createAppointment("Bearer $token", appointment.toDto())
            if (response.isSuccessful) {
                response.body()?.data?.toDomain()
                    ?.let { AppointmentCreateResult.Success(it) }
                    ?: AppointmentCreateResult.Failure(retryable = false)
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapAppointmentCreateError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppointmentCreateResult.Failure(retryable = true)
        } catch (e: Exception) {
            AppointmentCreateResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapAppointmentCreateError(httpStatus: Int, errorCode: String?): AppointmentCreateResult<Nothing> =
    when {
        errorCode in CREATE_LOGIN_REQUIRED_CODES -> AppointmentCreateResult.LoginRequired
        errorCode == "DUPLICATE_CANDIDATE_DATE" ||
            errorCode == "PAST_CANDIDATE_DATE" ||
            errorCode == "INVALID_CANDIDATE_DATES" ->
            AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_DATES)
        errorCode == "INVALID_RESPONSE_DEADLINE" || errorCode == "CANDIDATE_DATE_BEFORE_RESPONSE_DEADLINE" ->
            AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_DEADLINE)
        errorCode == "INVALID_CANDIDATE_LOCATION_COUNT" ->
            AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_LOCATIONS)
        errorCode == "INVALID_PREVIOUS_MEMBER" || errorCode == "NOT_APPOINTMENT_PARTICIPANT" ->
            AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_PREVIOUS_MEMBERS)
        errorCode == "INVALID_INPUT_VALUE" || httpStatus == 400 ->
            AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_INPUT)
        httpStatus == 401 -> AppointmentCreateResult.LoginRequired
        httpStatus >= 500 -> AppointmentCreateResult.Failure(retryable = true)
        else -> AppointmentCreateResult.Failure(retryable = false)
    }

private val CREATE_LOGIN_REQUIRED_CODES = setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")
