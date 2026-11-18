package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.api.TravelApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.data.remote.dto.DepartureLocationRequestDto
import com.jigmnnaga.data.remote.dto.TravelModeRequestDto
import com.jigmnnaga.domain.model.TravelMode
import com.jigmnnaga.domain.model.TravelOptions
import com.jigmnnaga.domain.model.TravelRejectReason
import com.jigmnnaga.domain.model.TravelResult
import com.jigmnnaga.domain.repository.TravelRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class TravelRepositoryImpl @Inject constructor(
    private val api: TravelApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : TravelRepository {

    override suspend fun registerDepartureLocation(
        appointmentId: Long,
        name: String,
        latitude: Double,
        longitude: Double
    ) = request(
        call = { auth ->
            api.registerDepartureLocation(
                auth,
                appointmentId,
                DepartureLocationRequestDto(name, latitude, longitude)
            )
        },
        onSuccess = { TravelResult.Success(Unit) }
    )

    override suspend fun getTravelOptions(
        appointmentId: Long,
        latitude: Double,
        longitude: Double
    ) = request(
        call = { auth -> api.getTravelOptions(auth, appointmentId, latitude, longitude) },
        onSuccess = { response ->
            response.body()?.data?.toDomain()
                ?.let { TravelResult.Success(it) }
                ?: TravelResult.Failure(retryable = false)
        }
    )

    override suspend fun selectTravelMode(
        appointmentId: Long,
        mode: TravelMode,
        durationMinutes: Int
    ) = request(
        call = { auth ->
            api.selectTravelMode(auth, appointmentId, TravelModeRequestDto(mode.name, durationMinutes))
        },
        onSuccess = { TravelResult.Success(Unit) }
    )

    override suspend fun depart(appointmentId: Long) = request(
        call = { auth -> api.depart(auth, appointmentId) },
        onSuccess = { TravelResult.Success(Unit) }
    )

    override suspend fun arrive(appointmentId: Long) = request(
        call = { auth -> api.arrive(auth, appointmentId) },
        onSuccess = { TravelResult.Success(Unit) }
    )

    private suspend fun <R, T> request(
        call: suspend (authorization: String) -> Response<R>,
        onSuccess: (Response<R>) -> TravelResult<T>
    ): TravelResult<T> {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return TravelResult.LoginRequired

        return try {
            val response = call("Bearer $token")
            if (response.isSuccessful) {
                onSuccess(response)
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapTravelError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            TravelResult.Failure(retryable = true)
        } catch (e: Exception) {
            TravelResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapTravelError(httpStatus: Int, errorCode: String?): TravelResult<Nothing> = when {
    errorCode in TRAVEL_LOGIN_REQUIRED_CODES -> TravelResult.LoginRequired
    errorCode == "TRAVEL_DESTINATION_NOT_CONFIRMED" ->
        TravelResult.Rejected(TravelRejectReason.DESTINATION_NOT_CONFIRMED)
    errorCode == "TRAVEL_MODE_TIME_PASSED" -> TravelResult.Rejected(TravelRejectReason.TIME_PASSED)
    errorCode == "DEPARTURE_LOCATION_NOT_REGISTERED" ->
        TravelResult.Rejected(TravelRejectReason.DEPARTURE_LOCATION_NOT_REGISTERED)
    errorCode == "ALREADY_DEPARTED" -> TravelResult.Rejected(TravelRejectReason.ALREADY_DEPARTED)
    errorCode == "ALREADY_ARRIVED" -> TravelResult.Rejected(TravelRejectReason.ALREADY_ARRIVED)
    errorCode == "NOT_YET_DEPARTED" -> TravelResult.Rejected(TravelRejectReason.NOT_YET_DEPARTED)
    errorCode == "NOT_APPOINTMENT_PARTICIPANT" -> TravelResult.Rejected(TravelRejectReason.NOT_PARTICIPANT)
    errorCode == "INVALID_INPUT_VALUE" || httpStatus == 400 ->
        TravelResult.Rejected(TravelRejectReason.INVALID_INPUT)
    httpStatus == 401 -> TravelResult.LoginRequired
    httpStatus == 403 || httpStatus == 404 -> TravelResult.Rejected(TravelRejectReason.NOT_PARTICIPANT)
    httpStatus >= 500 -> TravelResult.Failure(retryable = true)
    else -> TravelResult.Failure(retryable = false)
}

private val TRAVEL_LOGIN_REQUIRED_CODES = setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")
