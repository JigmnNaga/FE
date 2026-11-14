package com.jigmnnaga.data.repository

import android.util.Log
import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.AuthApi
import com.jigmnnaga.data.remote.dto.ApiErrorBody
import com.jigmnnaga.data.remote.dto.KakaoLoginRequestDto
import com.jigmnnaga.data.remote.dto.TokenReissueRequestDto
import com.jigmnnaga.domain.model.LoginResult
import com.jigmnnaga.domain.model.ReissueResult
import com.jigmnnaga.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

private const val TAG = "LoginDebug"

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : AuthRepository {

    override suspend fun kakaoLogin(kakaoAccessToken: String): LoginResult {
        return try {
            Log.d(TAG, "백엔드 로그인 API 호출 시작")
            val response = api.kakaoLogin(KakaoLoginRequestDto(kakaoAccessToken))

            if (response.isSuccessful) {
                val loginData = response.body()?.data
                if (loginData != null) {
                    Log.d(TAG, "HTTP 200 수신 (isNewMember=${loginData.isNewMember})")
                    tokenManager.saveTokens(loginData.accessToken, loginData.refreshToken)
                    Log.d(TAG, "JWT 토큰 저장 완료")
                    LoginResult.Success(loginData.isNewMember)
                } else {
                    Log.e(TAG, "서버 오류: 응답 body가 null (HTTP ${response.code()})")
                    LoginResult.ServerError(debugInfo = "빈 응답 body (HTTP ${response.code()})")
                }
            } else {
                val rawErrorBody = response.errorBody()?.string()
                val errorCode = rawErrorBody?.let(::parseErrorCode)
                Log.e(TAG, "HTTP 오류 ${response.code()}: errorCode=$errorCode")
                mapLoginError(response.code(), errorCode)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "네트워크 오류 (${e.javaClass.simpleName}): ${e.message}")
            LoginResult.NetworkError(debugInfo = e.javaClass.simpleName)
        } catch (e: Exception) {
            Log.e(TAG, "예상치 못한 오류 (${e.javaClass.simpleName}): ${e.message}")
            LoginResult.ServerError(debugInfo = e.javaClass.simpleName)
        }
    }

    override suspend fun reissueTokens(): ReissueResult {
        return try {
            val storedRefreshToken = tokenManager.refreshToken.first()
                ?: return ReissueResult.TokenInvalidOrExpired

            Log.d(TAG, "토큰 재발급 API 호출 시작")
            val response = api.reissueTokens(TokenReissueRequestDto(storedRefreshToken))

            if (response.isSuccessful) {
                val tokenData = response.body()?.data
                if (tokenData != null) {
                    tokenManager.saveTokens(tokenData.accessToken, tokenData.refreshToken)
                    Log.d(TAG, "토큰 재발급 완료, 새 토큰 저장")
                    ReissueResult.Success
                } else {
                    Log.e(TAG, "재발급 오류: 응답 body가 null (HTTP ${response.code()})")
                    ReissueResult.ServerError(debugInfo = "빈 응답 body (HTTP ${response.code()})")
                }
            } else {
                val rawErrorBody = response.errorBody()?.string()
                val errorCode = rawErrorBody?.let(::parseErrorCode)
                Log.e(TAG, "재발급 HTTP 오류 ${response.code()}: errorCode=$errorCode")
                when (errorCode) {
                    "EXPIRED_REFRESH_TOKEN", "INVALID_REFRESH_TOKEN" ->
                        ReissueResult.TokenInvalidOrExpired
                    else -> ReissueResult.ServerError(debugInfo = "HTTP ${response.code()}")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "재발급 네트워크 오류 (${e.javaClass.simpleName}): ${e.message}")
            ReissueResult.NetworkError(debugInfo = e.javaClass.simpleName)
        } catch (e: Exception) {
            Log.e(TAG, "재발급 예상치 못한 오류 (${e.javaClass.simpleName}): ${e.message}")
            ReissueResult.ServerError(debugInfo = e.javaClass.simpleName)
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), body).code
    } catch (e: Exception) {
        null
    }
}

private fun mapLoginError(httpStatus: Int, errorCode: String?): LoginResult = when (errorCode) {
    "KAKAO_TOKEN_INVALID" -> LoginResult.InvalidKakaoToken
    "WITHDRAWN_MEMBER" -> LoginResult.WithdrawnMember
    "KAKAO_ACCOUNT_INFO_MISSING" -> LoginResult.KakaoAccountInfoMissing
    else -> LoginResult.ServerError(debugInfo = "HTTP $httpStatus")
}
