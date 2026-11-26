package com.jigmnnaga.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.NotificationSettingsApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.NotificationSettingsDto
import com.jigmnnaga.domain.model.NotificationSettings
import com.jigmnnaga.domain.model.NotificationSettingsSyncResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * [NotificationSettingsRepositoryImpl] 단위 테스트.
 *
 * DataStore를 파일 없이 메모리에 보관하는 가짜 구현으로 대체한다.
 * Windows에서 DataStore가 연속 write 시 .tmp → .pb rename 경합이 발생하는 문제를 회피하면서,
 * 서버 동기화·롤백 등 비즈니스 로직을 완전하게 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 파일 I/O 없이 [Preferences]를 메모리에 보관하는 테스트용 [DataStore].
     * [MutableStateFlow]로 구현해 [NotificationSettingsLocalStore.settings] Flow 구독도 동작한다.
     */
    private fun inMemoryDataStore(): DataStore<Preferences> {
        val state = MutableStateFlow(emptyPreferences())
        return object : DataStore<Preferences> {
            override val data: Flow<Preferences> = state
            override suspend fun updateData(
                transform: suspend (t: Preferences) -> Preferences,
            ): Preferences {
                val updated = transform(state.value)
                state.value = updated
                return updated
            }
        }
    }

    private class FakeApi : NotificationSettingsApi {
        var getResponse: () -> Response<ApiResponse<NotificationSettingsDto>> =
            { ok(defaultDto()) }
        var saveResponse: (NotificationSettingsDto) -> Response<ApiResponse<NotificationSettingsDto>> =
            { ok(it) }
        var getCalls = 0
        val saved = mutableListOf<NotificationSettingsDto>()
        val authorizations = mutableListOf<String?>()

        override suspend fun getSettings(
            authorization: String?,
        ): Response<ApiResponse<NotificationSettingsDto>> {
            getCalls++
            authorizations += authorization
            return getResponse()
        }

        override suspend fun saveSettings(
            authorization: String?,
            request: NotificationSettingsDto,
        ): Response<ApiResponse<NotificationSettingsDto>> {
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
        block: suspend TestScope.(NotificationSettingsRepositoryImpl) -> Unit,
    ) = runTest {
        // 메모리 DataStore를 사용해 파일 I/O와 Windows rename 경합을 완전히 제거한다.
        val repository = NotificationSettingsRepositoryImpl(
            api = api,
            tokenManager = FakeTokenManager(token),
            json = json,
            localStore = NotificationSettingsLocalStore(inMemoryDataStore()),
        )
        block(repository)
    }

    // ─────────────────────────────────────────
    // 조회 (refresh)
    // ─────────────────────────────────────────

    @Test
    fun `서버 설정을 받으면 DataStore에 저장하고 인증 헤더를 보낸다`() {
        val serverDto = NotificationSettingsDto(
            allNotificationsEnabled = false,
            appointmentReminderEnabled = true,
            departureTimeEnabled = false,
            friendArrivalEnabled = true,
            marketingEnabled = true,
        )
        val api = FakeApi().apply { getResponse = { ok(serverDto) } }

        runRepositoryTest(api) { repository ->
            assertEquals(NotificationSettingsSyncResult.Success, repository.refresh())

            val stored = repository.settings.first()
            assertEquals(false, stored.allNotificationsEnabled)
            assertEquals(true, stored.appointmentReminderEnabled)
            assertEquals(false, stored.departureTimeEnabled)
            assertEquals(true, stored.friendArrivalEnabled)
            assertEquals(true, stored.marketingEnabled)
            assertEquals(listOf<String?>("Bearer access"), api.authorizations)
        }
    }

    @Test
    fun `서버 오류로 받지 못하면 DataStore 기존 값을 그대로 둔다`() {
        val api = FakeApi().apply { getResponse = { errorResponse(500) } }

        runRepositoryTest(api) { repository ->
            assertEquals(NotificationSettingsSyncResult.Failure(retryable = true), repository.refresh())
            assertEquals(NotificationSettings(), repository.settings.first())
        }
    }

    @Test
    fun `토큰이 없으면 서버에 요청하지 않고 로그인이 필요하다고 알린다`() {
        val api = FakeApi()

        runRepositoryTest(api, token = null) { repository ->
            assertEquals(NotificationSettingsSyncResult.LoginRequired, repository.refresh())
            assertEquals(0, api.getCalls)
            assertEquals(NotificationSettings(), repository.settings.first())
        }
    }

    @Test
    fun `네트워크 오류로 받지 못하면 Failure(retryable=true)를 돌려 준다`() {
        val api = FakeApi().apply { getResponse = { throw IOException("offline") } }

        runRepositoryTest(api) { repository ->
            assertEquals(NotificationSettingsSyncResult.Failure(retryable = true), repository.refresh())
        }
    }

    // ─────────────────────────────────────────
    // 수정 (update)
    // ─────────────────────────────────────────

    @Test
    fun `설정을 바꾸면 다섯 항목을 모두 서버로 보내고 DataStore에 저장한다`() {
        val api = FakeApi()

        runRepositoryTest(api) { repository ->
            val result = repository.update { it.copy(marketingEnabled = true) }

            assertEquals(NotificationSettingsSyncResult.Success, result)
            assertEquals(
                listOf(
                    NotificationSettingsDto(
                        allNotificationsEnabled = true,
                        appointmentReminderEnabled = true,
                        departureTimeEnabled = true,
                        friendArrivalEnabled = true,
                        marketingEnabled = true,
                    ),
                ),
                api.saved,
            )
            assertEquals(true, repository.settings.first().marketingEnabled)
        }
    }

    @Test
    fun `서버가 돌려준 값을 DataStore에 반영한다`() {
        val serverResponse = NotificationSettingsDto(
            allNotificationsEnabled = false,
            appointmentReminderEnabled = false,
            departureTimeEnabled = false,
            friendArrivalEnabled = false,
            marketingEnabled = false,
        )
        val api = FakeApi().apply { saveResponse = { ok(serverResponse) } }

        runRepositoryTest(api) { repository ->
            repository.update { it.copy(marketingEnabled = true) }

            val stored = repository.settings.first()
            assertEquals(false, stored.allNotificationsEnabled)
            assertEquals(false, stored.marketingEnabled)
        }
    }

    @Test
    fun `서버 저장에 실패하면 바꾸기 전 값으로 되돌린다`() {
        val api = FakeApi().apply { saveResponse = { errorResponse(500) } }

        runRepositoryTest(api) { repository ->
            val result = repository.update { it.copy(allNotificationsEnabled = false) }

            assertEquals(NotificationSettingsSyncResult.Failure(retryable = true), result)
            assertEquals(true, repository.settings.first().allNotificationsEnabled)
        }
    }

    @Test
    fun `로그인이 만료되어 저장하지 못하면 되돌리고 로그인이 필요하다고 알린다`() {
        val api = FakeApi().apply {
            saveResponse = { errorResponse(401, """{"code":"INVALID_ACCESS_TOKEN"}""") }
        }

        runRepositoryTest(api) { repository ->
            val result = repository.update { it.copy(marketingEnabled = true) }

            assertEquals(NotificationSettingsSyncResult.LoginRequired, result)
            assertEquals(false, repository.settings.first().marketingEnabled)
        }
    }

    @Test
    fun `네트워크 오류로 저장하지 못하면 되돌리고 다시 시도할 수 있다고 알린다`() {
        val api = FakeApi().apply { saveResponse = { throw IOException("offline") } }

        runRepositoryTest(api) { repository ->
            val result = repository.update { it.copy(appointmentReminderEnabled = false) }

            assertEquals(NotificationSettingsSyncResult.Failure(retryable = true), result)
            assertEquals(true, repository.settings.first().appointmentReminderEnabled)
        }
    }

    @Test
    fun `바뀐 것이 없으면 서버에 보내지 않는다`() {
        val api = FakeApi()

        runRepositoryTest(api) { repository ->
            val result = repository.update { it }

            assertEquals(NotificationSettingsSyncResult.Success, result)
            assertEquals(emptyList<NotificationSettingsDto>(), api.saved)
        }
    }

    @Test
    fun `토큰이 없으면 저장 요청을 하지 않고 로그인이 필요하다고 알린다`() {
        val api = FakeApi()

        runRepositoryTest(api, token = null) { repository ->
            val result = repository.update { it.copy(marketingEnabled = true) }

            assertEquals(NotificationSettingsSyncResult.LoginRequired, result)
            assertEquals(emptyList<NotificationSettingsDto>(), api.saved)
        }
    }

    // ─────────────────────────────────────────
    // 에러 코드 매핑
    // ─────────────────────────────────────────

    @Test
    fun `응답 코드에 따라 실패 종류를 나눈다`() {
        assertEquals(NotificationSettingsSyncResult.LoginRequired, mapNotificationSettingsError(401))
        assertEquals(
            NotificationSettingsSyncResult.LoginRequired,
            mapNotificationSettingsError(403, "EXPIRED_ACCESS_TOKEN"),
        )
        assertEquals(
            NotificationSettingsSyncResult.LoginRequired,
            mapNotificationSettingsError(403, "INVALID_ACCESS_TOKEN"),
        )
        assertEquals(
            NotificationSettingsSyncResult.Failure(retryable = true),
            mapNotificationSettingsError(500),
        )
        assertEquals(
            NotificationSettingsSyncResult.Failure(retryable = false),
            mapNotificationSettingsError(400, "INVALID_INPUT_VALUE"),
        )
    }

    // ─────────────────────────────────────────
    // DataStore Flow 구독 회귀 테스트
    // ─────────────────────────────────────────

    @Test
    fun `refresh 성공 시 settings Flow에서 서버 값을 받는다`() {
        val serverDto = NotificationSettingsDto(
            allNotificationsEnabled = true,
            appointmentReminderEnabled = false,
            departureTimeEnabled = true,
            friendArrivalEnabled = false,
            marketingEnabled = true,
        )
        val api = FakeApi().apply { getResponse = { ok(serverDto) } }

        runRepositoryTest(api) { repository ->
            repository.refresh()

            val settings = repository.settings.first()
            assertEquals(false, settings.appointmentReminderEnabled)
            assertEquals(false, settings.friendArrivalEnabled)
            assertEquals(true, settings.marketingEnabled)
        }
    }

    @Test
    fun `update 성공 시 settings Flow에서 갱신된 값을 받는다`() {
        val api = FakeApi()

        runRepositoryTest(api) { repository ->
            repository.update { it.copy(allNotificationsEnabled = false) }

            assertEquals(false, repository.settings.first().allNotificationsEnabled)
        }
    }
}

private fun defaultDto() = NotificationSettingsDto(
    allNotificationsEnabled = true,
    appointmentReminderEnabled = true,
    departureTimeEnabled = true,
    friendArrivalEnabled = true,
    marketingEnabled = false,
)

private fun ok(dto: NotificationSettingsDto): Response<ApiResponse<NotificationSettingsDto>> =
    Response.success(ApiResponse(success = true, data = dto))

private fun errorResponse(
    code: Int,
    body: String = "{}",
): Response<ApiResponse<NotificationSettingsDto>> =
    Response.error(code, body.toResponseBody("application/json".toMediaType()))
