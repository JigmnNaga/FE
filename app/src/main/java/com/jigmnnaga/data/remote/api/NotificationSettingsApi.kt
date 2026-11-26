package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.NotificationSettingsDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

interface NotificationSettingsApi {

    @GET("api/v1/members/me/notification-settings")
    suspend fun getSettings(
        @Header("Authorization") authorization: String?
    ): Response<ApiResponse<NotificationSettingsDto>>

    /** 다섯 항목을 항상 모두 보낸다. 저장된 설정을 그대로 돌려 준다. */
    @PUT("api/v1/members/me/notification-settings")
    suspend fun saveSettings(
        @Header("Authorization") authorization: String?,
        @Body request: NotificationSettingsDto
    ): Response<ApiResponse<NotificationSettingsDto>>
}
