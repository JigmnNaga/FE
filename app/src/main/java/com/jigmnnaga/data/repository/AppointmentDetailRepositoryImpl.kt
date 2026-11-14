package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.api.AppointmentDetailApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentDetailResult
import com.jigmnnaga.domain.repository.AppointmentDetailRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

class AppointmentDetailRepositoryImpl @Inject constructor(
    private val api: AppointmentDetailApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : AppointmentDetailRepository {

    override suspend fun getAppointmentDetail(
        appointmentId: Long
    ): AppointmentDetailResult<AppointmentDetail> {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return AppointmentDetailResult.LoginRequired

        return try {
            val response = api.getAppointmentDetail("Bearer $token", appointmentId)
            if (response.isSuccessful) {
                response.body()?.data?.toDomain()
                    ?.let { AppointmentDetailResult.Success(it) }
                    ?: AppointmentDetailResult.Failure(retryable = false)
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapAppointmentDetailError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppointmentDetailResult.Failure(retryable = true)
        } catch (e: Exception) {
            AppointmentDetailResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapAppointmentDetailError(
    httpStatus: Int,
    errorCode: String?
): AppointmentDetailResult<Nothing> = when {
    errorCode in LOGIN_REQUIRED_CODES -> AppointmentDetailResult.LoginRequired
    errorCode == "NOT_APPOINTMENT_PARTICIPANT" -> AppointmentDetailResult.NotParticipant
    httpStatus == 401 -> AppointmentDetailResult.LoginRequired
    httpStatus == 403 || httpStatus == 404 -> AppointmentDetailResult.NotParticipant
    httpStatus >= 500 -> AppointmentDetailResult.Failure(retryable = true)
    else -> AppointmentDetailResult.Failure(retryable = false)
}

private val LOGIN_REQUIRED_CODES = setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")
