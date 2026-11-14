package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.KakaoLoginRequestDto
import com.jigmnnaga.data.remote.dto.LoginResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/kakao/login")
    suspend fun kakaoLogin(
        @Body request: KakaoLoginRequestDto
    ): Response<ApiResponse<LoginResponseDto>>
}
