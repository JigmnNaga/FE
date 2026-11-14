package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.PlaceSearchApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.PlaceDto
import com.jigmnnaga.data.remote.dto.PlaceSearchResponseDto
import com.jigmnnaga.domain.model.PlaceSearchPage
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.SearchedPlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class PlaceSearchRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class FakeApi : PlaceSearchApi {
        var onSearch: suspend () -> Response<ApiResponse<PlaceSearchResponseDto>> = { error("not set") }
        var calls = 0
        var lastAuthorization: String? = null
        var lastQuery: String? = null
        var lastType: String? = null
        var lastPage: Int? = null
        var lastSize: Int? = null

        override suspend fun search(
            authorization: String?,
            query: String,
            type: String,
            page: Int,
            size: Int
        ): Response<ApiResponse<PlaceSearchResponseDto>> {
            calls++; lastAuthorization = authorization; lastQuery = query
            lastType = type; lastPage = page; lastSize = size
            return onSearch()
        }
    }

    private class FakeTokenManager(private val token: String?) : TokenManager {
        override val accessToken: Flow<String?> = flowOf(token)
        override val refreshToken: Flow<String?> = flowOf(null)
        override suspend fun saveTokens(accessToken: String, refreshToken: String) = Unit
        override suspend fun clearTokens() = Unit
        override suspend fun isAccessTokenExpired(): Boolean = false
        override suspend fun isRefreshTokenExpired(): Boolean = false
        override suspend fun validateAndClearInvalidTokens(): Boolean = true
    }

    private fun repo(api: FakeApi, token: String? = "token") =
        PlaceSearchRepositoryImpl(api, FakeTokenManager(token), json)

    private fun <T> errorBody(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private fun ok(dto: PlaceSearchResponseDto) = Response.success(ApiResponse(data = dto))

    @Test
    fun `검색은 토큰과 검색어와 종류와 페이지를 보내고 결과를 도메인으로 바꾼다`() = runTest {
        val api = FakeApi().apply {
            onSearch = {
                ok(
                    PlaceSearchResponseDto(
                        places = listOf(
                            PlaceDto(
                                kakaoPlaceId = "1",
                                placeName = "강남역",
                                lineNames = "2호선, 신분당선",
                                roadAddress = "서울 강남구 강남대로 396",
                                jibunAddress = "",
                                latitude = 37.49,
                                longitude = 127.02
                            )
                        ),
                        isEnd = false
                    )
                )
            }
        }

        val result = repo(api).search(" 강남 ", PlaceSearchType.STATION, page = 2, size = 10)

        assertEquals(
            PlaceSearchResult.Success(
                PlaceSearchPage(
                    places = listOf(
                        SearchedPlace(
                            kakaoPlaceId = "1",
                            name = "강남역",
                            lineNames = "2호선, 신분당선",
                            roadAddress = "서울 강남구 강남대로 396",
                            jibunAddress = null,
                            latitude = 37.49,
                            longitude = 127.02
                        )
                    ),
                    isEnd = false
                )
            ),
            result
        )
        assertEquals("Bearer token", api.lastAuthorization)
        assertEquals("강남", api.lastQuery)
        assertEquals("STATION", api.lastType)
        assertEquals(2, api.lastPage)
        assertEquals(10, api.lastSize)
    }

    @Test
    fun `장소 종류는 이름 그대로 보낸다`() = runTest {
        val api = FakeApi().apply { onSearch = { ok(PlaceSearchResponseDto()) } }
        repo(api).search("카페", PlaceSearchType.PLACE)
        assertEquals("PLACE", api.lastType)
        assertEquals(1, api.lastPage)
        assertEquals(15, api.lastSize)
    }

    @Test
    fun `검색어가 비어 있으면 서버를 부르지 않고 빈 결과를 돌려준다`() = runTest {
        for (query in listOf("", "   ")) {
            val api = FakeApi()
            assertEquals(
                PlaceSearchResult.Success(PlaceSearchPage(emptyList(), isEnd = true)),
                repo(api).search(query, PlaceSearchType.STATION)
            )
            assertEquals(0, api.calls)
        }
    }

    @Test
    fun `토큰이 없거나 비어 있으면 서버를 부르지 않고 로그인이 필요하다`() = runTest {
        for (token in listOf(null, "", "  ")) {
            val api = FakeApi()
            assertEquals(
                PlaceSearchResult.LoginRequired,
                repo(api, token).search("강남", PlaceSearchType.STATION)
            )
            assertEquals(0, api.calls)
        }
    }

    @Test
    fun `토큰 오류 코드와 401은 로그인이 필요하다`() = runTest {
        for (code in listOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")) {
            val api = FakeApi().apply {
                onSearch = { errorBody(401, """{"code":"$code","message":"x"}""") }
            }
            assertEquals(PlaceSearchResult.LoginRequired, repo(api).search("강남", PlaceSearchType.STATION))
        }
        val api = FakeApi().apply { onSearch = { errorBody(401, "") } }
        assertEquals(PlaceSearchResult.LoginRequired, repo(api).search("강남", PlaceSearchType.STATION))
    }

    @Test
    fun `카카오 검색 실패는 다시 시도할 수 있고 설정 안 됨은 다시 시도해도 같다`() = runTest {
        val failed = FakeApi().apply {
            onSearch = { errorBody(502, """{"code":"PLACE_SEARCH_FAILED","message":"x"}""") }
        }
        assertEquals(
            PlaceSearchResult.Failure(retryable = true),
            repo(failed).search("강남", PlaceSearchType.STATION)
        )

        val unavailable = FakeApi().apply {
            onSearch = { errorBody(503, """{"code":"PLACE_SEARCH_UNAVAILABLE","message":"x"}""") }
        }
        assertEquals(
            PlaceSearchResult.Failure(retryable = false),
            repo(unavailable).search("강남", PlaceSearchType.STATION)
        )
    }

    @Test
    fun `서버 오류는 다시 시도할 수 있고 그 밖의 오류는 아니다`() = runTest {
        val server = FakeApi().apply { onSearch = { errorBody(500, "") } }
        assertEquals(PlaceSearchResult.Failure(retryable = true), repo(server).search("강남", PlaceSearchType.STATION))

        val bad = FakeApi().apply { onSearch = { errorBody(400, """{"code":"INVALID_INPUT"}""") } }
        assertEquals(PlaceSearchResult.Failure(retryable = false), repo(bad).search("강남", PlaceSearchType.STATION))
    }

    @Test
    fun `네트워크 오류는 다시 시도할 수 있고 응답 본문이 비면 아니다`() = runTest {
        val io = FakeApi().apply { onSearch = { throw IOException("offline") } }
        assertEquals(PlaceSearchResult.Failure(retryable = true), repo(io).search("강남", PlaceSearchType.STATION))

        val empty = FakeApi().apply { onSearch = { Response.success(ApiResponse(data = null)) } }
        assertEquals(PlaceSearchResult.Failure(retryable = false), repo(empty).search("강남", PlaceSearchType.STATION))
    }

    @Test
    fun `응답의 isEnd 또는 end 키를 모두 읽는다`() {
        val a = json.decodeFromString(PlaceSearchResponseDto.serializer(), """{"places":[],"isEnd":false}""")
        val b = json.decodeFromString(PlaceSearchResponseDto.serializer(), """{"places":[],"end":false}""")
        assertEquals(false, a.isEnd)
        assertEquals(false, b.isEnd)
    }
}
