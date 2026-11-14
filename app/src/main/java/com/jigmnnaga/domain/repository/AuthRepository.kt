package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.LoginResult

interface AuthRepository {

    /**
     * 카카오 액세스 토큰으로 백엔드 로그인 API를 호출합니다.
     * 성공 시 서버에서 발급한 JWT를 TokenManager에 저장합니다.
     */
    suspend fun kakaoLogin(kakaoAccessToken: String): LoginResult
}
