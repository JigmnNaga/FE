package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.ConfirmDateTimeRequestDto
import com.jigmnnaga.data.remote.dto.ConfirmLocationRequestDto
import com.jigmnnaga.data.remote.dto.ScheduleHeatmapDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ConfirmationApi {

    /** 슬롯 단위는 30 또는 60분이다. */
    @GET("api/v1/appointments/{appointmentId}/schedules/heatmap")
    suspend fun getHeatmap(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Query("slotMinutes") slotMinutes: Int
    ): Response<ApiResponse<ScheduleHeatmapDto>>

    @PUT("api/v1/appointments/{appointmentId}/confirmation/date-time")
    suspend fun confirmDateTime(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Body request: ConfirmDateTimeRequestDto
    ): Response<Unit>

    @PUT("api/v1/appointments/{appointmentId}/confirmation/location")
    suspend fun confirmLocation(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Body request: ConfirmLocationRequestDto
    ): Response<Unit>
}
