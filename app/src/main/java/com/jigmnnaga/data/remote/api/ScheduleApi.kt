package com.jigmnnaga.data.remote.api

import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.MyScheduleDto
import com.jigmnnaga.data.remote.dto.ScheduleStatusDto
import com.jigmnnaga.data.remote.dto.ScheduleSubmitRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface ScheduleApi {

    @GET("api/v1/appointments/{appointmentId}/schedules/me")
    suspend fun getMySchedule(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long
    ): Response<ApiResponse<MyScheduleDto>>

    @GET("api/v1/appointments/{appointmentId}/schedules")
    suspend fun getScheduleStatus(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long
    ): Response<ApiResponse<ScheduleStatusDto>>

    /** 가능 날짜별 시간과 희망 장소를 제출한다. 기존 선택은 전부 교체된다. */
    @PUT("api/v1/appointments/{appointmentId}/schedules/me")
    suspend fun submitSchedule(
        @Header("Authorization") authorization: String?,
        @Path("appointmentId") appointmentId: Long,
        @Body request: ScheduleSubmitRequestDto
    ): Response<Unit>
}
