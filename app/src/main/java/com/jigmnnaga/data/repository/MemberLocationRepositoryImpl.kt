package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.mapper.toRequestDto
import com.jigmnnaga.data.remote.api.MemberLocationApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.MemberLocationDto
import com.jigmnnaga.domain.model.MemberLocation
import com.jigmnnaga.domain.model.MemberLocationInput
import com.jigmnnaga.domain.model.MemberLocationRejectReason
import com.jigmnnaga.domain.model.MemberLocationResult
import com.jigmnnaga.domain.repository.MemberLocationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class MemberLocationRepositoryImpl @Inject constructor(
    private val api: MemberLocationApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : MemberLocationRepository {

    override suspend fun getLocations(): MemberLocationResult<List<MemberLocation>> = request(
        call = { auth -> api.getLocations(auth) },
        onSuccess = { response ->
            response.body()?.data?.map { it.toDomain() }
                ?.let { MemberLocationResult.Success(it) }
                ?: MemberLocationResult.Failure(retryable = false)
        }
    )

    override suspend fun createLocation(input: MemberLocationInput): MemberLocationResult<MemberLocation> = request(
        call = { auth -> api.createLocation(auth, input.toRequestDto()) },
        onSuccess = { response -> response.toSavedLocation() }
    )

    override suspend fun updateLocation(
        locationId: Long,
        input: MemberLocationInput
    ): MemberLocationResult<MemberLocation> = request(
        call = { auth -> api.updateLocation(auth, locationId, input.toRequestDto()) },
        onSuccess = { response -> response.toSavedLocation() }
    )

    override suspend fun deleteLocation(locationId: Long): MemberLocationResult<Unit> = request(
        call = { auth -> api.deleteLocation(auth, locationId) },
        onSuccess = { MemberLocationResult.Success(Unit) }
    )

    private fun Response<ApiResponse<MemberLocationDto>>.toSavedLocation(): MemberLocationResult<MemberLocation> =
        body()?.data?.toDomain()
            ?.let { MemberLocationResult.Success(it) }
            ?: MemberLocationResult.Failure(retryable = false)

    private suspend fun <R, T> request(
        call: suspend (authorization: String) -> Response<R>,
        onSuccess: (Response<R>) -> MemberLocationResult<T>
    ): MemberLocationResult<T> {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return MemberLocationResult.LoginRequired

        return try {
            val response = call("Bearer $token")
            if (response.isSuccessful) {
                onSuccess(response)
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapMemberLocationError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            MemberLocationResult.Failure(retryable = true)
        } catch (e: Exception) {
            MemberLocationResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapMemberLocationError(httpStatus: Int, errorCode: String? = null): MemberLocationResult<Nothing> = when {
    errorCode in MEMBER_LOCATION_LOGIN_REQUIRED_CODES -> MemberLocationResult.LoginRequired
    errorCode == "MEMBER_LOCATION_LIMIT_EXCEEDED" ->
        MemberLocationResult.Rejected(MemberLocationRejectReason.LIMIT_EXCEEDED)
    errorCode == "MEMBER_LOCATION_NOT_FOUND" ->
        MemberLocationResult.Rejected(MemberLocationRejectReason.NOT_FOUND)
    errorCode == "INVALID_INPUT_VALUE" ->
        MemberLocationResult.Rejected(MemberLocationRejectReason.INVALID_INPUT)
    httpStatus == 401 -> MemberLocationResult.LoginRequired
    httpStatus == 400 -> MemberLocationResult.Rejected(MemberLocationRejectReason.INVALID_INPUT)
    httpStatus >= 500 -> MemberLocationResult.Failure(retryable = true)
    else -> MemberLocationResult.Failure(retryable = false)
}

private val MEMBER_LOCATION_LOGIN_REQUIRED_CODES = setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")
