package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.LocationSharingSettingsDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

interface LocationSharingSettingsApi {

    @GET("api/v1/members/me/location-sharing")
    suspend fun getSettings(
        @Header("Authorization") authorization: String?
    ): Response<ApiResponse<LocationSharingSettingsDto>>

    /** 세 항목을 항상 모두 보낸다. 저장된 값을 그대로 돌려 준다. */
    @PUT("api/v1/members/me/location-sharing")
    suspend fun saveSettings(
        @Header("Authorization") authorization: String?,
        @Body request: LocationSharingSettingsDto
    ): Response<ApiResponse<LocationSharingSettingsDto>>
}
