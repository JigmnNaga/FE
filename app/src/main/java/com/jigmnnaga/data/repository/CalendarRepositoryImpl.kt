package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.api.MyAppointmentApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.domain.model.CalendarResult
import com.jigmnnaga.domain.repository.CalendarRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

class CalendarRepositoryImpl @Inject constructor(
    private val appointmentApi: MyAppointmentApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : CalendarRepository {

    override suspend fun getCalendar(month: String): CalendarResult {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return CalendarResult.LoginRequired

        return try {
            val response = appointmentApi.getCalendar("Bearer $token", month)
            if (response.isSuccessful) {
                val days = response.body()?.data?.days?.map { it.toDomain() } ?: emptyList()
                CalendarResult.Success(days)
            } else {
                val code = parseErrorCode(response.errorBody()?.string())
                mapCalendarError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            CalendarResult.Failure(retryable = true)
        } catch (e: Exception) {
            CalendarResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String?): String? = body?.let {
        try {
            json.decodeFromString(ApiErrorBody.serializer(), it).code
        } catch (e: Exception) {
            null
        }
    }
}

// ── 에러 매핑 ─────────────────────────────────────────────────────────────────

private val CALENDAR_LOGIN_REQUIRED_CODES =
    setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")

internal fun mapCalendarError(httpStatus: Int, errorCode: String?): CalendarResult = when {
    errorCode in CALENDAR_LOGIN_REQUIRED_CODES || httpStatus == 401 -> CalendarResult.LoginRequired
    httpStatus >= 500 -> CalendarResult.Failure(retryable = true)
    else -> CalendarResult.Failure(retryable = false)
}
