package com.jigmnnaga.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.LocationSharingSettingsApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.LocationSharingSettingsDto
import com.jigmnnaga.domain.model.LocationSharingScope
import com.jigmnnaga.domain.model.LocationSharingSettings
import com.jigmnnaga.domain.model.LocationSharingStartTime
import com.jigmnnaga.domain.model.LocationSharingSyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LocationSharingSettingsRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeApi : LocationSharingSettingsApi {
        var getResponse: () -> Response<ApiResponse<LocationSharingSettingsDto>> = { ok(LocationSharingSettingsDto(true, "PARTICIPANTS", 30)) }
        var saveResponse: (LocationSharingSettingsDto) -> Response<ApiResponse<LocationSharingSettingsDto>> = { ok(it) }
        var getCalls = 0
        val saved = mutableListOf<LocationSharingSettingsDto>()
        val authorizations = mutableListOf<String?>()

        override suspend fun getSettings(authorization: String?): Response<ApiResponse<LocationSharingSettingsDto>> {
            getCalls++
            authorizations += authorization
            return getResponse()
        }

        override suspend fun saveSettings(
            authorization: String?,
            request: LocationSharingSettingsDto
        ): Response<ApiResponse<LocationSharingSettingsDto>> {
            saved += request
            authorizations += authorization
            return saveResponse(request)
        }
    }

    private class FakeTokenManager(private val token: String?) : TokenManager {
        override val accessToken: Flow<String?> = flowOf(token)
        override val refreshToken: Flow<String?> = flowOf(token)
        override suspend fun saveTokens(accessToken: String, refreshToken: String) = Unit
        override suspend fun clearTokens() = Unit
        override suspend fun isAccessTokenExpired(): Boolean = false
        override suspend fun isRefreshTokenExpired(): Boolean = false
        override suspend fun validateAndClearInvalidTokens(): Boolean = true
    }

    private fun runRepositoryTest(
        api: FakeApi,
        token: String? = "access",
        block: suspend TestScope.(LocationSharingSettingsRepositoryImpl) -> Unit
    ) = runTest {
        val job = Job()
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + job)
        ) { File(temporaryFolder.root, "settings.preferences_pb") }
        val repository = LocationSharingSettingsRepositoryImpl(
            api = api,
            tokenManager = FakeTokenManager(token),
            json = json,
            localStore = LocationSharingLocalStore(dataStore)
        )
        block(repository)
        job.cancelAndJoin()
    }

    @Test
    fun `서버 값을 받으면 기기에 저장하고 인증 헤더를 보낸다`() {
        val api = FakeApi().apply { getResponse = { ok(LocationSharingSettingsDto(false, "NONE", 60)) } }

        runRepositoryTest(api) { repository ->
            assertEquals(LocationSharingSyncResult.Success, repository.refresh())

            assertEquals(
                LocationSharingSettings(false, LocationSharingScope.NONE, LocationSharingStartTime.HOUR_1),
                repository.settings.first()
            )
            assertEquals(listOf<String?>("Bearer access"), api.authorizations)
        }
    }

    @Test
    fun `토큰이 없으면 서버에 요청하지 않고 로그인이 필요하다고 알린다`() {
        val api = FakeApi()

        runRepositoryTest(api, token = null) { repository ->
            assertEquals(LocationSharingSyncResult.LoginRequired, repository.refresh())
            assertEquals(LocationSharingSyncResult.LoginRequired, repository.update { it.copy(sharingEnabled = false) })

            assertEquals(0, api.getCalls)
            assertEquals(emptyList<LocationSharingSettingsDto>(), api.saved)
            assertEquals(LocationSharingSettings(), repository.settings.first())
        }
    }

    @Test
    fun `서버 오류로 받지 못하면 기기에 저장된 값을 그대로 둔다`() {
        val api = FakeApi().apply { getResponse = { errorResponse(500) } }

        runRepositoryTest(api) { repository ->
            assertEquals(LocationSharingSyncResult.Failure(retryable = true), repository.refresh())

            assertEquals(LocationSharingSettings(), repository.settings.first())
        }
    }

    @Test
    fun `앱이 모르는 서버 값은 반영하지 않는다`() {
        val api = FakeApi().apply { getResponse = { ok(LocationSharingSettingsDto(false, "EVERYONE", 30)) } }

        runRepositoryTest(api) { repository ->
            assertEquals(LocationSharingSyncResult.Failure(retryable = false), repository.refresh())

            assertEquals(LocationSharingSettings(), repository.settings.first())
        }
    }

    @Test
    fun `설정을 바꾸면 세 항목을 모두 서버로 보내고 기기에 저장한다`() {
        val api = FakeApi()

        runRepositoryTest(api) { repository ->
            val result = repository.update { it.copy(sharingEnabled = false) }

            assertEquals(LocationSharingSyncResult.Success, result)
            assertEquals(listOf(LocationSharingSettingsDto(false, "PARTICIPANTS", 30)), api.saved)
            assertEquals(false, repository.settings.first().sharingEnabled)
        }
    }

    @Test
    fun `범위와 시작 시간을 바꾸면 서버 값으로 바꿔 보낸다`() {
        val api = FakeApi()

        runRepositoryTest(api) { repository ->
            repository.update { it.copy(scope = LocationSharingScope.NONE, startTime = LocationSharingStartTime.HOUR_1) }

            assertEquals(listOf(LocationSharingSettingsDto(true, "NONE", 60)), api.saved)
        }
    }

    @Test
    fun `서버가 돌려준 값을 기기에 저장한다`() {
        val api = FakeApi().apply { saveResponse = { ok(LocationSharingSettingsDto(true, "NONE", 10)) } }

        runRepositoryTest(api) { repository ->
            repository.update { it.copy(scope = LocationSharingScope.NONE) }

            assertEquals(
                LocationSharingSettings(true, LocationSharingScope.NONE, LocationSharingStartTime.MINUTES_10),
                repository.settings.first()
            )
        }
    }

    @Test
    fun `서버 저장에 실패하면 바꾸기 전 값으로 되돌린다`() {
        val api = FakeApi().apply { saveResponse = { errorResponse(500) } }

        runRepositoryTest(api) { repository ->
            val result = repository.update { it.copy(sharingEnabled = false) }

            assertEquals(LocationSharingSyncResult.Failure(retryable = true), result)
            assertEquals(LocationSharingSettings(), repository.settings.first())
        }
    }

    @Test
    fun `로그인이 만료되어 저장하지 못하면 되돌리고 로그인이 필요하다고 알린다`() {
        val api = FakeApi().apply { saveResponse = { errorResponse(401, """{"code":"INVALID_ACCESS_TOKEN"}""") } }

        runRepositoryTest(api) { repository ->
            val result = repository.update { it.copy(scope = LocationSharingScope.NONE) }

            assertEquals(LocationSharingSyncResult.LoginRequired, result)
            assertEquals(LocationSharingSettings(), repository.settings.first())
        }
    }

    @Test
    fun `네트워크 오류로 저장하지 못하면 되돌리고 다시 시도할 수 있다고 알린다`() {
        val api = FakeApi().apply { saveResponse = { throw IOException("offline") } }

        runRepositoryTest(api) { repository ->
            val result = repository.update { it.copy(startTime = LocationSharingStartTime.MINUTES_10) }

            assertEquals(LocationSharingSyncResult.Failure(retryable = true), result)
            assertEquals(LocationSharingSettings(), repository.settings.first())
        }
    }

    @Test
    fun `바뀐 것이 없으면 서버에 보내지 않는다`() {
        val api = FakeApi()

        runRepositoryTest(api) { repository ->
            val result = repository.update { it }

            assertEquals(LocationSharingSyncResult.Success, result)
            assertEquals(emptyList<LocationSharingSettingsDto>(), api.saved)
        }
    }

    @Test
    fun `응답 코드에 따라 실패 종류를 나눈다`() {
        assertEquals(LocationSharingSyncResult.LoginRequired, mapLocationSharingError(401))
        assertEquals(LocationSharingSyncResult.LoginRequired, mapLocationSharingError(403, "EXPIRED_ACCESS_TOKEN"))
        assertEquals(LocationSharingSyncResult.Failure(retryable = true), mapLocationSharingError(500))
        assertEquals(
            LocationSharingSyncResult.Failure(retryable = false),
            mapLocationSharingError(400, "INVALID_LOCATION_SHARING_START_MINUTES")
        )
        assertEquals(LocationSharingSyncResult.Failure(retryable = false), mapLocationSharingError(400, "INVALID_INPUT_VALUE"))
    }
}

private fun ok(dto: LocationSharingSettingsDto): Response<ApiResponse<LocationSharingSettingsDto>> =
    Response.success(ApiResponse(success = true, data = dto))

private fun errorResponse(code: Int, body: String = "{}"): Response<ApiResponse<LocationSharingSettingsDto>> =
    Response.error(code, body.toResponseBody("application/json".toMediaType()))
