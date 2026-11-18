package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.api.MemberLocationApi
import com.jigmnnaga.domain.model.MemberLocation
import com.jigmnnaga.domain.model.MemberLocationResult
import com.jigmnnaga.domain.repository.MemberLocationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject

class MemberLocationRepositoryImpl @Inject constructor(
    private val api: MemberLocationApi,
    private val tokenManager: TokenManager
) : MemberLocationRepository {

    override suspend fun getLocations(): MemberLocationResult<List<MemberLocation>> {
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return MemberLocationResult.LoginRequired

        return try {
            val response = api.getLocations("Bearer $token")
            if (response.isSuccessful) {
                val list = response.body()?.data?.map { it.toDomain() }
                if (list != null) MemberLocationResult.Success(list) else MemberLocationResult.Failure(retryable = false)
            } else {
                mapMemberLocationError(response.code())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            MemberLocationResult.Failure(retryable = true)
        } catch (e: Exception) {
            MemberLocationResult.Failure(retryable = false)
        }
    }
}

internal fun mapMemberLocationError(httpStatus: Int): MemberLocationResult<Nothing> = when {
    httpStatus == 401 -> MemberLocationResult.LoginRequired
    httpStatus >= 500 -> MemberLocationResult.Failure(retryable = true)
    else -> MemberLocationResult.Failure(retryable = false)
}
