package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.ChatPageDto
import com.jigmnnaga.data.remote.dto.ChatReadRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @GET("api/v1/appointments/{appointmentId}/messages")
    suspend fun getMessages(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int
    ): Response<ApiResponse<ChatPageDto>>

    @PUT("api/v1/appointments/{appointmentId}/messages/read")
    suspend fun markAsRead(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Body request: ChatReadRequestDto
    ): Response<Unit>
}
