package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.api.PlaceSearchApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.domain.model.PlaceSearchPage
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

class PlaceSearchRepositoryImpl @Inject constructor(
    private val api: PlaceSearchApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : PlaceSearchRepository {

    override suspend fun search(
        query: String,
        type: PlaceSearchType,
        page: Int,
        size: Int
    ): PlaceSearchResult<PlaceSearchPage> {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            return PlaceSearchResult.Success(PlaceSearchPage(places = emptyList(), isEnd = true))
        }
        val token = tokenManager.accessToken.first()
        if (token.isNullOrBlank()) return PlaceSearchResult.LoginRequired

        return try {
            val response = api.search("Bearer $token", keyword, type.name, page, size)
            if (response.isSuccessful) {
                response.body()?.data?.toDomain()
                    ?.let { PlaceSearchResult.Success(it) }
                    ?: PlaceSearchResult.Failure(retryable = false)
            } else {
                val code = response.errorBody()?.string()?.let(::parseErrorCode)
                mapPlaceSearchError(response.code(), code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            PlaceSearchResult.Failure(retryable = true)
        } catch (e: Exception) {
            PlaceSearchResult.Failure(retryable = false)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

internal fun mapPlaceSearchError(httpStatus: Int, errorCode: String?): PlaceSearchResult<Nothing> = when {
    errorCode in PLACE_SEARCH_LOGIN_REQUIRED_CODES -> PlaceSearchResult.LoginRequired
    // 서버에 카카오 검색 키가 설정되지 않은 경우라 다시 시도해도 같다.
    errorCode == "PLACE_SEARCH_UNAVAILABLE" || httpStatus == 503 -> PlaceSearchResult.Failure(retryable = false)
    errorCode == "PLACE_SEARCH_FAILED" || httpStatus == 502 -> PlaceSearchResult.Failure(retryable = true)
    httpStatus == 401 -> PlaceSearchResult.LoginRequired
    httpStatus >= 500 -> PlaceSearchResult.Failure(retryable = true)
    else -> PlaceSearchResult.Failure(retryable = false)
}

private val PLACE_SEARCH_LOGIN_REQUIRED_CODES = setOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")
