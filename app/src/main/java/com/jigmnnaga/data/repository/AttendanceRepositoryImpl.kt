package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDto
import com.jigmnnaga.data.remote.api.AttendanceApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.domain.model.AttendanceAnswer
import com.jigmnnaga.domain.model.AttendanceRejectReason
import com.jigmnnaga.domain.model.AttendanceResult
import com.jigmnnaga.domain.repository.AttendanceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

class AttendanceRepositoryImpl @Inject constructor(
    private val api: AttendanceApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : AttendanceRepository {

    override suspend fun respond(
        appointmentId: Long,
        answer: AttendanceAnswer
    ): AttendanceResult<Unit> {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return AttendanceResult.LoginRequired

        return try {
            val response = api.respond("Bearer $token", appointmentId, answer.toDto())
            if (response.isSuccessful) {
                AttendanceResult.Success(Unit)
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapAttendanceError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AttendanceResult.Failure(retryable = true)
        } catch (e: Exception) {
            AttendanceResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapAttendanceError(httpStatus: Int, errorCode: String?): AttendanceResult<Nothing> = when {
    errorCode in ATTENDANCE_LOGIN_REQUIRED_CODES -> AttendanceResult.LoginRequired
    errorCode == "NOT_APPOINTMENT_PARTICIPANT" -> AttendanceResult.NotParticipant
    errorCode == "APPOINTMENT_CANCELED" -> AttendanceResult.Rejected(AttendanceRejectReason.APPOINTMENT_CANCELED)
    errorCode == "APPOINTMENT_NOT_CONFIRMED" -> AttendanceResult.Rejected(AttendanceRejectReason.NOT_CONFIRMED)
    errorCode == "APPOINTMENT_TIME_PASSED" -> AttendanceResult.Rejected(AttendanceRejectReason.TIME_PASSED)
    errorCode == "INVALID_ATTENDANCE_STATUS" -> AttendanceResult.Rejected(AttendanceRejectReason.INVALID_ANSWER)
    httpStatus == 401 -> AttendanceResult.LoginRequired
    httpStatus == 403 || httpStatus == 404 -> AttendanceResult.NotParticipant
    httpStatus >= 500 -> AttendanceResult.Failure(retryable = true)
    else -> AttendanceResult.Failure(retryable = false)
}

private val ATTENDANCE_LOGIN_REQUIRED_CODES = setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")
