package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.LoginResult
import com.jigmnnaga.domain.model.LogoutResult
import com.jigmnnaga.domain.model.ReissueResult
import com.jigmnnaga.domain.model.WithdrawResult

interface AuthRepository {

    /**
     * 카카오 액세스 토큰으로 백엔드 로그인 API를 호출합니다.
     * 성공 시 서버에서 발급한 JWT를 TokenManager에 저장합니다.
     */
    suspend fun kakaoLogin(kakaoAccessToken: String): LoginResult

    /**
     * 저장된 Refresh Token으로 Access Token과 Refresh Token을 재발급합니다.
     * Refresh Token Rotation 방식이므로 성공 시 두 토큰이 모두 갱신됩니다.
     * 성공 시 새 토큰을 TokenManager에 저장합니다.
     */
    suspend fun reissueTokens(): ReissueResult

    /**
     * 서버에 저장된 Refresh Token을 삭제하고 로컬 토큰을 초기화합니다.
     * API 실패 시 로컬 토큰을 삭제하지 않습니다.
     */
    suspend fun logout(): LogoutResult

    /**
     * 회원 정보를 익명화하고 소셜 연결을 해제합니다.
     * 성공 시 로컬 토큰을 초기화합니다.
     * API 실패 시 로컬 토큰을 삭제하지 않습니다.
     */
    suspend fun withdraw(): WithdrawResult
}
