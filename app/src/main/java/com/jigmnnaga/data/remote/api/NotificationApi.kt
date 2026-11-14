package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.NotificationCountDto
import com.jigmnnaga.data.remote.dto.NotificationPageDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {

    /** 최신순 커서 조회. 첫 조회에는 [cursor]를 보내지 않는다. */
    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Header("Authorization") authorization: String?,
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int
    ): Response<ApiResponse<NotificationPageDto>>

    @PATCH("api/v1/notifications/{notificationId}/read")
    suspend fun readNotification(
        @Header("Authorization") authorization: String?,
        @Path("notificationId") notificationId: Long
    ): Response<Unit>

    @PATCH("api/v1/notifications/read-all")
    suspend fun readAllNotifications(
        @Header("Authorization") authorization: String?
    ): Response<ApiResponse<NotificationCountDto>>
}
