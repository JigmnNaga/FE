package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.api.InviteApi
import com.jigmnnaga.data.remote.api.MyAppointmentApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.domain.model.HomeAppointmentListResult
import com.jigmnnaga.domain.model.NextAppointmentResult
import com.jigmnnaga.domain.model.PendingInviteCountResult
import com.jigmnnaga.domain.repository.HomeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val appointmentApi: MyAppointmentApi,
    private val inviteApi: InviteApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : HomeRepository {

    override suspend fun getUpcomingAppointments(): HomeAppointmentListResult {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return HomeAppointmentListResult.LoginRequired

        return try {
            val response = appointmentApi.getMyAppointments("Bearer $token", "UPCOMING")
            if (response.isSuccessful) {
                val list = response.body()?.data?.map { it.toDomain() } ?: emptyList()
                HomeAppointmentListResult.Success(list)
            } else {
                val code = parseErrorCode(response.errorBody()?.string())
                mapAppointmentListError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            HomeAppointmentListResult.Failure(retryable = true)
        } catch (e: Exception) {
            HomeAppointmentListResult.Failure(retryable = false)
        }
    }

    override suspend fun getPastAppointments(): HomeAppointmentListResult {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return HomeAppointmentListResult.LoginRequired

        return try {
            val response = appointmentApi.getMyAppointments("Bearer $token", "PAST")
            if (response.isSuccessful) {
                val list = response.body()?.data?.map { it.toDomain() } ?: emptyList()
                HomeAppointmentListResult.Success(list)
            } else {
                val code = parseErrorCode(response.errorBody()?.string())
                mapAppointmentListError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            HomeAppointmentListResult.Failure(retryable = true)
        } catch (e: Exception) {
            HomeAppointmentListResult.Failure(retryable = false)
        }
    }

    override suspend fun getNextAppointment(): NextAppointmentResult {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return NextAppointmentResult.LoginRequired

        return try {
            val response = appointmentApi.getNextAppointment("Bearer $token")
            if (response.isSuccessful) {
                // data: null → 다음 확정 약속 없음
                NextAppointmentResult.Success(response.body()?.data?.toDomain())
            } else {
                val code = parseErrorCode(response.errorBody()?.string())
                mapNextAppointmentError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            NextAppointmentResult.Failure(retryable = true)
        } catch (e: Exception) {
            NextAppointmentResult.Failure(retryable = false)
        }
    }

    override suspend fun getPendingInviteCount(): PendingInviteCountResult {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return PendingInviteCountResult.LoginRequired

        return try {
            val response = inviteApi.getReceivedInvites("Bearer $token")
            if (response.isSuccessful) {
                val count = response.body()?.data?.size ?: 0
                PendingInviteCountResult.Success(count)
            } else {
                val code = parseErrorCode(response.errorBody()?.string())
                mapPendingInviteCountError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            PendingInviteCountResult.Failure(retryable = true)
        } catch (e: Exception) {
            PendingInviteCountResult.Failure(retryable = false)
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

private val LOGIN_REQUIRED_CODES = setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")

internal fun mapAppointmentListError(
    httpStatus: Int,
    errorCode: String?
): HomeAppointmentListResult = when {
    errorCode in LOGIN_REQUIRED_CODES || httpStatus == 401 -> HomeAppointmentListResult.LoginRequired
    httpStatus >= 500 -> HomeAppointmentListResult.Failure(retryable = true)
    else -> HomeAppointmentListResult.Failure(retryable = false)
}

internal fun mapNextAppointmentError(
    httpStatus: Int,
    errorCode: String?
): NextAppointmentResult = when {
    errorCode in LOGIN_REQUIRED_CODES || httpStatus == 401 -> NextAppointmentResult.LoginRequired
    httpStatus >= 500 -> NextAppointmentResult.Failure(retryable = true)
    else -> NextAppointmentResult.Failure(retryable = false)
}

internal fun mapPendingInviteCountError(
    httpStatus: Int,
    errorCode: String?
): PendingInviteCountResult = when {
    errorCode in LOGIN_REQUIRED_CODES || httpStatus == 401 -> PendingInviteCountResult.LoginRequired
    httpStatus >= 500 -> PendingInviteCountResult.Failure(retryable = true)
    else -> PendingInviteCountResult.Failure(retryable = false)
}
