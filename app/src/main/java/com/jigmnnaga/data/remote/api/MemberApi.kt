package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.AppointmentStatsResponseDto
import com.jigmnnaga.data.remote.dto.MyInfoResponseDto
import com.jigmnnaga.data.remote.dto.ProfileImageUploadUrlRequestDto
import com.jigmnnaga.data.remote.dto.ProfileImageUploadUrlResponseDto
import com.jigmnnaga.data.remote.dto.ProfileUpdateRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST

interface MemberApi {

    @GET("api/v1/members/me")
    suspend fun getMyInfo(
        @Header("Authorization") authorization: String?
    ): Response<ApiResponse<MyInfoResponseDto>>

    @PATCH("api/v1/members/me/profile")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String?,
        @Body request: ProfileUpdateRequestDto
    ): Response<ApiResponse<MyInfoResponseDto>>

    @POST("api/v1/members/me/profile-image/upload-url")
    suspend fun getProfileImageUploadUrl(
        @Header("Authorization") authorization: String?,
        @Body request: ProfileImageUploadUrlRequestDto
    ): Response<ApiResponse<ProfileImageUploadUrlResponseDto>>

    @DELETE("api/v1/members/me/profile-image")
    suspend fun deleteProfileImage(
        @Header("Authorization") authorization: String?
    ): Response<ApiResponse<Unit>>

    @GET("api/v1/members/me/stats")
    suspend fun getMyStats(
        @Header("Authorization") authorization: String?
    ): Response<ApiResponse<AppointmentStatsResponseDto>>
}
